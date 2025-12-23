package me.nakilex.levelplugin.mercenary.board;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;

/**
 * Simple value object storing an expedition board's persisted coordinates and facing.
 */
public record ExpeditionBoardLocation(int id, String worldName, double x, double y, double z, BlockFace facing) {

    public Location toLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        return new Location(world, x, y, z);
    }

    public double distanceSquared(Location other) {
        Location base = toLocation();
        if (base == null || other == null || other.getWorld() == null) {
            return Double.MAX_VALUE;
        }
        if (!other.getWorld().getName().equalsIgnoreCase(base.getWorld().getName())) {
            return Double.MAX_VALUE;
        }
        return base.distanceSquared(other);
    }
}
