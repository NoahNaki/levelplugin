package me.nakilex.levelplugin.lootchests.data;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class ChestData {

    // Single, fixed world name for all chests
    private static final String WORLD_NAME = "rpgworld";

    private final int chestId;
    private final double x;
    private final double y;
    private final double z;
    private final int tier;

    /**
     * Constructor for a loot chest in a single world ("rpgworld").
     *
     * @param chestId Unique ID of the chest as stored in config
     * @param x       X coordinate
     * @param y       Y coordinate
     * @param z       Z coordinate
     * @param tier    The chest's tier (1–4)
     */
    public ChestData(int chestId, double x, double y, double z, int tier) {
        this.chestId = chestId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.tier = tier;
    }

    public int getChestId() {
        return chestId;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public int getTier() {
        return tier;
    }

    /**
     * Returns a Bukkit Location object in the world "rpgworld".
     */
    public Location toLocation() {
        World world = Bukkit.getWorld(WORLD_NAME);
        if (world == null) {
            // If the world is not loaded, handle it gracefully (e.g., return null)
            return null;
        }
        return new Location(world, x, y, z);
    }
}
