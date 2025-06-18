package me.nakilex.levelplugin.fakeblock;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Simple manager for sending per-player fake blocks using the
 * {@link Player#sendBlockChange(Location, BlockData)} API.
 */
public class FakeBlockManager {

    private final Map<UUID, Map<Location, BlockData>> playerBlocks = new HashMap<>();

    /**
     * Shows a fake block to a specific player and remembers the change so it can
     * be reverted later.
     */
    public void showFakeBlock(Player player, Location loc, BlockData data) {
        player.sendBlockChange(loc, data);
        playerBlocks
            .computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
            .put(loc, data);
    }

    /**
     * Shows multiple fake blocks at once. Useful for large selections where
     * calling {@link #showFakeBlock(Player, Location, BlockData)} for every
     * block individually would create a large amount of scheduled tasks.
     * The changes are remembered so they can later be reverted via
     * {@link #clear(Player)}.
     */
    public void showFakeBlocks(Player player, Map<Location, BlockData> blocks) {
        if (blocks == null || blocks.isEmpty()) return;
        // Convert the map to BlockState snapshots for the API
        java.util.List<org.bukkit.block.BlockState> states = new java.util.ArrayList<>(blocks.size());
        for (var entry : blocks.entrySet()) {
            var state = entry.getKey().getBlock().getState();
            state.setBlockData(entry.getValue());
            states.add(state);
        }
        // send block changes in bulk if supported by the server API
        try {
            player.sendBlockChanges(states);
        } catch (NoSuchMethodError ignore) {
            // Fallback for older API versions - send changes one by one
            for (var state : states) {
                player.sendBlockChange(state.getLocation(), state.getBlockData());
            }
        }
        playerBlocks
            .computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
            .putAll(blocks);
    }

    /**
     * Reverts a previously shown fake block for the player.
     */
    public void hideFakeBlock(Player player, Location loc) {
        player.sendBlockChange(loc, loc.getBlock().getBlockData());
        Map<Location, BlockData> map = playerBlocks.get(player.getUniqueId());
        if (map != null) {
            map.remove(loc);
            if (map.isEmpty()) {
                playerBlocks.remove(player.getUniqueId());
            }
        }
    }

    /**
     * Reverts multiple previously shown fake blocks for the player.
     * @param player target player
     * @param locations collection of block locations to restore
     */
    public void hideFakeBlocks(Player player, java.util.Collection<Location> locations) {
        if (locations == null || locations.isEmpty()) return;
        Map<Location, BlockData> map = playerBlocks.get(player.getUniqueId());
        for (Location loc : locations) {
            player.sendBlockChange(loc, loc.getBlock().getBlockData());
            if (map != null) {
                map.remove(loc);
            }
        }
        if (map != null && map.isEmpty()) {
            playerBlocks.remove(player.getUniqueId());
        }
    }

    /**
     * Clears all fake blocks for the given player, restoring the real world
     * state.
     */
    public void clear(Player player) {
        Map<Location, BlockData> map = playerBlocks.remove(player.getUniqueId());
        if (map != null) {
            for (Location loc : map.keySet()) {
                player.sendBlockChange(loc, loc.getBlock().getBlockData());
            }
        }
    }
}
