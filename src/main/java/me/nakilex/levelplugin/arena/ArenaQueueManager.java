package me.nakilex.levelplugin.arena;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
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

    /**
     * Add the player to the queue if they are not already present.
     *
     * @param player player entering the queue
     * @return {@code true} if the player was newly added
     */
    public boolean join(Player player) {
        return join(player.getUniqueId());
    }

    /**
     * Add a player by unique id. Useful for non-player contexts.
     */
    public boolean join(UUID playerId) {
        return queue.add(playerId);
    }

    /**
     * Remove the player from the queue.
     *
     * @param playerId id to remove
     * @return {@code true} if the player was queued
     */
    public boolean leave(UUID playerId) {
        return queue.remove(playerId);
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
    }
}
