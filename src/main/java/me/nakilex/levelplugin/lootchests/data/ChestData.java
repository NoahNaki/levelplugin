package me.nakilex.levelplugin.lootchests.data;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class ChestData {

    private static final String WORLD_NAME = "MmoRPG";

    private final int chestId;
    private final double x;
    private final double y;
    private final double z;
    private final int tier;

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

    public int getTier() {
        return tier;
    }

    // These are the getters you need:
    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public Location toLocation() {
        World world = Bukkit.getWorld(WORLD_NAME);
        if (world == null) return null;
        return new Location(world, x, y, z);
    }
}
