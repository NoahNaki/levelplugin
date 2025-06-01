package me.nakilex.levelplugin.lootchests.data;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ChestData {

    private static final String WORLD_NAME = "MmoRPG";

    private final int chestId;
    private final double x;
    private final double y;
    private final double z;
    private final int tier;
    private String customName; // Optional
    private String contentType; // Optional, like "Weapon", "Armor", etc.
    private final List<ArmorStand> holograms = new ArrayList<>();
    private ItemStack bufferedLootItem;


    public ChestData(int chestId, double x, double y, double z, int tier) {
        this.chestId = chestId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.tier = tier;
        this.bufferedLootItem = null;
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

    public Optional<String> getCustomName() {
        return Optional.ofNullable(customName);
    }

    public Optional<String> getContentType() {
        return Optional.ofNullable(contentType);
    }

    public List<org.bukkit.entity.ArmorStand> getHolograms() {
        return holograms;
    }


    public Location toLocation() {
        World world = Bukkit.getWorld(WORLD_NAME);
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
