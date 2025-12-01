package me.nakilex.levelplugin.items.data;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import me.nakilex.levelplugin.items.utils.ItemUtil;

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
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            if (pdc.has(ItemUtil.TEMPLATE_MATERIAL_KEY, PersistentDataType.STRING)) {
                String stored = pdc.get(ItemUtil.TEMPLATE_MATERIAL_KEY, PersistentDataType.STRING);
                if (stored != null) {
                    typeName = stored;
                }
            }
        }

        return resolveByName(typeName);
    }

    /**
     * Resolve armor type directly from a Bukkit Material.
     */
    public static ArmorType fromMaterial(Material material) {
        if (material == null || material == Material.AIR) {
            return null;
        }
        return resolveByName(material.name());
    }

    private static ArmorType resolveByName(String typeName) {
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
