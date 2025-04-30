package me.nakilex.levelplugin.items.data;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public enum WeaponType {
    SWORD,
    AXE,
    BOW,
    WAND,
    SHOVEL;

    public static WeaponType matchType(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;

        Material mat = item.getType();

        switch (mat) {
            // Swords
            case WOODEN_SWORD:
            case STONE_SWORD:
            case IRON_SWORD:
            case GOLDEN_SWORD:
            case DIAMOND_SWORD:
            case NETHERITE_SWORD:
                return SWORD;

            // Axes
            case WOODEN_AXE:
            case STONE_AXE:
            case IRON_AXE:
            case GOLDEN_AXE:
            case DIAMOND_AXE:
            case NETHERITE_AXE:
                return AXE;

            // Bows
            case BOW:
            case CROSSBOW:
                return BOW;

            // Wands
            case STICK:
            case BLAZE_ROD:
                return WAND;

            // Shovels (for Warrior class)
            case WOODEN_SHOVEL:
            case STONE_SHOVEL:
            case IRON_SHOVEL:
            case GOLDEN_SHOVEL:
            case DIAMOND_SHOVEL:
            case NETHERITE_SHOVEL:
                return SHOVEL;

            default:
                return null;
        }
    }

}
