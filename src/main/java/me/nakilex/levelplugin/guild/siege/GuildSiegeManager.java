package me.nakilex.levelplugin.guild.siege;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.guild.Guild;
import me.nakilex.levelplugin.guild.GuildManager;
import me.nakilex.levelplugin.environment.EnvironmentManager;
import me.nakilex.levelplugin.utils.ChatFormatter;
import me.nakilex.levelplugin.utils.MultiLineHologram;
import me.nakilex.levelplugin.utils.FireworkUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

/**
 * Manages the periodic guild siege event.
 * Players can sign up when the broadcast is sent. After a short delay the
 * siege begins and queued players are teleported to the arena. The first guild
 * to reach 100% capture progress wins ownership of the town.
 */
public class GuildSiegeManager {
    private static final GuildSiegeManager INSTANCE = new GuildSiegeManager();
    public static GuildSiegeManager getInstance() { return INSTANCE; }

    private Main plugin;
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private BukkitTask announceTask;
    private BukkitTask countdownTask;
    private BukkitTask captureTask;

    private final Set<UUID> queue = new HashSet<>();
    private final Set<UUID> active = new HashSet<>();
    private final Set<UUID> attackers = new HashSet<>();
    private final Set<UUID> defenders = new HashSet<>();
    private final Map<UUID, Location> respawnPoints = new HashMap<>();

    private final Location center = new Location(Bukkit.getWorld("world"), 192, 73, -71);
    private final Location teleportLocation = new Location(Bukkit.getWorld("world"), 193, 67, -174);
    private static final double RADIUS = 8.0;
    private final Location ownerHologramLocation = new Location(Bukkit.getWorld("world"), 200, 76, -78);

    private File dataFile;
    private String ownerGuild = null;
    private String capturingGuild = null;
    private int progress = 0;
    private static final int CAPTURE_RATE = 1;
    private static final int NORMAL_SIEGE_DURATION = 600; // seconds
    private static final int FAST_SIEGE_DURATION = 45; // seconds
    private static final int PARTICIPATION_COINS = 1000;
    private static final int PARTICIPATION_EXP = 500;
    private static final int WIN_BONUS_COINS = 9000;
    private static final int WIN_BONUS_EXP = 2000;
    private int captureElapsed = 0;
    private MultiLineHologram ownerHologram;
    private org.bukkit.boss.BossBar bossBar;
    private boolean fastCapture = false;

    private GuildSiegeManager() {}

    public void init(Main plugin) {
        this.plugin = plugin;
        dataFile = new File(plugin.getDataFolder(), "siege.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create siege.yml: " + e.getMessage());
            }
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        ownerGuild = cfg.getString("ownerGuild", null);

        // clean up any stray holograms from previous runs
        MultiLineHologram.removeAll(ownerHologramLocation, 5, "siege_owner");

        startAnnouncements();
        updateOwnerHologram();
        applyTownVisibility();
    }

    public void save() {
        if (dataFile == null) return;
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("ownerGuild", ownerGuild);
        try {
            cfg.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save siege.yml: " + e.getMessage());
        }
    }

    private void startAnnouncements() {
        if (announceTask != null) announceTask.cancel();
        announceTask = new BukkitRunnable() {
            @Override
            public void run() {
                announce();
            }
        }.runTaskTimer(plugin, 0L, 20L * 60 * 60 * 24 * 7); // every week
    }

    private void announce() {
        queue.clear();
        respawnPoints.clear();
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        broadcastSignupMessage();
    }

    /** Broadcast the siege signup message without resetting any state. */
    public void broadcastSignupMessage() {
        String raw = "<glyph:flagleft_icon> " + ChatColor.GOLD + "" + ChatColor.BOLD + "CLICK-HERE "
                + ChatColor.GRAY + "to sign up for the guild siege! " + ChatColor.RESET + "<glyph:flagright_icon>";
        Component msg = LEGACY.deserialize(ChatFormatter.getCenteredText(raw))
                .clickEvent(ClickEvent.runCommand("/siege join"));
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getWorld().getName().equals("world")) continue;
            // Temporarily disabled quest completion check for testing
            // if (!qm.hasCompleted(p.getUniqueId(), "newbeginning")) continue;
            ChatFormatter.sendCenteredMessage(p, " ");
            p.sendMessage(msg);
            ChatFormatter.sendCenteredMessage(p, " ");
        }
    }

    /** Sign up a player for the next siege. */
    public void signUp(Player p) {
        if (plugin == null) return;
        Guild g = GuildManager.getInstance().getGuild(p.getUniqueId());
        if (g == null) {
            ChatFormatter.sendCenteredMessage(p, ChatColor.RED + "You must be in a guild to join the siege.");
            return;
        }
        if (ownerGuild != null && ownerGuild.equalsIgnoreCase(g.getName())) {
            ChatFormatter.sendCenteredMessage(p, ChatColor.RED + "Your guild is defending this siege.");
            return;
        }
        if (queue.add(p.getUniqueId())) {
            ChatFormatter.sendCenteredMessage(p, ChatColor.GREEN + "You have signed up for the siege!");
            if (queue.size() == 1) startCountdown();
        }
    }

    /** Remove a player from the queue/active sets.
     *  @return true if the player was queued or active. */
    public boolean leave(UUID id) {
        boolean removed = queue.remove(id) | active.remove(id) | attackers.remove(id) | defenders.remove(id);
        respawnPoints.remove(id);
        Player player = Bukkit.getPlayer(id);
        if (player != null) {
            if (bossBar != null) {
                bossBar.removePlayer(player);
            }
            if (removed) {
                Main.getInstance().getLocationMusicManager().stopSiege(player);
            }
        }
        if (removed && queue.isEmpty() && countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        return removed;
    }

    private void begin() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        active.clear();
        attackers.clear();
        attackers.addAll(queue);
        queue.clear();
        defenders.clear();
        if (ownerGuild != null) {
            Guild defGuild = GuildManager.getInstance().getGuild(ownerGuild);
            if (defGuild != null) {
                for (UUID id : defGuild.getMembers()) {
                    Player dp = Bukkit.getPlayer(id);
                    if (dp != null) {
                        defenders.add(id);
                    }
                }
            }
        }
        active.addAll(attackers);
        active.addAll(defenders);
        progress = 0;
        capturingGuild = null;
        runSiegeStep("hide Rowan gate for siege", () ->
                Main.getInstance().getModelGateManager().setGateHidden("rowan", true));

        if (ownerHologram != null) {
            ownerHologram.despawn();
            ownerHologram = null;
        }

        Main.getInstance().getEnvironmentManager().removeAllHolograms();

        bossBar = Bukkit.createBossBar("", BarColor.YELLOW, BarStyle.SOLID);
        bossBar.setVisible(true);
        updateBossBar();

        forEachOnline(attackers, p -> {
            bossBar.addPlayer(p);
            respawnPoints.put(p.getUniqueId(), teleportLocation.clone());
            me.nakilex.levelplugin.utils.TeleportUtils.teleportWithEffect(p, teleportLocation);
            Main.getInstance().getLocationMusicManager().startSiege(p);
        });
        forEachOnline(defenders, p -> {
            bossBar.addPlayer(p);
            respawnPoints.put(p.getUniqueId(), center.clone());
            me.nakilex.levelplugin.utils.TeleportUtils.teleportWithEffect(p, center);
            Main.getInstance().getLocationMusicManager().startSiege(p);
        });

        for (Player p : Bukkit.getOnlinePlayers()) {
            sendCenteredBlock(p, "<glyph:flagleft_icon> " + ChatColor.GRAY + "The siege has begun!" + ChatColor.RESET + " <glyph:flagright_icon>");
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 1f);
        }

        captureElapsed = 0;
        if (captureTask != null) captureTask.cancel();
        captureTask = new BukkitRunnable() {
            @Override
            public void run() {
                int siegeDuration = getSiegeDurationSeconds();
                int remaining = siegeDuration - captureElapsed;
                broadcastRemainingTime(remaining, siegeDuration);

                tickCapture();
                if (captureTask == null) {
                    return;
                }
                captureElapsed++;
                if (captureElapsed >= siegeDuration) {
                    end(ownerGuild);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void startCountdown() {
        countdownTask = new BukkitRunnable() {
            int seconds = fastCapture ? 5 : 60;
            @Override
            public void run() {
                if (queue.isEmpty()) {
                    cancel();
                    countdownTask = null;
                    return;
                }
                if (seconds <= 0) {
                    begin();
                    cancel();
                    countdownTask = null;
                    return;
                }
                if (seconds == 60 || seconds == 30 || seconds == 15 || seconds <= 5) {
                    String msg = ChatColor.GRAY + "Siege starts in " + ChatColor.YELLOW + seconds + "s";
                    forEachOnline(queue, p -> {
                        ChatFormatter.sendCenteredMessage(p, msg);
                        if (seconds <= 5) {
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f);
                        }
                    });
                }
                seconds--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void tickCapture() {
        spawnParticles();
        // Defenders inside the circle pause capture progress while they contest.
        if (hasDefenderOnPoint()) {
            capturingGuild = null;
            updateBossBar();
            return;
        }

        Map<String, Integer> counts = getAttackersOnPointByGuild();
        if (counts.isEmpty()) {
            // Keep progress when the point is temporarily empty so teams can
            // continue pushing toward completion instead of starting over.
            capturingGuild = null;
            updateBossBar();
            return;
        }

        // Determine top guild and difference
        String top = null;
        int topCount = 0;
        int second = 0;
        for (Map.Entry<String,Integer> e : counts.entrySet()) {
            int c = e.getValue();
            if (c > topCount) {
                second = topCount;
                topCount = c;
                top = e.getKey();
            } else if (c > second) {
                second = c;
            }
        }
        int diff = topCount - second;
        if (diff <= 0) return; // contested
        if (!Objects.equals(top, capturingGuild)) {
            if (capturingGuild != null && !capturingGuild.equals(top)) {
                progress = 0;
            }
            capturingGuild = top;
            updateBossBar();
        }
        int rate = fastCapture ? 50 : CAPTURE_RATE;
        progress += diff * rate;
        if (progress > 100) progress = 100;
        updateBossBar();
        if (progress >= 100) {
            end(top);
        }
    }

    private boolean hasDefenderOnPoint() {
        for (UUID id : defenders) {
            Player p = Bukkit.getPlayer(id);
            if (isInsideCaptureZone(p)) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Integer> getAttackersOnPointByGuild() {
        Map<String, Integer> counts = new HashMap<>();
        for (UUID id : attackers) {
            Player p = Bukkit.getPlayer(id);
            if (!isInsideCaptureZone(p)) {
                continue;
            }
            Guild g = GuildManager.getInstance().getGuild(id);
            if (g != null) {
                counts.merge(g.getName(), 1, Integer::sum);
            }
        }
        return counts;
    }

    private boolean isInsideCaptureZone(Player player) {
        if (player == null || center.getWorld() == null) {
            return false;
        }
        if (!player.getWorld().equals(center.getWorld())) {
            return false;
        }
        return player.getLocation().distanceSquared(center) <= RADIUS * RADIUS;
    }

    private void end(String winner) {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        if (captureTask != null) {
            captureTask.cancel();
        }
        captureTask = null;
        captureElapsed = 0;
        Set<String> participantGuilds = new HashSet<>();
        for (UUID id : active) {
            Guild g = GuildManager.getInstance().getGuild(id);
            if (g != null) participantGuilds.add(g.getName());
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                if (winner != null) {
                    sendCenteredBlock(p, ChatColor.GOLD + "Siege has ended!");
                } else {
                    sendCenteredBlock(p, ChatColor.RED + "Siege ended with no capture.");
                }
                Main.getInstance().getLocationMusicManager().stopSiege(p);
            }
        }
        active.clear();
        queue.clear();
        attackers.clear();
        defenders.clear();
        respawnPoints.clear();
        progress = 0;
        capturingGuild = null;
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }

        String msg;
        if (winner != null) {
            if (ownerGuild != null && !ownerGuild.equalsIgnoreCase(winner)) {
                me.nakilex.levelplugin.guild.Guild prev = GuildManager.getInstance().getGuild(ownerGuild);
                if (prev != null) {
                    Main.getInstance().getEnvironmentManager().neutralizeGuildTown(prev);
                }
            }
            ownerGuild = winner;
            msg = "<glyph:flagleft_icon> " + ChatColor.GOLD + winner
                    + ChatColor.GRAY + " has taken control of the town! <glyph:flagright_icon>";
            me.nakilex.levelplugin.guild.Guild g = GuildManager.getInstance().getGuild(winner);
            if (g != null) {
                Main.getInstance().getEnvironmentManager().syncGuildTown(g);
            }
            launchVictoryFireworks();
        } else {
            msg = "<glyph:flagleft_icon> " + ChatColor.RED + "No guild captured the town." + ChatColor.GRAY + " <glyph:flagright_icon>";
        }

        GuildManager gm = GuildManager.getInstance();
        for (String name : participantGuilds) {
            Guild g = gm.getGuild(name);
            awardGuildRewards(g,
                    PARTICIPATION_COINS,
                    PARTICIPATION_EXP,
                    ChatColor.GRAY + "Your guild earned "
                            + ChatColor.GOLD + PARTICIPATION_COINS + " <glyph:coins_icon>"
                            + ChatColor.GRAY + " and %s" + PARTICIPATION_EXP + " <glyph:experience_orb_icon>"
                            + ChatColor.GRAY + " for participating in the siege!");
        }
        if (winner != null) {
            Guild g = gm.getGuild(winner);
            awardGuildRewards(g,
                    WIN_BONUS_COINS,
                    WIN_BONUS_EXP,
                    ChatColor.GRAY + "Your guild won the siege and earned "
                            + ChatColor.GOLD + (PARTICIPATION_COINS + WIN_BONUS_COINS) + " <glyph:coins_icon>"
                            + ChatColor.GRAY + " and %s" + WIN_BONUS_EXP + " <glyph:experience_orb_icon>"
                            + ChatColor.GRAY + "!");
        }
        gm.save();

        for (Player p : Bukkit.getOnlinePlayers()) {
            sendCenteredBlock(p, msg);
            if (winner != null) {
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            }
        }

        runSiegeStep("restore Rowan gate after siege", () ->
                Main.getInstance().getModelGateManager().setGateHidden("rowan", false));
        save();
        applyTownVisibility();
        updateOwnerHologram();
    }

    private void broadcast(String msg) {
        forEachOnline(active, p -> ChatFormatter.sendCenteredMessage(p, msg));
    }

    private void forEachOnline(Collection<UUID> playerIds, Consumer<Player> action) {
        for (UUID id : playerIds) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                action.accept(p);
            }
        }
    }

    private void sendCenteredBlock(Player player, String message) {
        ChatFormatter.sendCenteredMessage(player, " ");
        ChatFormatter.sendCenteredMessage(player, message);
        ChatFormatter.sendCenteredMessage(player, " ");
    }

    private void awardGuildRewards(Guild guild, int coins, int exp, String messageTemplate) {
        if (guild == null) {
            return;
        }
        guild.addCoins(coins);
        guild.addExp(exp);
        String expColor = ChatFormatter.experienceColor();
        String message = String.format(messageTemplate, expColor);
        forEachOnline(guild.getMembers(), p -> sendCenteredBlock(p, message));
    }

    private void runSiegeStep(String stepName, Runnable action) {
        try {
            action.run();
        } catch (Exception ex) {
            plugin.getLogger().warning("[Siege] Failed to " + stepName + ": " + ex.getMessage());
        }
    }

    public boolean isActive(UUID id) { return active.contains(id); }
    public String getCapturingGuild() { return capturingGuild; }
    public int getProgress() { return progress; }
    public String getOwnerGuild() { return ownerGuild; }
    public boolean isSiegeRunning() { return captureTask != null; }
    public Location getRespawnLocation(UUID id) {
        return isSiegeRunning() ? respawnPoints.get(id) : null;
    }

    /** Toggle the fast-capture debug mode. */
    public boolean toggleFastCapture() {
        fastCapture = !fastCapture;
        return fastCapture;
    }

    public boolean isFastCapture() {
        return fastCapture;
    }

    private int getSiegeDurationSeconds() {
        return fastCapture ? FAST_SIEGE_DURATION : NORMAL_SIEGE_DURATION;
    }

    private void broadcastRemainingTime(int remaining, int totalDuration) {
        if (remaining <= 0) {
            return;
        }
        if (remaining <= 5) {
            forEachOnline(active, p -> {
                ChatFormatter.sendCenteredMessage(p, ChatColor.GRAY + "Siege ends in "
                        + ChatColor.YELLOW + remaining + "s");
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f);
            });
            return;
        }

        if (totalDuration >= 600) {
            if (remaining == 600) broadcast(ChatColor.GRAY + "Siege ends in " + ChatColor.YELLOW + "10m");
            else if (remaining == 300) broadcast(ChatColor.GRAY + "Siege ends in " + ChatColor.YELLOW + "5m");
            else if (remaining == 60) broadcast(ChatColor.GRAY + "Siege ends in " + ChatColor.YELLOW + "1m");
            else if (remaining == 30) broadcast(ChatColor.GRAY + "Siege ends in " + ChatColor.YELLOW + "30s");
            else if (remaining == 10) broadcast(ChatColor.GRAY + "Siege ends in " + ChatColor.YELLOW + "10s");
            return;
        }

        if (remaining == 30 || remaining == 15 || remaining == 10) {
            broadcast(ChatColor.GRAY + "Siege ends in " + ChatColor.YELLOW + remaining + "s");
        }
    }

    public int getRemainingSeconds() {
        return captureTask != null ? Math.max(0, getSiegeDurationSeconds() - captureElapsed) : 0;
    }

    public String getFormattedRemaining() {
        int sec = getRemainingSeconds();
        int m = sec / 60;
        int s = sec % 60;
        return String.format("%d:%02d", m, s);
    }

    private void spawnParticles() {
        if (center.getWorld() == null) {
            return;
        }
        for (int i = 0; i < 40; i++) {
            double angle = (2 * Math.PI * i) / 40;
            double x = center.getX() + RADIUS * Math.cos(angle);
            double z = center.getZ() + RADIUS * Math.sin(angle);
            center.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, x, center.getY(), z, 1, 0,0,0,0);
        }
    }

    private void launchVictoryFireworks() {
        Location base = center.clone();
        base.setY(Math.max(70, center.getY()));
        FireworkUtil.burst(base, 9);
    }

    /**
     * Check if two players are opposing siege participants.
     */
    public boolean areSiegeOpponents(UUID a, UUID b) {
        if (!active.contains(a) || !active.contains(b)) return false;
        Guild gA = GuildManager.getInstance().getGuild(a);
        Guild gB = GuildManager.getInstance().getGuild(b);
        if (gA == null || gB == null) return false;
        return !Objects.equals(gA.getName(), gB.getName());
    }

    /**
     * Hide town structures and holograms from players not in the owning guild.
     */
    private void applyTownVisibility() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            refreshTownVisibility(p);
        }
    }

    public void refreshTownVisibility(Player p) {
        EnvironmentManager env = Main.getInstance().getEnvironmentManager();
        env.hideAllBuildingHolograms(p);
        env.removeAllBuildingHolograms(p.getUniqueId());
        Guild g = GuildManager.getInstance().getGuild(p.getUniqueId());
        boolean allowed = ownerGuild == null || (g != null && ownerGuild.equalsIgnoreCase(g.getName()));
        if (allowed) {
            if (g != null && !g.getLeader().equals(p.getUniqueId())) {
                env.shareTownWithMember(g.getLeader(), p.getUniqueId());
            }
            if (!env.isTownLoaded(p)) {
                env.markTownLoaded(p, true);
            }
            env.initializePlayer(p);
        } else {
            env.removeGuildMember(p.getUniqueId());
            env.unloadPlayerTown(p);
            env.markTownLoaded(p, false);
        }
    }

    /**
     * Clear ownership if the owning guild disbands and remove any queued
     * or active members belonging to that guild.
     */
    public void handleGuildDisband(String name, java.util.Collection<UUID> members) {
        if (ownerGuild != null && ownerGuild.equalsIgnoreCase(name)) {
            ownerGuild = null;
            save();
            applyTownVisibility();
            updateOwnerHologram();
        }
        for (UUID id : members) {
            if (leave(id)) {
                Player p = Bukkit.getPlayer(id);
                if (p != null) {
                    ChatFormatter.sendCenteredMessage(p,
                            ChatColor.RED + "Your guild disbanded so your siege sign-up was cancelled.");
                }
            }
        }
    }

    /**
     * Debug helper to assign the castle to a guild immediately.
     * @return true if ownership was updated
     */
    public boolean debugAssignOwner(String guildName) {
        if (guildName == null || guildName.isBlank()) {
            return false;
        }
        GuildManager gm = GuildManager.getInstance();
        Guild guild = gm.getGuild(guildName);
        if (guild == null) {
            return false;
        }

        if (ownerGuild != null && !ownerGuild.equalsIgnoreCase(guild.getName())) {
            Guild prev = gm.getGuild(ownerGuild);
            if (prev != null) {
                Main.getInstance().getEnvironmentManager().neutralizeGuildTown(prev);
            }
        }

        ownerGuild = guild.getName();
        Main.getInstance().getEnvironmentManager().syncGuildTown(guild);
        updateOwnerHologram();
        save();
        applyTownVisibility();
        return true;
    }

    private void updateBossBar() {
        if (bossBar == null) return;
        String guild = capturingGuild != null ? capturingGuild : "None";
        String title = ChatColor.GOLD.toString() + ChatColor.BOLD + "SIEGE "
                + ChatColor.DARK_GRAY + "\u2013 " + ChatColor.WHITE + guild
                + ChatColor.GRAY + " [" + ChatColor.DARK_GRAY + progress + "%" + ChatColor.GRAY + "]";
        bossBar.setTitle(title);
        bossBar.setProgress(progress / 100.0);
    }

    private void updateOwnerHologram() {
        if (bossBar != null) return; // during active siege we show boss bar instead
        if (ownerHologram == null) {
            ownerHologram = new MultiLineHologram(ownerHologramLocation, "siege_owner");
        }
        String title = ChatColor.DARK_GRAY + "[" + ChatColor.YELLOW + "Rowan Castle" + ChatColor.DARK_GRAY + "]";
        String spacer = " ";
        String guildLine = ownerGuild != null
                ? ChatColor.GOLD + "<glyph:flagleft_icon> " + ownerGuild + " <glyph:flagright_icon>"
                : ChatColor.GRAY + "<glyph:flagleft_icon> None <glyph:flagright_icon>";
        ownerHologram.setLines(java.util.Arrays.asList(title, spacer, guildLine, spacer));
    }

    /** Remove holograms and cancel tasks on shutdown. */
    public void cleanup() {
        if (announceTask != null) announceTask.cancel();
        if (countdownTask != null) countdownTask.cancel();
        if (captureTask != null) captureTask.cancel();
        attackers.clear();
        defenders.clear();
        active.clear();
        queue.clear();
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
        if (ownerHologram != null) {
            ownerHologram.despawn();
            ownerHologram = null;
        }
        MultiLineHologram.removeAll(ownerHologramLocation, 5, "siege_owner");
    }
}
