package me.nakilex.levelplugin.events;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Identifies which armor slot is involved (helmet, chestplate, leggings, boots).
 */
public enum ArmorType {
    HELMET,
    CHESTPLATE,
    LEGGINGS,
    BOOTS;

    /**
     * Determines which ArmorType an ItemStack matches, or null if it's not armor.
     *
     * @param item The ItemStack to check
     * @return The matching ArmorType, or null if not recognized as armor
     */
    public static ArmorType matchType(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }

        String typeName = item.getType().name();
        // Helmet checks
        if (typeName.endsWith("_HELMET") || typeName.endsWith("_HEAD") || typeName.endsWith("_SKULL")) {
            return HELMET;
        }
        // Chestplate checks
        if (typeName.endsWith("_CHESTPLATE") || typeName.equalsIgnoreCase("ELYTRA")) {
            return CHESTPLATE;
        }
        // Leggings check
        if (typeName.endsWith("_LEGGINGS")) {
            return LEGGINGS;
        }
        // Boots check
        if (typeName.endsWith("_BOOTS")) {
            return BOOTS;
        }

        // If it doesn't match any known armor suffix, return null
        return null;
    }
}
