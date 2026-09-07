package me.nakilex.prisonenchants.mine;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

public record MineArea(World world, ProtectedRegion region) {
    public boolean contains(Location location) {
        return location != null && location.getWorld() == world
                && contains(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public boolean contains(Block block) {
        return block != null && block.getWorld() == world
                && contains(block.getX(), block.getY(), block.getZ());
    }

    public boolean contains(int x, int y, int z) {
        return region.contains(BlockVector3.at(x, y, z));
    }

    public int minY() {
        return Math.max(world.getMinHeight(), region.getMinimumPoint().y());
    }

    public int maxY() {
        return Math.min(world.getMaxHeight() - 1, region.getMaximumPoint().y());
    }
}
