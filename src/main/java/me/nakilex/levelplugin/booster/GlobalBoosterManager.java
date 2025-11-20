package me.nakilex.levelplugin.booster;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

/**
 * Tracks global server-wide boosters such as coin and combat XP multipliers.
 */
public class GlobalBoosterManager {

    private final Main plugin;
    private final Map<BoosterType, BoosterState> boosters = new EnumMap<>(BoosterType.class);
    private final double defaultMultiplier;

    public GlobalBoosterManager(Main plugin, double defaultMultiplier) {
        this.plugin = plugin;
        this.defaultMultiplier = defaultMultiplier;

        new BukkitRunnable() {
            @Override
            public void run() {
                boosters.entrySet().removeIf(entry -> entry.getValue().expired());
            }
        }.runTaskTimer(plugin, 20L, 20L * 30); // clean up every 30 seconds
    }

    public double getMultiplier(BoosterType type) {
        BoosterState state = boosters.get(type);
        if (state == null || state.expired()) {
            boosters.remove(type);
            return 1.0;
        }
        return state.multiplier();
    }

    public Duration getRemaining(BoosterType type) {
        BoosterState state = boosters.get(type);
        if (state == null || state.expired()) return Duration.ZERO;
        return Duration.between(Instant.now(), state.expiresAt());
    }

    public boolean activateBooster(BoosterType type, Duration duration, Player activator) {
        Duration remaining = getRemaining(type);
        Duration appliedDuration = duration;
        if (!remaining.isZero() && !remaining.isNegative()) {
            appliedDuration = remaining.plus(duration);
        }

        boosters.put(type, new BoosterState(Instant.now().plus(appliedDuration), defaultMultiplier));

        broadcastActivation(type, activator, appliedDuration);
        return true;
    }

    private void broadcastActivation(BoosterType type, Player activator, Duration totalDuration) {
        String name = activator != null ? activator.getName() : "Server";
        String durationText = formatDuration(totalDuration);
        String message = type == BoosterType.COIN
                ? "" + type.accent() + "Coin booster activated by " + name + "! " + defaultMultiplier + "x for " + durationText + "."
                : "" + type.accent() + "Combat XP booster activated by " + name + "! " + defaultMultiplier + "x for " + durationText + ".";
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(ChatMessageUtil.format(MessageType.INFO, message));
        }
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

    private record BoosterState(Instant expiresAt, double multiplier) {
        boolean expired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
