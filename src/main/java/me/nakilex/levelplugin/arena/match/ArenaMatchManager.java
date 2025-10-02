package me.nakilex.levelplugin.arena.match;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.arena.ArenaMode;
import me.nakilex.levelplugin.arena.ArenaQueueManager;
import me.nakilex.levelplugin.arena.instance.ArenaInstance;
import me.nakilex.levelplugin.arena.instance.ArenaInstanceManager;
import me.nakilex.levelplugin.arena.rating.ArenaRatingManager;
import me.nakilex.levelplugin.scoreboard.PlayerScoreboardManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;

/**
 * Coordinates arena matches: teleporting players, running countdowns, handling
 * victory conditions and updating ELO ratings once a duel concludes.
 */
public class ArenaMatchManager implements Listener {
    private final Main plugin;
    private final ArenaQueueManager queueManager;
    private final ArenaInstanceManager instanceManager;
    private final ArenaRatingManager ratingManager;
    private final PlayerScoreboardManager scoreboardManager;
    private final ArenaCombatTracker combatTracker;

    private final Map<UUID, ArenaMatch> matchesByPlayer = new HashMap<>();
    private final Map<ArenaMatch, BukkitTask> countdownTasks = new HashMap<>();
    private final Map<ArenaMatch, BukkitTask> timeoutTasks = new HashMap<>();

    private static final long MATCH_TIMEOUT_TICKS = 20L * 8 * 60; // 8 minutes

    public ArenaMatchManager(Main plugin,
                             ArenaQueueManager queueManager,
                             ArenaInstanceManager instanceManager,
                             ArenaRatingManager ratingManager,
                             PlayerScoreboardManager scoreboardManager,
                             ArenaCombatTracker combatTracker) {
        this.plugin = plugin;
        this.queueManager = queueManager;
        this.instanceManager = instanceManager;
        this.ratingManager = ratingManager;
        this.scoreboardManager = scoreboardManager;
        this.combatTracker = combatTracker;
    }

    /** Fetch the match the given player is currently involved in, if any. */
    public Optional<ArenaMatch> findMatch(UUID playerId) {
        return Optional.ofNullable(matchesByPlayer.get(playerId));
    }

    /** Check if the player is currently fighting in an arena instance. */
    public boolean isInMatch(UUID playerId) {
        return findMatch(playerId).isPresent();
    }

    /** Begin a new match for the provided queue entrants. */
    public void startMatch(ArenaQueueManager.QueueEntry first, ArenaQueueManager.QueueEntry second) {
        if (first.members().isEmpty() || second.members().isEmpty()) {
            return;
        }
        UUID firstId = first.members().get(0);
        UUID secondId = second.members().get(0);
        Player one = Bukkit.getPlayer(firstId);
        Player two = Bukkit.getPlayer(secondId);
        if (one == null || !one.isOnline()) {
            if (two != null) {
                sendMessage(two, MessageType.WARNING, ChatColor.GRAY + "Opponent left before the match started. Re-queuing you.");
                queueManager.requeue(second);
            }
            return;
        }
        if (two == null || !two.isOnline()) {
            sendMessage(one, MessageType.WARNING, ChatColor.GRAY + "Opponent left before the match started. Re-queuing you.");
            queueManager.requeue(first);
            return;
        }

        if (isInMatch(firstId) || isInMatch(secondId)) {
            queueManager.requeue(first);
            queueManager.requeue(second);
            return;
        }

        ArenaInstance instance = instanceManager.createInstance();
        if (instance == null) {
            sendMessage(one, MessageType.ERROR, ChatColor.RED + "Arena instance unavailable. You have been re-queued.");
            sendMessage(two, MessageType.ERROR, ChatColor.RED + "Arena instance unavailable. You have been re-queued.");
            queueManager.requeue(first);
            queueManager.requeue(second);
            return;
        }

        ArenaRatingManager.RatingSnapshot firstSnapshot = first.ratingSnapshot(firstId);
        if (firstSnapshot == null) {
            firstSnapshot = ratingManager.getSnapshot(firstId, ArenaMode.ONE_VS_ONE.ratingCategory());
        }
        ArenaRatingManager.RatingSnapshot secondSnapshot = second.ratingSnapshot(secondId);
        if (secondSnapshot == null) {
            secondSnapshot = ratingManager.getSnapshot(secondId, ArenaMode.ONE_VS_ONE.ratingCategory());
        }

        ArenaMatch match = new ArenaMatch(firstId,
                secondId,
                instance,
                firstSnapshot,
                secondSnapshot);

        matchesByPlayer.put(firstId, match);
        matchesByPlayer.put(secondId, match);

        combatTracker.beginTracking(List.of(firstId, secondId));

        preparePlayer(one);
        preparePlayer(two);

        one.teleport(instance.getFirstSpawn());
        two.teleport(instance.getSecondSpawn());

        announceMatchFound(one, two, first, second);
        runCountdown(match, one, two);
        scheduleTimeout(match);
    }

    private void runCountdown(ArenaMatch match, Player one, Player two) {
        BukkitTask task = new BukkitRunnable() {
            private int seconds = 5;

            @Override
            public void run() {
                if (!one.isOnline() || !two.isOnline()) {
                    cancel();
                    return;
                }
                if (match.getState() == ArenaMatch.State.FINISHED) {
                    cancel();
                    return;
                }
                if (seconds <= 0) {
                    match.setState(ArenaMatch.State.ACTIVE);
                    one.sendTitle(ChatColor.GREEN + "FIGHT!", ChatColor.DARK_GRAY + "Good luck!", 0, 30, 10);
                    two.sendTitle(ChatColor.GREEN + "FIGHT!", ChatColor.DARK_GRAY + "Good luck!", 0, 30, 10);
                    sendMessage(one, MessageType.SUCCESS, ChatColor.GRAY + "The duel has begun! Defeat " + ChatColor.YELLOW + two.getName());
                    sendMessage(two, MessageType.SUCCESS, ChatColor.GRAY + "The duel has begun! Defeat " + ChatColor.YELLOW + one.getName());
                    cancel();
                    return;
                }

                String title = ChatColor.YELLOW + String.valueOf(seconds);
                one.sendTitle(ChatColor.GRAY + "Match starting in", title, 0, 25, 10);
                two.sendTitle(ChatColor.GRAY + "Match starting in", title, 0, 25, 10);
                seconds--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
        countdownTasks.put(match, task);
    }

    private void scheduleTimeout(ArenaMatch match) {
        BukkitTask timeout = new BukkitRunnable() {
            @Override
            public void run() {
                if (match.getState() == ArenaMatch.State.FINISHED) {
                    return;
                }
                finishMatch(match, Optional.empty(), Optional.empty(), VictoryReason.TIMEOUT);
            }
        }.runTaskLater(plugin, MATCH_TIMEOUT_TICKS);
        timeoutTasks.put(match, timeout);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        ArenaMatch match = findMatch(event.getPlayer().getUniqueId()).orElse(null);
        if (match == null || match.getState() != ArenaMatch.State.COUNTDOWN) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        if (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ()) {
            return;
        }

        Location locked = from.clone();
        locked.setYaw(to.getYaw());
        locked.setPitch(to.getPitch());
        event.setTo(locked);
    }

    private void announceMatchFound(Player one,
                                    Player two,
                                    ArenaQueueManager.QueueEntry first,
                                    ArenaQueueManager.QueueEntry second) {
        int ratingOne = ratingManager.getRating(one.getUniqueId());
        int ratingTwo = ratingManager.getRating(two.getUniqueId());
        sendMessage(one, MessageType.INFO,
                ChatColor.GRAY + "Matched against " + ChatColor.YELLOW + two.getName() + ChatColor.GRAY +
                        " (" + ChatColor.GOLD + ratingTwo + ChatColor.GRAY + ", " + ratingManager.formatTier(ratingTwo) + ChatColor.GRAY + ")");
        sendMessage(two, MessageType.INFO,
                ChatColor.GRAY + "Matched against " + ChatColor.YELLOW + one.getName() + ChatColor.GRAY +
                        " (" + ChatColor.GOLD + ratingOne + ChatColor.GRAY + ", " + ratingManager.formatTier(ratingOne) + ChatColor.GRAY + ")");

        ArenaRatingManager.RatingSnapshot firstSnapshot = first.ratingSnapshot(firstId);
        if (firstSnapshot == null) {
            firstSnapshot = ratingManager.getSnapshot(firstId, ArenaMode.ONE_VS_ONE.ratingCategory());
        }
        ArenaRatingManager.RatingSnapshot secondSnapshot = second.ratingSnapshot(secondId);
        if (secondSnapshot == null) {
            secondSnapshot = ratingManager.getSnapshot(secondId, ArenaMode.ONE_VS_ONE.ratingCategory());
        }

        sendMessage(one, MessageType.INFO,
                ChatColor.DARK_GRAY + "Estimated rating window: ±" + firstSnapshot.matchWindow(Duration.ZERO));
        sendMessage(two, MessageType.INFO,
                ChatColor.DARK_GRAY + "Estimated rating window: ±" + secondSnapshot.matchWindow(Duration.ZERO));
    }

    private void preparePlayer(Player player) {
        player.setFallDistance(0);
        player.setFireTicks(0);
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setHealth(player.getMaxHealth());
        player.setAllowFlight(false);
        player.setFlying(false);
    }

    private void sendMessage(Player player, MessageType type, String message) {
        if (player != null) {
            ChatMessageUtil.send(player, type, message);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        ArenaMatch match = findMatch(victim.getUniqueId()).orElse(null);
        if (match == null) {
            return;
        }

        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null || !match.involves(attacker.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        if (match.getState() != ArenaMatch.State.ACTIVE) {
            event.setCancelled(true);
            return;
        }

        double finalDamage = event.getFinalDamage();
        combatTracker.recordDamage(attacker.getUniqueId(), finalDamage);

        double newHealth = victim.getHealth() - finalDamage;
        if (newHealth <= 1.0) {
            event.setCancelled(true);
            event.setDamage(0);
            finishMatch(match,
                    Optional.of(attacker.getUniqueId()),
                    Optional.of(victim.getUniqueId()),
                    VictoryReason.KNOCKOUT);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (event instanceof EntityDamageByEntityEvent) {
            return; // handled in the more specific listener
        }
        ArenaMatch match = findMatch(player.getUniqueId()).orElse(null);
        if (match == null) {
            return;
        }
        if (match.getState() != ArenaMatch.State.ACTIVE) {
            event.setCancelled(true);
            return;
        }
        double newHealth = player.getHealth() - event.getFinalDamage();
        if (newHealth <= 1.0) {
            event.setCancelled(true);
            finishMatch(match,
                    Optional.of(match.opponent(player.getUniqueId())),
                    Optional.of(player.getUniqueId()),
                    VictoryReason.KNOCKOUT);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onRegainHealth(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ArenaMatch match = findMatch(player.getUniqueId()).orElse(null);
        if (match == null || match.getState() != ArenaMatch.State.ACTIVE) {
            return;
        }
        combatTracker.recordHealing(player.getUniqueId(), event.getAmount());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        ArenaMatch match = findMatch(id).orElse(null);
        if (match != null && match.getState() != ArenaMatch.State.FINISHED) {
            finishMatch(match,
                    Optional.of(match.opponent(id)),
                    Optional.of(id),
                    VictoryReason.DISCONNECT);
        }
        queueManager.leave(id);
    }

    private Player resolveAttacker(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }

    private void finishMatch(ArenaMatch match,
                             Optional<UUID> winnerId,
                             Optional<UUID> loserId,
                             VictoryReason reason) {
        if (match.getState() == ArenaMatch.State.FINISHED) {
            return;
        }
        match.setState(ArenaMatch.State.FINISHED);

        cancelTask(countdownTasks.remove(match));
        cancelTask(timeoutTasks.remove(match));

        matchesByPlayer.remove(match.getPlayerOne());
        matchesByPlayer.remove(match.getPlayerTwo());

        Player playerOne = Bukkit.getPlayer(match.getPlayerOne());
        Player playerTwo = Bukkit.getPlayer(match.getPlayerTwo());

        if (winnerId.isPresent() && loserId.isPresent()) {
            applyRatingResults(match, winnerId.get(), loserId.get(), reason);
        } else if (reason == VictoryReason.TIMEOUT) {
            sendMessage(playerOne, MessageType.WARNING, ChatColor.GRAY + "Match ended in a draw after reaching the time limit.");
            sendMessage(playerTwo, MessageType.WARNING, ChatColor.GRAY + "Match ended in a draw after reaching the time limit.");
        }

        announceSummary(match, winnerId, loserId);

        teleportOut(playerOne);
        teleportOut(playerTwo);

        if (playerOne != null) {
            scoreboardManager.updateBoard(playerOne);
        }
        if (playerTwo != null) {
            scoreboardManager.updateBoard(playerTwo);
        }

        instanceManager.destroyInstance(match.getInstance());
    }

    private void announceSummary(ArenaMatch match,
                                 Optional<UUID> winnerId,
                                 Optional<UUID> loserId) {
        List<UUID> participants = List.of(match.getPlayerOne(), match.getPlayerTwo());
        Set<UUID> winners = winnerId.map(Set::of).orElseGet(Set::of);
        ArenaCombatSummaryBroadcaster.broadcast(combatTracker, participants, winners);
    }

    private void applyRatingResults(ArenaMatch match,
                                    UUID winnerId,
                                    UUID loserId,
                                    VictoryReason reason) {
        Player winner = Bukkit.getPlayer(winnerId);
        Player loser = Bukkit.getPlayer(loserId);
        ArenaRatingManager.RatingUpdate update = ratingManager.recordMatch(winnerId, loserId);

        int winnerNew = update.winnerAfter().rating();
        int loserNew = update.loserAfter().rating();

        sendMessage(winner, MessageType.SUCCESS,
                ChatColor.GRAY + "You defeated " + ChatColor.YELLOW + safeName(loser) + ChatColor.GRAY +
                        " (" + ChatColor.GREEN + "+" + update.winnerDelta() + ChatColor.GRAY + " ELO, now " +
                        ChatColor.GOLD + winnerNew + ChatColor.GRAY + ")");
        sendMessage(loser, MessageType.ERROR,
                ChatColor.GRAY + "You lost against " + ChatColor.YELLOW + safeName(winner) + ChatColor.GRAY +
                        " (" + ChatColor.RED + update.loserDelta() + ChatColor.GRAY + " ELO, now " +
                        ChatColor.GOLD + loserNew + ChatColor.GRAY + ")");

        maybeAnnounceTierChange(winner, update.winnerBefore().rating(), winnerNew);
        maybeAnnounceTierChange(loser, update.loserBefore().rating(), loserNew);

        if (reason == VictoryReason.DISCONNECT) {
            sendMessage(loser, MessageType.WARNING, ChatColor.RED + "You forfeited by disconnecting.");
            sendMessage(winner, MessageType.INFO, ChatColor.GRAY + "Victory awarded due to opponent disconnect.");
        }
    }

    private void maybeAnnounceTierChange(Player player, int before, int after) {
        if (player == null) {
            return;
        }
        ratingManager.buildTierChangeMessage(before, after)
                .ifPresent(message -> sendMessage(player, MessageType.INFO, message));
    }

    private void teleportOut(Player player) {
        if (player == null) {
            return;
        }
        preparePlayer(player);
        Location fallback = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0).getSpawnLocation();
        if (fallback != null) {
            player.teleport(fallback);
        }
    }

    private void cancelTask(BukkitTask task) {
        if (task != null) {
            task.cancel();
        }
    }

    private String safeName(Player player) {
        return player != null ? player.getName() : ChatColor.DARK_GRAY + "Offline";
    }

    private enum VictoryReason {
        KNOCKOUT,
        DISCONNECT,
        TIMEOUT
    }
}
