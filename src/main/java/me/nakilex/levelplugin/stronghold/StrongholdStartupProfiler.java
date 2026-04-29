package me.nakilex.levelplugin.stronghold;

import me.nakilex.levelplugin.Main;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight console profiler for tracking stronghold startup latency.
 */
public final class StrongholdStartupProfiler {
    private static final String PREFIX = "[StrongholdStartupTiming] ";
    private static final Map<UUID, StrongholdStartupProfiler> ACTIVE_SESSIONS = new ConcurrentHashMap<>();

    private final Main plugin;
    private final UUID playerId;
    private final String playerName;
    private final long startedAtNanos;
    private final Map<String, Long> durationsMs = new LinkedHashMap<>();

    private StrongholdStartupProfiler(Main plugin, Player player) {
        this.plugin = plugin;
        this.playerId = player == null ? null : player.getUniqueId();
        this.playerName = player == null ? "unknown" : player.getName();
        this.startedAtNanos = System.nanoTime();
        log("Session started.");
    }

    public static StrongholdStartupProfiler start(Main plugin, Player player) {
        if (plugin == null || player == null) {
            return null;
        }
        StrongholdStartupProfiler profiler = new StrongholdStartupProfiler(plugin, player);
        ACTIVE_SESSIONS.put(player.getUniqueId(), profiler);
        return profiler;
    }

    public static StrongholdStartupProfiler startOrContinue(Main plugin, Player player) {
        if (plugin == null || player == null) {
            return null;
        }
        return ACTIVE_SESSIONS.computeIfAbsent(player.getUniqueId(), ignored -> new StrongholdStartupProfiler(plugin, player));
    }

    public long stepStarted(String stepName) {
        log("Step started: " + stepName);
        return System.nanoTime();
    }

    public void stepFinished(String stepName, long startedNanos) {
        long elapsedMs = Math.max(0L, (System.nanoTime() - startedNanos) / 1_000_000L);
        durationsMs.put(stepName, elapsedMs);
        log("Step finished: " + stepName + " (" + elapsedMs + "ms)");
    }

    public void summary() {
        long totalMs = Math.max(0L, (System.nanoTime() - startedAtNanos) / 1_000_000L);
        Map.Entry<String, Long> slowest = durationsMs.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);
        if (slowest == null) {
            log("Session finished with no tracked steps. total=" + totalMs + "ms");
            return;
        }
        log("Session summary -> slowest=" + slowest.getKey() + " (" + slowest.getValue() + "ms), total=" + totalMs + "ms.");
        ACTIVE_SESSIONS.remove(playerId);
    }

    private void log(String message) {
        plugin.getLogger().info(PREFIX + "[" + playerName + "/" + (playerId == null ? "?" : playerId) + "] " + message);
    }
}
