package me.nakilex.levelplugin.guild.siege;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.guild.Guild;
import me.nakilex.levelplugin.guild.GuildManager;
import me.nakilex.levelplugin.environment.EnvironmentManager;
import me.nakilex.levelplugin.utils.ChatFormatter;
import me.nakilex.levelplugin.utils.MultiLineHologram;
import me.nakilex.levelplugin.utils.FireworkUtil;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;

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
    private BukkitTask announceTask;
    private BukkitTask countdownTask;
    private BukkitTask captureTask;

    private final Set<UUID> queue = new HashSet<>();
    private final Set<UUID> active = new HashSet<>();

    private final Location center = new Location(Bukkit.getWorld("world"), 192, 73, -71);
    private final Location teleportLocation = new Location(Bukkit.getWorld("world"), 193, 67, -174);
    private static final double RADIUS = 8.0;
    private final Location ownerHologramLocation = new Location(Bukkit.getWorld("world"), 200, 76, -78);

    private File dataFile;
    private String ownerGuild = null;
    private String capturingGuild = null;
    private int progress = 0;
    private static final int CAPTURE_RATE = 1;
    private static final int SIEGE_DURATION = 600; // seconds
    private int captureElapsed = 0;
    private BossBar progressBar;
    private MultiLineHologram ownerHologram;

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

        // clean up any stray owner holograms from previous runs
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
        TextComponent msg = new TextComponent(ChatFormatter.getCenteredText(raw));
        msg.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/siege join"));
        QuestManager qm = Main.getInstance().getQuestManager();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getWorld().getName().equals("world")) continue;
            // Temporarily disabled quest completion check for testing
            // if (!qm.hasCompleted(p.getUniqueId(), "newbeginning")) continue;
            ChatFormatter.sendCenteredMessage(p, " ");
            p.spigot().sendMessage(msg);
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
        if (queue.add(p.getUniqueId())) {
            ChatFormatter.sendCenteredMessage(p, ChatColor.GREEN + "You have signed up for the siege!");
            if (queue.size() == 1) startCountdown();
        }
    }

    /** Remove a player from the queue/active sets.
     *  @return true if the player was queued or active. */
    public boolean leave(UUID id) {
        boolean removed = queue.remove(id) | active.remove(id);
        if (removed && progressBar != null) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) progressBar.removePlayer(p);
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
        active.addAll(queue);
        queue.clear();
        progress = 0;
        capturingGuild = null;

        if (ownerHologram != null) {
            ownerHologram.despawn();
            ownerHologram = null;
        }

        Main.getInstance().getEnvironmentManager().removeAllHolograms();

        progressBar = Bukkit.createBossBar("", BarColor.RED, BarStyle.SOLID);
        updateBossBar();

        for (UUID id : active) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                me.nakilex.levelplugin.utils.TeleportUtils.teleportWithEffect(p, teleportLocation);
                progressBar.addPlayer(p);
            }
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            ChatFormatter.sendCenteredMessage(p, " ");
            ChatFormatter.sendCenteredMessage(p, "<glyph:flagleft_icon> " + ChatColor.GRAY + "The siege has begun!" + ChatColor.RESET + " <glyph:flagright_icon>");
            ChatFormatter.sendCenteredMessage(p, " ");
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 1f);
        }

        captureElapsed = 0;
        if (captureTask != null) captureTask.cancel();
        captureTask = new BukkitRunnable() {
            @Override
            public void run() {
                int remaining = SIEGE_DURATION - captureElapsed;
                switch (remaining) {
                    case 600: broadcast(ChatColor.GRAY + "Siege ends in " + ChatColor.YELLOW + "10m"); break;
                    case 300: broadcast(ChatColor.GRAY + "Siege ends in " + ChatColor.YELLOW + "5m"); break;
                    case 60: broadcast(ChatColor.GRAY + "Siege ends in " + ChatColor.YELLOW + "1m"); break;
                    case 30: broadcast(ChatColor.GRAY + "Siege ends in " + ChatColor.YELLOW + "30s"); break;
                    case 10: broadcast(ChatColor.GRAY + "Siege ends in " + ChatColor.YELLOW + "10s"); break;
                    case 5: case 4: case 3: case 2: case 1:
                        for (UUID id : active) {
                            Player p = Bukkit.getPlayer(id);
                            if (p != null) {
                                ChatFormatter.sendCenteredMessage(p, ChatColor.GRAY + "Siege ends in "
                                        + ChatColor.YELLOW + remaining + "s");
                                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f);
                            }
                        }
                        break;
                }

                tickCapture();
                captureElapsed++;
                if (captureElapsed >= SIEGE_DURATION) {
                    end(ownerGuild);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void startCountdown() {
        countdownTask = new BukkitRunnable() {
            int seconds = 60;
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
                    for (UUID id : queue) {
                        Player p = Bukkit.getPlayer(id);
                        if (p != null) {
                            ChatFormatter.sendCenteredMessage(p, msg);
                            if (seconds <= 5) {
                                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f);
                            }
                        }
                    }
                }
                seconds--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void tickCapture() {
        spawnParticles();
        Map<String, Integer> counts = new HashMap<>();
        for (UUID id : active) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;
            if (!p.getWorld().equals(center.getWorld())) continue;
            if (p.getLocation().distanceSquared(center) > RADIUS * RADIUS) continue;
            Guild g = GuildManager.getInstance().getGuild(id);
            if (g != null) counts.merge(g.getName(), 1, Integer::sum);
        }
        if (counts.isEmpty()) {
            capturingGuild = null;
            progress = 0;
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
        if (!top.equals(capturingGuild)) {
            capturingGuild = top;
            progress = 0;
            updateBossBar();
        }
        progress += diff * CAPTURE_RATE;
        if (progress > 100) progress = 100;
        updateBossBar();
        if (progress >= 100) {
            end(capturingGuild);
        }
    }

    private void end(String winner) {
        if (captureTask != null) captureTask.cancel();
        captureTask = null;
        captureElapsed = 0;
        for (UUID id : active) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                ChatFormatter.sendCenteredMessage(p, " ");
                if (winner != null) {
                    ChatFormatter.sendCenteredMessage(p, ChatColor.GOLD + "Siege has ended!");
                } else {
                    ChatFormatter.sendCenteredMessage(p, ChatColor.RED + "Siege ended with no capture.");
                }
                ChatFormatter.sendCenteredMessage(p, " ");
            }
        }
        active.clear();
        queue.clear();
        progress = 0;
        capturingGuild = null;
        if (progressBar != null) {
            progressBar.removeAll();
            progressBar = null;
        }

        String msg;
        if (winner != null) {
            EnvironmentManager env = Main.getInstance().getEnvironmentManager();
            String previous = ownerGuild;
            if (previous != null && !previous.equalsIgnoreCase(winner)) {
                Guild prevGuild = GuildManager.getInstance().getGuild(previous);
                env.clearGuildTown(prevGuild);
            }
            ownerGuild = winner;
            Guild g = GuildManager.getInstance().getGuild(winner);
            if (g != null) {
                for (UUID id : g.getMembers()) {
                    env.resetTown(id);
                }
                env.syncGuildTown(g);
            }
            msg = "<glyph:flagleft_icon> " + ChatColor.GOLD + winner
                    + ChatColor.GRAY + " has taken control of the town! <glyph:flagright_icon>";
            launchVictoryFireworks();
        } else {
            msg = "<glyph:flagleft_icon> " + ChatColor.RED + "No guild captured the town." + ChatColor.GRAY + " <glyph:flagright_icon>";
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            ChatFormatter.sendCenteredMessage(p, " ");
            ChatFormatter.sendCenteredMessage(p, msg);
            ChatFormatter.sendCenteredMessage(p, " ");
            if (winner != null) {
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            }
        }
        save();
        applyTownVisibility();
        updateOwnerHologram();
    }

    private void broadcast(String msg) {
        for (UUID id : active) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) ChatFormatter.sendCenteredMessage(p, msg);
        }
    }

    public boolean isActive(UUID id) { return active.contains(id); }
    public String getCapturingGuild() { return capturingGuild; }
    public int getProgress() { return progress; }
    public String getOwnerGuild() { return ownerGuild; }
    public boolean isSiegeRunning() { return captureTask != null; }

    public int getRemainingSeconds() {
        return captureTask != null ? Math.max(0, SIEGE_DURATION - captureElapsed) : 0;
    }

    public String getFormattedRemaining() {
        int sec = getRemainingSeconds();
        int m = sec / 60;
        int s = sec % 60;
        return String.format("%d:%02d", m, s);
    }

    private void spawnParticles() {
        for (int i = 0; i < 40; i++) {
            double angle = (2 * Math.PI * i) / 40;
            double x = center.getX() + RADIUS * Math.cos(angle);
            double z = center.getZ() + RADIUS * Math.sin(angle);
            center.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, x, center.getY(), z, 1, 0,0,0,0);
        }
    }

    private void launchVictoryFireworks() {
        FireworkUtil.launchRandomFireworkBurst(center, 70, 9, 70, 10);
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
        plugin.getLogger().info("[SiegeDebug] Refresh visibility for " + p.getName());
        EnvironmentManager env = Main.getInstance().getEnvironmentManager();
        env.hideAllBuildingHolograms(p);
        env.removeAllBuildingHolograms(p.getUniqueId());
        if (ownerGuild == null) {
            env.shareTownWithMember(EnvironmentManager.UNCAPTURED_TOWN_ID, p.getUniqueId());
            if (!env.isTownLoaded(p)) {
                env.markTownLoaded(p, true);
            }
            env.initializePlayer(p);
            plugin.getLogger().info("[SiegeDebug] Initialized neutral town for " + p.getName());
            return;
        }

        Guild g = GuildManager.getInstance().getGuild(p.getUniqueId());
        String gName = g != null ? g.getName() : "none";
        boolean allowed = g != null && ownerGuild.equalsIgnoreCase(g.getName());
        plugin.getLogger().info("[SiegeDebug] owner=" + ownerGuild + " playerGuild=" + gName + " allowed=" + allowed);
        if (allowed) {
            if (!g.getLeader().equals(p.getUniqueId())) {
                env.shareTownWithMember(g.getLeader(), p.getUniqueId());
            }
            if (!env.isTownLoaded(p)) {
                env.markTownLoaded(p, true);
            }
            env.initializePlayer(p);
            plugin.getLogger().info("[SiegeDebug] Initialized holograms for " + p.getName());
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

    private void updateBossBar() {
        if (progressBar == null) return;
        String guild = capturingGuild != null
                ? ChatColor.WHITE + capturingGuild
                : ChatColor.GRAY + "None";
        String title = ChatColor.GOLD + "" + ChatColor.BOLD + "SIEGE "
                + ChatColor.DARK_GRAY + "- " + guild
                + ChatColor.DARK_GRAY + " [" + ChatColor.GRAY + progress + "%" + ChatColor.DARK_GRAY + "]";
        progressBar.setTitle(title);
        progressBar.setProgress(progress / 100.0);
    }

    private void updateOwnerHologram() {
        if (progressBar != null) return; // during active siege we show progress instead
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
        if (progressBar != null) {
            progressBar.removeAll();
            progressBar = null;
        }
        if (ownerHologram != null) {
            ownerHologram.despawn();
            ownerHologram = null;
        }
        MultiLineHologram.removeAll(ownerHologramLocation, 5, "siege_owner");
    }
}
