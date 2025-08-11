package me.nakilex.levelplugin.lootchests.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class LocationUtils {

    /**
     * Converts a string in the format "worldName:x:y:z"
     * to a Bukkit Location object.
     *
     * Example input: "world:100:64:-200"
     *
     * @param locationString The string representing the location
     * @return A Location object, or null if invalid
     */
    public static Location stringToLocation(String locationString) {
        if (locationString == null || locationString.trim().isEmpty()) {
            return null;
        }

        String[] parts = locationString.split(":");
        if (parts.length < 4) {
            return null; // Not enough data
        }

        String worldName = parts[0];
        double x, y, z;
        try {
            x = Double.parseDouble(parts[1]);
            y = Double.parseDouble(parts[2]);
            z = Double.parseDouble(parts[3]);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return null;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null; // The specified world doesn't exist or isn't loaded
        }

        return new Location(world, x, y, z);
    }

    /**
     * Converts a Bukkit Location to a string in the format "worldName:x:y:z"
     * for easy storage in config files, etc.
     *
     * @param location The Location object
     * @return A string with format "worldName:x:y:z", or empty string if location/world is null
     */
    public static String locationToString(Location location) {
        if (location == null || location.getWorld() == null) {
            return "";
        }
        String worldName = location.getWorld().getName();
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        return worldName + ":" + x + ":" + y + ":" + z;
    }

    /**
     * Convenience method to parse "x, y, z" coordinate strings without world data.
     *
     * @param coords A string in the format "x, y, z"
     * @param world  The Bukkit World, if you know it
     * @return A new Location with the given coordinates in the specified world, or null if invalid
     */
    public static Location coordsStringToLocation(String coords, World world) {
        if (coords == null || coords.trim().isEmpty() || world == null) {
            return null;
        }

        String[] split = coords.split(",");
        if (split.length < 3) {
            return null;
        }

        try {
            double x = Double.parseDouble(split[0].trim());
            double y = Double.parseDouble(split[1].trim());
            double z = Double.parseDouble(split[2].trim());
            return new Location(world, x, y, z);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Returns a new Location centered within the block that contains the
     * provided location. Useful when spawning entities that should sit in
     * the middle of a block rather than at its corner.
     *
     * @param location any world position
     * @return centered location (block coordinates + 0.5 on X/Z) or null if the input is null
     */
    public static Location centerOnBlock(Location location) {
        if (location == null) {
            return null;
        }
        return location.getBlock().getLocation().add(0.5, 0, 0.5);
    }

    /**
     * Adjust a location so it sits just above the highest solid block
     * at the given X/Z coordinates. Useful for spawning entities so they
     * don't appear underground.
     *
     * @param location base location
     * @return new location one block above the surface
     */
    public static Location aboveSurface(Location location) {
        if (location == null || location.getWorld() == null) {
            return location;
        }
        int highest = location.getWorld().getHighestBlockYAt(location);
        if (location.getY() <= highest) {
            return new Location(
                    location.getWorld(),
                    location.getX(),
                    highest + 1,
                    location.getZ(),
                    location.getYaw(),
                    location.getPitch());
        }
        return location;
    }

    /**
     * Counts consecutive air blocks directly above the given location.
     * Starts from the block one unit higher and stops at the first
     * non-air block or the world height limit.
     *
     * @param location base location
     * @return number of continuous air blocks above
     */
    public static int countAirAbove(Location location) {
        if (location == null || location.getWorld() == null) {
            return 0;
        }
        World world = location.getWorld();
        int x = location.getBlockX();
        int z = location.getBlockZ();
        int maxY = world.getMaxHeight();
        int count = 0;
        for (int y = location.getBlockY() + 1; y < maxY; y++) {
            if (world.getBlockAt(x, y, z).getType().isAir()) {
                count++;
            } else {
                break;
            }
        }
        return count;
    }

    /**
     * Returns a location positioned just above the first solid block found
     * when searching downward from the provided coordinates. The resulting
     * location is centered on the block for consistency with entity spawning.
     *
     * @param location starting point for the downward search
     * @return location one block above the discovered surface
     */
    public static Location surfaceBelow(Location location) {
        if (location == null || location.getWorld() == null) {
            return location;
        }
        World world = location.getWorld();
        int x = location.getBlockX();
        int z = location.getBlockZ();
        int y = location.getBlockY();
        int minY = world.getMinHeight();
        while (y > minY && world.getBlockAt(x, y - 1, z).getType().isAir()) {
            y--;
        }
        return new Location(world, x + 0.5, y, z + 0.5, location.getYaw(), location.getPitch());
    }
}
