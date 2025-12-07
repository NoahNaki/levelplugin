package me.nakilex.levelplugin.lootchests.data;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class ChestData {

    private static final String DEFAULT_WORLD = "MmoRPG";

    private final int chestId;
    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private final BlockFace facing;
    private String customName; // Optional
    private String contentType; // Optional, like "Weapon", "Armor", etc.
    private ItemStack bufferedLootItem;


    public ChestData(int chestId, double x, double y, double z, BlockFace facing) {
        this(chestId, DEFAULT_WORLD, x, y, z, facing);
    }

    public ChestData(int chestId, String worldName, double x, double y, double z, BlockFace facing) {
        this.chestId = chestId;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.facing = facing == null ? BlockFace.NORTH : facing;
        this.bufferedLootItem = null;
    }

    public int getChestId() {
        return chestId;
    }

    public String getWorldName() {
        return worldName;
    }

    public BlockFace getFacing() {
        return facing;
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

    public Optional<String> getCustomName() {
        return Optional.ofNullable(customName);
    }

    public Optional<String> getContentType() {
        return Optional.ofNullable(contentType);
    }

    public Location toLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world, x, y, z);
    }

    public void setBufferedLootItem(ItemStack item) {
        this.bufferedLootItem = item;
    }

    // ADD THIS getter if you need it when building the GUI:
    public ItemStack getBufferedLootItem() {
        return bufferedLootItem;
    }
}
