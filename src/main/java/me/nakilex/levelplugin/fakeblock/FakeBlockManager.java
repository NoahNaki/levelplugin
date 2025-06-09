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
