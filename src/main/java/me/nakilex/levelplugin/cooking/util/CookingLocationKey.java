package me.nakilex.levelplugin.cooking.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;
import org.bukkit.block.Block;

/** Immutable block-coordinate key for in-memory cooking workstation lookup. */
public record CookingLocationKey(String worldId, int x, int y, int z) {
    public static CookingLocationKey of(Block block) {
        if (block == null) {
            throw new IllegalArgumentException("block cannot be null");
        }
        return of(block.getLocation());
    }

    public static CookingLocationKey of(Location location) {
        if (location == null || location.getWorld() == null) {
            throw new IllegalArgumentException("location and world cannot be null");
        }
        World world = location.getWorld();
        return new CookingLocationKey(
                world.getUID().toString(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());
    }

    public Location toLocation() {
        World world;
        try {
            world = Bukkit.getWorld(UUID.fromString(worldId));
        } catch (IllegalArgumentException ex) {
            world = Bukkit.getWorld(worldId);
        }
        return world == null ? null : new Location(world, x, y, z);
    }

    @Override
    public String toString() {
        return worldId + ":" + x + ":" + y + ":" + z;
    }
}
