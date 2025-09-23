package me.nakilex.levelplugin.arena;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.scoreboard.PlayerScoreboardManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks players waiting for the arena queue. The queue preserves the
 * order players joined so matchmakers can pull from the head to start
 * fights. This manager purposefully keeps the API minimal so it can be
 * reused by commands, GUIs or future matchmaking logic.
 */
public class ArenaQueueManager {
    private final Set<UUID> queue = new LinkedHashSet<>();
    private final Map<UUID, Long> joinTimes = new LinkedHashMap<>();
    private PlayerScoreboardManager scoreboardManager;

    /**
     * Add the player to the queue if they are not already present.
     *
     * @param player player entering the queue
     * @return {@code true} if the player was newly added
     */
    public boolean join(Player player) {
        boolean added = queue.add(player.getUniqueId());
        if (added) {
            joinTimes.put(player.getUniqueId(), System.currentTimeMillis());
            updateScoreboard(player);
        }
        return added;
    }

    /**
     * Add a player by unique id. Useful for non-player contexts.
     */
    public boolean join(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            return join(player);
        }
        boolean added = queue.add(playerId);
        if (added) {
            joinTimes.put(playerId, System.currentTimeMillis());
        }
        return added;
    }

    /**
     * Remove the player from the queue.
     *
     * @param playerId id to remove
     * @return {@code true} if the player was queued
     */
    public boolean leave(UUID playerId) {
        boolean removed = queue.remove(playerId);
        if (removed) {
            joinTimes.remove(playerId);
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                updateScoreboard(player);
            }
        }
        return removed;
    }

    /**
     * Convenience overload accepting a {@link Player} instance.
     */
    public boolean leave(Player player) {
        return leave(player.getUniqueId());
    }

    /**
     * Check whether the player is already queued.
     */
    public boolean isQueued(UUID playerId) {
        return queue.contains(playerId);
    }

    /**
     * @return number of players currently queued.
     */
    public int getQueueSize() {
        return queue.size();
    }

    /**
     * Snapshot of the queue order. The returned list is immutable to prevent
     * callers from accidentally mutating the underlying set.
     */
    public List<UUID> getQueueSnapshot() {
        return Collections.unmodifiableList(new ArrayList<>(queue));
    }

    /**
     * Remove all queued players. Typically invoked on plugin shutdown.
     */
    public void clear() {
        queue.clear();
        joinTimes.clear();
    }

    /**
     * Record the scoreboard manager so queue changes can trigger immediate updates.
     */
    public void setScoreboardManager(PlayerScoreboardManager scoreboardManager) {
        this.scoreboardManager = scoreboardManager;
    }

    /**
     * Return the elapsed time since the player joined the queue. {@link Duration#ZERO}
     * is returned when the player is not currently queued.
     */
    public Duration getWaitDuration(UUID playerId) {
        Long joined = joinTimes.get(playerId);
        if (joined == null) {
            return Duration.ZERO;
        }
        long elapsed = Math.max(0L, System.currentTimeMillis() - joined);
        return Duration.ofMillis(elapsed);
    }

    private void updateScoreboard(Player player) {
        if (scoreboardManager != null) {
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> scoreboardManager.updateBoard(player));
        }
    }
}
