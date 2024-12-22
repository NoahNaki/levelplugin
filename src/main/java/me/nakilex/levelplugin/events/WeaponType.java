package me.nakilex.levelplugin.events;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Identifies which type of weapon (e.g., sword, axe, bow)
 * an ItemStack corresponds to in your plugin.
 */
public enum WeaponType {
    SWORD,
    AXE,
    BOW,
    CROSSBOW,
    TRIDENT;

    /**
     * Matches the given ItemStack to a WeaponType,
     * or returns null if not recognized as a weapon.
     *
     * @param item The ItemStack to check
     * @return The matching WeaponType, or null if it's not recognized as a weapon
     */
    public static WeaponType matchType(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }

        String matName = item.getType().name();

        // Swords end with "_SWORD" (WOODEN_SWORD, STONE_SWORD, etc.)
        if (matName.endsWith("_SWORD")) {
            return SWORD;
        }
        // Axes end with "_AXE" (WOODEN_AXE, DIAMOND_AXE, etc.)
        else if (matName.endsWith("_AXE")) {
            return AXE;
        }
        // BOW / CROSSBOW / TRIDENT checks
        else if (matName.equals("BOW")) {
            return BOW;
        } else if (matName.equals("CROSSBOW")) {
            return CROSSBOW;
        } else if (matName.equals("TRIDENT")) {
            return TRIDENT;
        }

        // If it doesn't match any known weapon type, return null
        return null;
    }
}
