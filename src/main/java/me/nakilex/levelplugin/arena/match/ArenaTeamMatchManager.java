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
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;

/**
 * Handles the lifecycle of 2v2 arena matches including countdowns, spectator
 * mode on death or disconnect and declaring a winner once both opponents on a
 * team have fallen.
 */
public class ArenaTeamMatchManager implements Listener {
    private final Main plugin;
    private final ArenaQueueManager queueManager;
    private final ArenaInstanceManager instanceManager;
    private final ArenaRatingManager ratingManager;
    private final PlayerScoreboardManager scoreboardManager;

    private final Map<UUID, ArenaTeamMatch> matchesByPlayer = new HashMap<>();
    private final Map<ArenaTeamMatch, BukkitTask> countdownTasks = new HashMap<>();
    private final Map<ArenaTeamMatch, BukkitTask> timeoutTasks = new HashMap<>();

    private static final long MATCH_TIMEOUT_TICKS = 20L * 8 * 60; // 8 minutes

    public ArenaTeamMatchManager(Main plugin,
                                 ArenaQueueManager queueManager,
                                 ArenaInstanceManager instanceManager,
                                 ArenaRatingManager ratingManager,
                                 PlayerScoreboardManager scoreboardManager) {
        this.plugin = plugin;
        this.queueManager = queueManager;
        this.instanceManager = instanceManager;
        this.ratingManager = ratingManager;
        this.scoreboardManager = scoreboardManager;
    }

    public boolean isInMatch(UUID playerId) {
        return matchesByPlayer.containsKey(playerId);
    }

    public Optional<ArenaTeamMatch> findMatch(UUID playerId) {
        return Optional.ofNullable(matchesByPlayer.get(playerId));
    }

    public void startMatch(ArenaQueueManager.QueueEntry first, ArenaQueueManager.QueueEntry second) {
        if (first.mode() != ArenaMode.TWO_VS_TWO || second.mode() != ArenaMode.TWO_VS_TWO) {
            return;
        }
        List<UUID> teamOneIds = new ArrayList<>(first.members());
        List<UUID> teamTwoIds = new ArrayList<>(second.members());
        List<Player> teamOne = resolveOnlinePlayers(teamOneIds);
        List<Player> teamTwo = resolveOnlinePlayers(teamTwoIds);

        if (teamOne.size() != teamOneIds.size()) {
            notifyTeamOffline(teamTwo, second);
            queueManager.requeue(second);
            return;
        }
        if (teamTwo.size() != teamTwoIds.size()) {
            notifyTeamOffline(teamOne, first);
            queueManager.requeue(first);
            return;
        }

        for (Player player : teamOne) {
            if (isInMatch(player.getUniqueId())) {
                queueManager.requeue(first);
                queueManager.requeue(second);
                return;
            }
        }
        for (Player player : teamTwo) {
            if (isInMatch(player.getUniqueId())) {
                queueManager.requeue(first);
                queueManager.requeue(second);
                return;
            }
        }

        ArenaInstance instance = instanceManager.createInstance();
        if (instance == null) {
            notifyInstanceFailure(teamOne, teamTwo);
            queueManager.requeue(first);
            queueManager.requeue(second);
            return;
        }

        ArenaTeamMatch match = new ArenaTeamMatch(teamOneIds, teamTwoIds, instance);
        registerMatch(match);

        prepareTeam(teamOne);
        prepareTeam(teamTwo);

        teleportTeam(teamOne, instance.getFirstSpawn(), true);
        teleportTeam(teamTwo, instance.getSecondSpawn(), false);

        announceMatchFound(teamOne, teamTwo);
        runCountdown(match, teamOne, teamTwo);
        scheduleTimeout(match);
    }

    private void registerMatch(ArenaTeamMatch match) {
        for (UUID playerId : match.allPlayers()) {
            matchesByPlayer.put(playerId, match);
        }
    }

    private void unregisterMatch(ArenaTeamMatch match) {
        for (UUID playerId : match.allPlayers()) {
            matchesByPlayer.remove(playerId);
        }
    }

    private List<Player> resolveOnlinePlayers(List<UUID> members) {
        List<Player> players = new ArrayList<>();
        for (UUID id : members) {
            Player player = Bukkit.getPlayer(id);
            if (player != null && player.isOnline()) {
                players.add(player);
            }
        }
        return players;
    }

    private void notifyTeamOffline(List<Player> team, ArenaQueueManager.QueueEntry entry) {
        for (Player player : team) {
            sendMessage(player, MessageType.WARNING,
                    ChatColor.GRAY + "Opponents left before the match started. Re-queuing you.");
        }
    }

    private void notifyInstanceFailure(List<Player> teamOne, List<Player> teamTwo) {
        for (Player player : teamOne) {
            sendMessage(player, MessageType.ERROR, ChatColor.RED + "Arena instance unavailable. You have been re-queued.");
        }
        for (Player player : teamTwo) {
            sendMessage(player, MessageType.ERROR, ChatColor.RED + "Arena instance unavailable. You have been re-queued.");
        }
    }

    private void prepareTeam(List<Player> team) {
        for (Player player : team) {
            player.setFallDistance(0);
            player.setFireTicks(0);
            player.setFoodLevel(20);
            player.setSaturation(20f);
            player.setHealth(player.getMaxHealth());
            player.setAllowFlight(false);
            player.setFlying(false);
            player.setGameMode(GameMode.SURVIVAL);
        }
    }

    private void teleportTeam(List<Player> team, Location base, boolean firstTeam) {
        Location primary = base.clone();
        Location secondary = base.clone().add(firstTeam ? 1.5 : -1.5, 0, 0);
        if (team.size() > 0) {
            team.get(0).teleport(primary);
        }
        if (team.size() > 1) {
            team.get(1).teleport(secondary);
        }
    }

    private void announceMatchFound(List<Player> teamOne, List<Player> teamTwo) {
        String opponents = formatTeamNames(teamTwo);
        for (Player player : teamOne) {
            sendMessage(player, MessageType.INFO,
                    ChatColor.GRAY + "Matched against " + opponents + ChatColor.GRAY + ".");
        }
        opponents = formatTeamNames(teamOne);
        for (Player player : teamTwo) {
            sendMessage(player, MessageType.INFO,
                    ChatColor.GRAY + "Matched against " + opponents + ChatColor.GRAY + ".");
        }
    }

    private void runCountdown(ArenaTeamMatch match, List<Player> teamOne, List<Player> teamTwo) {
        BukkitTask task = new BukkitRunnable() {
            private int seconds = 5;

            @Override
            public void run() {
                if (match.getState() == ArenaTeamMatch.State.FINISHED) {
                    cancel();
                    return;
                }
                if (!allOnline(match)) {
                    cancel();
                    return;
                }
                if (seconds <= 0) {
                    match.setState(ArenaTeamMatch.State.ACTIVE);
                    for (Player player : teamOne) {
                        player.sendTitle(ChatColor.GREEN + "FIGHT!", ChatColor.DARK_GRAY + "Good luck!", 0, 30, 10);
                    }
                    for (Player player : teamTwo) {
                        player.sendTitle(ChatColor.GREEN + "FIGHT!", ChatColor.DARK_GRAY + "Good luck!", 0, 30, 10);
                    }
                    cancel();
                    return;
                }
                String title = ChatColor.YELLOW + String.valueOf(seconds);
                for (Player player : teamOne) {
                    player.sendTitle(ChatColor.GRAY + "Match starting in", title, 0, 25, 10);
                }
                for (Player player : teamTwo) {
                    player.sendTitle(ChatColor.GRAY + "Match starting in", title, 0, 25, 10);
                }
                seconds--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
        countdownTasks.put(match, task);
    }

    private void scheduleTimeout(ArenaTeamMatch match) {
        BukkitTask timeout = new BukkitRunnable() {
            @Override
            public void run() {
                if (match.getState() == ArenaTeamMatch.State.FINISHED) {
                    return;
                }
                finishMatch(match, match.getTeamOne(), match.getTeamTwo(), ChatColor.GRAY + "Match ended in a draw.", false);
            }
        }.runTaskLater(plugin, MATCH_TIMEOUT_TICKS);
        timeoutTasks.put(match, timeout);
    }

    private boolean allOnline(ArenaTeamMatch match) {
        for (UUID id : match.allPlayers()) {
            Player player = Bukkit.getPlayer(id);
            if (player == null || !player.isOnline()) {
                return false;
            }
        }
        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        ArenaTeamMatch match = findMatch(victim.getUniqueId()).orElse(null);
        if (match == null) {
            return;
        }

        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null || !match.involves(attacker.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        if (match.getState() != ArenaTeamMatch.State.ACTIVE) {
            event.setCancelled(true);
            return;
        }

        double newHealth = victim.getHealth() - event.getFinalDamage();
        if (newHealth <= 1.0) {
            event.setCancelled(true);
            event.setDamage(0);
            eliminatePlayer(match, victim, ChatColor.YELLOW + attacker.getName() + ChatColor.GRAY + " defeated you!");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        if (event instanceof EntityDamageByEntityEvent) {
            return;
        }
        ArenaTeamMatch match = findMatch(victim.getUniqueId()).orElse(null);
        if (match == null) {
            return;
        }
        if (match.getState() != ArenaTeamMatch.State.ACTIVE) {
            event.setCancelled(true);
            return;
        }
        double newHealth = victim.getHealth() - event.getFinalDamage();
        if (newHealth <= 1.0) {
            event.setCancelled(true);
            eliminatePlayer(match, victim, ChatColor.RED + "You were eliminated!");
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        ArenaTeamMatch match = findMatch(event.getPlayer().getUniqueId()).orElse(null);
        if (match == null || match.getState() != ArenaTeamMatch.State.COUNTDOWN) {
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

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        ArenaTeamMatch match = findMatch(id).orElse(null);
        if (match == null || match.getState() == ArenaTeamMatch.State.FINISHED) {
            return;
        }
        eliminatePlayer(match, event.getPlayer(), ChatColor.GRAY + "You left the match and became a spectator.");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        ArenaTeamMatch match = findMatch(id).orElse(null);
        if (match == null) {
            return;
        }
        Player player = event.getPlayer();
        player.teleport(match.getInstance().getFirstSpawn());
        player.setGameMode(GameMode.SPECTATOR);
        sendMessage(player, MessageType.INFO, ChatColor.GRAY + "You rejoined as a spectator.");
    }

    private void eliminatePlayer(ArenaTeamMatch match, Player player, String message) {
        UUID id = player.getUniqueId();
        if (match.isSpectator(id)) {
            return;
        }
        match.markEliminated(id);
        player.setGameMode(GameMode.SPECTATOR);
        sendMessage(player, MessageType.ERROR, message);

        boolean teamOneEliminated = match.isTeamEliminated(true);
        boolean teamTwoEliminated = match.isTeamEliminated(false);

        if (teamOneEliminated || teamTwoEliminated) {
            List<UUID> winners = teamOneEliminated ? match.getTeamTwo() : match.getTeamOne();
            List<UUID> losers = teamOneEliminated ? match.getTeamOne() : match.getTeamTwo();
            finishMatch(match, winners, losers,
                    ChatColor.YELLOW + composeNames(winners) + ChatColor.GRAY + " won the match!", true);
        }
    }

    private String composeNames(List<UUID> team) {
        List<String> names = new ArrayList<>();
        for (UUID id : team) {
            Player player = Bukkit.getPlayer(id);
            String name = player != null ? player.getName() : Bukkit.getOfflinePlayer(id).getName();
            if (name == null) name = "Unknown";
            names.add(name);
        }
        return String.join(ChatColor.GRAY + " & " + ChatColor.YELLOW, names);
    }

    private String formatTeamNames(List<Player> team) {
        List<String> names = new ArrayList<>();
        for (Player player : team) {
            if (player != null) {
                names.add(player.getName());
            }
        }
        if (names.isEmpty()) {
            return ChatColor.YELLOW + "Unknown";
        }
        return ChatColor.YELLOW + String.join(ChatColor.GRAY + " & " + ChatColor.YELLOW, names);
    }

    private void finishMatch(ArenaTeamMatch match,
                             List<UUID> winners,
                             List<UUID> losers,
                             String message,
                             boolean awardRating) {
        if (match.getState() == ArenaTeamMatch.State.FINISHED) {
            return;
        }
        match.setState(ArenaTeamMatch.State.FINISHED);
        cancelTask(countdownTasks.remove(match));
        cancelTask(timeoutTasks.remove(match));
        unregisterMatch(match);

        for (UUID id : winners) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                sendMessage(player, MessageType.SUCCESS, message);
            }
        }
        for (UUID id : losers) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                sendMessage(player, MessageType.WARNING, ChatColor.GRAY + "Better luck next time!");
            }
        }

        ArenaRatingManager.MultiRatingUpdate ratingUpdate = null;
        if (awardRating && ratingManager != null && !winners.isEmpty() && !losers.isEmpty()) {
            ratingUpdate = ratingManager.recordMatch(winners, losers, ArenaMode.TWO_VS_TWO.ratingCategory());
        }

        if (ratingUpdate != null) {
            for (UUID id : winners) {
                sendRatingFeedback(id, true, ratingUpdate);
            }
            for (UUID id : losers) {
                sendRatingFeedback(id, false, ratingUpdate);
            }
        }

        for (UUID id : match.allPlayers()) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                resetPlayer(player);
                scoreboardManager.updateBoard(player);
            }
        }

        instanceManager.destroyInstance(match.getInstance());
    }

    private void sendRatingFeedback(UUID playerId, boolean winner, ArenaRatingManager.MultiRatingUpdate update) {
        if (update == null) {
            return;
        }
        ArenaRatingManager.RatingChange change = update.change(playerId);
        if (change == null) {
            return;
        }
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            return;
        }
        int delta = change.delta();
        int rating = change.after().rating();
        ChatColor deltaColor = delta >= 0 ? ChatColor.GREEN : ChatColor.RED;
        String deltaText = delta >= 0 ? "+" + delta : Integer.toString(delta);
        String prefix = winner ? ChatColor.GRAY + "You won with your team " : ChatColor.GRAY + "Your team was defeated ";
        MessageType type = winner ? MessageType.SUCCESS : MessageType.ERROR;
        String message = prefix + ChatColor.GRAY + "(" + deltaColor + deltaText + ChatColor.GRAY + " ELO, now "
                + ChatColor.GOLD + rating + ChatColor.GRAY + ")";
        sendMessage(player, type, message);
        ratingManager.buildTierChangeMessage(change.before().rating(), rating)
                .ifPresent(tierMessage -> sendMessage(player, MessageType.INFO, tierMessage));
    }

    private void resetPlayer(Player player) {
        player.setGameMode(GameMode.SURVIVAL);
        player.setAllowFlight(false);
        player.setFlying(false);
        Location fallback = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0).getSpawnLocation();
        if (fallback != null) {
            player.teleport(fallback);
        }
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

    private void cancelTask(BukkitTask task) {
        if (task != null) {
            task.cancel();
        }
    }

    private void sendMessage(Player player, MessageType type, String message) {
        if (player != null) {
            ChatMessageUtil.send(player, type, message);
        }
    }
}

