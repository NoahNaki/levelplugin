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
}
