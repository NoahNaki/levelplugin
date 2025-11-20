package me.nakilex.levelplugin.booster;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.settings.managers.SettingsManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks global server-wide boosters such as coin and combat XP multipliers.
 */
public class GlobalBoosterManager {

    private final Main plugin;
    private final double defaultMultiplier;
    private final Map<UUID, BossBar> boosterBars = new HashMap<>();
    private BoosterState activeBooster;

    public GlobalBoosterManager(Main plugin, double defaultMultiplier) {
        this.plugin = plugin;
        this.defaultMultiplier = defaultMultiplier;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (activeBooster != null && activeBooster.expired()) {
                    activeBooster = null;
                }
                updateBossBars(Bukkit.getOnlinePlayers());
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public double getMultiplier(BoosterType type) {
        if (activeBooster == null || activeBooster.type() != type || activeBooster.expired()) {
            return 1.0;
        }
        return activeBooster.multiplier();
    }

    public Duration getRemaining(BoosterType type) {
        if (activeBooster == null || activeBooster.type() != type || activeBooster.expired()) {
            return Duration.ZERO;
        }
        return activeBooster.remaining();
    }

    public boolean activateBooster(BoosterType type, Duration duration, Player activator) {
        if (activeBooster != null && !activeBooster.expired()) {
            if (activator != null) {
                activator.sendMessage(ChatMessageUtil.format(MessageType.WARNING,
                        "Another booster is already active for " + formatDuration(activeBooster.remaining()) + "."));
            }
            return false;
        }

        activeBooster = new BoosterState(type, Instant.now(), duration, defaultMultiplier);

        broadcastActivation(type, activator, duration);
        updateBossBars(Bukkit.getOnlinePlayers());
        return true;
    }

    private void broadcastActivation(BoosterType type, Player activator, Duration totalDuration) {
        String name = activator != null ? activator.getName() : "Server";
        String durationText = formatDuration(totalDuration);
        String descriptor = type == BoosterType.COIN ? "Coin" : "Combat XP";
        String message = ChatColor.GRAY + "[" + type.accent() + descriptor + ChatColor.GRAY + "] "
                + ChatColor.WHITE + name + ChatColor.GRAY + " activated a "
                + type.accent() + defaultMultiplier + "x " + descriptor + " booster for " + ChatColor.WHITE + durationText + ChatColor.GRAY + ".";
        ChatMessageUtil.broadcast(MessageType.INFO, message);
    }

    private String formatDuration(Duration duration) {
        long minutes = duration.toMinutes();
        long hours = minutes / 60;
        long mins = minutes % 60;
        if (hours > 0 && mins > 0) {
            return hours + "h " + mins + "m";
        }
        if (hours > 0) {
            return hours + "h";
        }
        return mins + "m";
    }

    public void refreshBossBar(Player player) {
        updateBossBars(java.util.Collections.singletonList(player));
    }

    private void updateBossBars(Collection<? extends Player> players) {
        boolean hasActive = activeBooster != null && !activeBooster.expired();
        BoosterType activeType = hasActive ? activeBooster.type() : null;
        Duration remaining = hasActive ? activeBooster.remaining() : Duration.ZERO;
        double progress = hasActive ? activeBooster.progress() : 0.0;

        for (Player player : players) {
            BossBar bar = boosterBars.computeIfAbsent(player.getUniqueId(), id -> {
                BossBar created = Bukkit.createBossBar("", BarColor.YELLOW, BarStyle.SOLID);
                created.addPlayer(player);
                created.setVisible(false);
                return created;
            });

            if (!hasActive || !wantsBossBar(player)) {
                bar.removePlayer(player);
                bar.setVisible(false);
                continue;
            }

            String title = buildTitle(activeType, remaining);
            bar.setTitle(title);
            bar.setColor(activeType == BoosterType.COIN ? BarColor.YELLOW : BarColor.GREEN);
            bar.setProgress(progress);
            if (!bar.getPlayers().contains(player)) {
                bar.addPlayer(player);
            }
            bar.setVisible(true);
        }
    }

    private String buildTitle(BoosterType type, Duration remaining) {
        String timeText = formatTime(remaining);
        return (type == BoosterType.COIN ? ChatColor.YELLOW + "Coin Booster " : ChatColor.GREEN + "XP Booster ")
                + ChatColor.WHITE + timeText;
    }

    private boolean wantsBossBar(Player player) {
        SettingsManager settingsManager = plugin.getSettingsManager();
        if (settingsManager == null) return true;
        return settingsManager.getSettings(player).isBoosterBossBarEnabled();
    }

    private String formatTime(Duration duration) {
        long seconds = Math.max(0, duration.getSeconds());
        long minutes = seconds / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    private record BoosterState(BoosterType type, Instant startedAt, Duration duration, double multiplier) {
        boolean expired() {
            return Instant.now().isAfter(startedAt.plus(duration));
        }

        Duration remaining() {
            Instant now = Instant.now();
            Instant end = startedAt.plus(duration);
            if (now.isAfter(end)) return Duration.ZERO;
            return Duration.between(now, end);
        }

        double progress() {
            if (duration.isZero() || duration.isNegative()) return 0.0;
            double remainingSeconds = remaining().toMillis();
            double totalSeconds = duration.toMillis();
            return Math.min(1.0, Math.max(0.0, remainingSeconds / totalSeconds));
        }

        public double multiplier() {
            return multiplier;
        }
    }
}
