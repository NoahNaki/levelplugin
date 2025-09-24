package me.nakilex.levelplugin.cutscene;

import me.nakilex.levelplugin.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/** Shared parsing helpers for the cutscene YAML schema. */
public final class CutsceneIO {
    private CutsceneIO() {}

    public static Location parseLocation(Main plugin, String worldName, String coords) {
        if (coords == null) {
            return null;
        }
        String[] parts = coords.split(" ");
        if (parts.length < 3) {
            return null;
        }
        try {
            double x = Double.parseDouble(parts[0]);
            double y = Double.parseDouble(parts[1]);
            double z = Double.parseDouble(parts[2]);
            float yaw = parts.length >= 4 ? Float.parseFloat(parts[3]) : 0f;
            float pitch = parts.length >= 5 ? Float.parseFloat(parts[4]) : 0f;
            World world = resolveWorld(plugin, worldName);
            if (world == null) {
                return null;
            }
            return new Location(world, x, y, z, yaw, pitch);
        } catch (NumberFormatException ex) {
            plugin.getLogger().warning("Failed to parse cutscene location '" + coords + "': " + ex.getMessage());
            return null;
        }
    }

    public static Location parseVector(Main plugin, String worldName, String coords) {
        if (coords == null) {
            return null;
        }
        String[] parts = coords.split(" ");
        if (parts.length < 3) {
            return null;
        }
        try {
            double x = Double.parseDouble(parts[0]);
            double y = Double.parseDouble(parts[1]);
            double z = Double.parseDouble(parts[2]);
            World world = resolveWorld(plugin, worldName);
            if (world == null) {
                return null;
            }
            return new Location(world, x, y, z);
        } catch (NumberFormatException ex) {
            plugin.getLogger().warning("Failed to parse cutscene vector '" + coords + "': " + ex.getMessage());
            return null;
        }
    }

    public static String formatLocation(Location location) {
        if (location == null) {
            return null;
        }
        return location.getX() + " " + location.getY() + " " + location.getZ() + " "
                + location.getYaw() + " " + location.getPitch();
    }

    public static String formatVector(Location location) {
        if (location == null) {
            return null;
        }
        return location.getX() + " " + location.getY() + " " + location.getZ();
    }

    private static World resolveWorld(Main plugin, String worldName) {
        World world = null;
        if (worldName != null) {
            world = Bukkit.getWorld(worldName);
            if (world == null && "world2".equalsIgnoreCase(worldName)) {
                world = Bukkit.getWorld("world");
            }
        }
        if (world == null) {
            world = plugin.getServer().getWorlds().isEmpty() ? null : plugin.getServer().getWorlds().get(0);
        }
        return world;
    }
}
