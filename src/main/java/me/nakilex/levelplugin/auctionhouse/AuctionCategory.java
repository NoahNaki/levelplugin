package me.nakilex.levelplugin.auctionhouse;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Basic classification for auction items.
 */
public enum AuctionCategory {
    WEAPON,
    ARMOR,
    TOOL,
    MISC;

    /**
     * Determine a category for the provided item.
     */
    public static AuctionCategory fromItem(ItemStack item) {
        Material type = item.getType();
        String name = type.name();
        if (name.endsWith("_SWORD") || name.endsWith("_BOW") || name.endsWith("_CROSSBOW")) {
            return WEAPON;
        }
        if (name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS")) {
            return ARMOR;
        }
        if (name.endsWith("_AXE") || name.endsWith("_PICKAXE") || name.endsWith("_SHOVEL") || name.endsWith("_HOE")) {
            return TOOL;
        }
        return MISC;
    }
}
