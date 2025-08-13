package me.nakilex.levelplugin.guild.siege;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.guild.Guild;
import me.nakilex.levelplugin.guild.GuildManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import me.nakilex.levelplugin.utils.ChatFormatter;
import me.nakilex.levelplugin.utils.MultiLineHologram;

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
    private final Location teleportLocation = new Location(Bukkit.getWorld("world"), 193, 66, -174);
    private static final double RADIUS = 8.0;
    private final Location hologramLocation = new Location(Bukkit.getWorld("world"), 192, 78, -71);

    private String ownerGuild = null;
    private String capturingGuild = null;
    private int progress = 0;
    private int lastAnnounce = 0;
    private MultiLineHologram progressHologram;

    private static final String PREFIX = ChatColor.YELLOW + "[Siege] " + ChatColor.WHITE;

    private GuildSiegeManager() {}

    public void init(Main plugin) {
        this.plugin = plugin;
        startAnnouncements();
    }

    private void startAnnouncements() {
        if (announceTask != null) announceTask.cancel();
        announceTask = new BukkitRunnable() {
            @Override
            public void run() {
                announce();
            }
        }.runTaskTimer(plugin, 0L, 20L * 60); // every minute
    }

    private void announce() {
        queue.clear();
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        String raw = PREFIX + "Click here to join the guild siege!";
        TextComponent msg = new TextComponent(ChatFormatter.getCenteredText(raw));
        msg.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/siege join"));
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.spigot().sendMessage(msg);
        }
    }

    /** Sign up a player for the next siege. */
    public void signUp(Player p) {
        if (plugin == null) return;
        Guild g = GuildManager.getInstance().getGuild(p.getUniqueId());
        if (g == null) {
            ChatFormatter.sendCenteredMessage(p, PREFIX + ChatColor.RED + "You must be in a guild to join the siege.");
            return;
        }
        if (queue.add(p.getUniqueId())) {
            ChatFormatter.sendCenteredMessage(p, PREFIX + ChatColor.GREEN + "You have signed up for the siege!");
            if (queue.size() == 1) startCountdown();
        }
    }

    public void leave(UUID id) {
        queue.remove(id);
        active.remove(id);
        if (queue.isEmpty() && countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
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
        lastAnnounce = 0;
        capturingGuild = null;

        progressHologram = new MultiLineHologram(hologramLocation);
        updateHologram();

        for (UUID id : active) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) p.teleport(teleportLocation);
        }

        if (captureTask != null) captureTask.cancel();
        captureTask = new BukkitRunnable() {
            int elapsed = 0;
            @Override
            public void run() {
                tickCapture();
                elapsed++;
                if (elapsed >= 120) {
                    end(ownerGuild);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void startCountdown() {
        countdownTask = new BukkitRunnable() {
            int seconds = 10;
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
                String msg = PREFIX + ChatColor.YELLOW + "Siege starts in " + seconds + "s";
                for (UUID id : queue) {
                    Player p = Bukkit.getPlayer(id);
                    if (p != null) ChatFormatter.sendCenteredMessage(p, msg);
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
            lastAnnounce = 0;
            updateHologram();
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
            lastAnnounce = 0;
            updateHologram();
        }
        progress += diff;
        if (progress > 100) progress = 100;
        if (progress >= lastAnnounce + 5) {
            lastAnnounce = progress - (progress % 5);
            broadcast(ChatColor.YELLOW + capturingGuild + ChatColor.WHITE + " is capturing [" + progress + "%]");
            updateHologram();
        }
        if (progress >= 100) {
            end(capturingGuild);
        }
    }

    private void end(String winner) {
        if (captureTask != null) captureTask.cancel();
        captureTask = null;
        for (UUID id : active) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                if (winner != null) {
                    ChatFormatter.sendCenteredMessage(p, PREFIX + ChatColor.GOLD + "Siege has ended!");
                } else {
                    ChatFormatter.sendCenteredMessage(p, PREFIX + ChatColor.RED + "Siege ended with no capture.");
                }
            }
        }
        active.clear();
        queue.clear();
        progress = 0;
        lastAnnounce = 0;
        capturingGuild = null;
        if (progressHologram != null) {
            progressHologram.despawn();
            progressHologram = null;
        }
        if (winner != null) {
            ownerGuild = winner;
            String msg = PREFIX + ChatColor.GOLD + "Guild " + winner + " has taken control of the town!";
            Bukkit.broadcastMessage(ChatFormatter.getCenteredText(msg));
        } else {
            String msg = PREFIX + ChatColor.RED + "No guild captured the town.";
            Bukkit.broadcastMessage(ChatFormatter.getCenteredText(msg));
        }
    }

    private void broadcast(String msg) {
        for (UUID id : active) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) ChatFormatter.sendCenteredMessage(p, PREFIX + msg);
        }
    }

    public boolean isActive(UUID id) { return active.contains(id); }
    public String getCapturingGuild() { return capturingGuild; }
    public int getProgress() { return progress; }
    public String getOwnerGuild() { return ownerGuild; }

    private void spawnParticles() {
        for (int i = 0; i < 40; i++) {
            double angle = (2 * Math.PI * i) / 40;
            double x = center.getX() + RADIUS * Math.cos(angle);
            double z = center.getZ() + RADIUS * Math.sin(angle);
            center.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, x, center.getY(), z, 1, 0,0,0,0);
        }
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

    private void updateHologram() {
        if (progressHologram == null) return;
        String guildLine = capturingGuild != null ? ChatColor.GOLD + capturingGuild : ChatColor.GRAY + "No Capture";
        int filled = progress / 5;
        int total = 20;
        StringBuilder bar = new StringBuilder();
        bar.append(ChatColor.GRAY).append("[");
        bar.append(ChatColor.GREEN).append("I".repeat(filled));
        if (filled < total) {
            bar.append(ChatColor.DARK_GRAY).append("I".repeat(total - filled));
        }
        bar.append(ChatColor.GRAY).append("] ").append(ChatColor.WHITE).append(progress).append("%");
        progressHologram.setLines(java.util.Arrays.asList(guildLine, bar.toString()));
    }
}
