package me.nakilex.levelplugin.woodcutting.protection;

import me.nakilex.levelplugin.woodcutting.tree.TreeTypeRegistry;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PlacedBlockTracker implements Listener {
    private final TreeTypeRegistry treeTypeRegistry;
    private final Set<LocationKey> placedBlocks = ConcurrentHashMap.newKeySet();

    public PlacedBlockTracker(TreeTypeRegistry treeTypeRegistry) { this.treeTypeRegistry = treeTypeRegistry; }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (treeTypeRegistry.isWoodLike(event.getBlockPlaced().getType())) placedBlocks.add(LocationKey.of(event.getBlockPlaced()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) { placedBlocks.remove(LocationKey.of(event.getBlock())); }

    public boolean isPlaced(Block block) { return placedBlocks.contains(LocationKey.of(block)); }

    public boolean tooManyPlaced(Collection<Block> blocks) {
        if (blocks.isEmpty()) return false;
        long placed = blocks.stream().filter(this::isPlaced).count();
        return placed / (double) blocks.size() >= 0.50D;
    }

    private record LocationKey(String world, int x, int y, int z) {
        static LocationKey of(Block block) {
            World world = block.getWorld();
            return new LocationKey(world.getUID().toString(), block.getX(), block.getY(), block.getZ());
        }
    }
}
