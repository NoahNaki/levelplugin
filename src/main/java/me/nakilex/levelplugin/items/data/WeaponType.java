package me.nakilex.levelplugin.items.data;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum WeaponType {
    SWORD(Arrays.asList(
        Material.WOODEN_SWORD, Material.STONE_SWORD,
        Material.IRON_SWORD, Material.GOLDEN_SWORD,
        Material.DIAMOND_SWORD, Material.NETHERITE_SWORD
    )),
    AXE(Arrays.asList(
        Material.WOODEN_AXE, Material.STONE_AXE,
        Material.IRON_AXE, Material.GOLDEN_AXE,
        Material.DIAMOND_AXE, Material.NETHERITE_AXE
    )),
    BOW(Arrays.asList(
        Material.BOW, Material.CROSSBOW
    )),
    WAND(Arrays.asList(
        Material.STICK, Material.BLAZE_ROD
    )),
    SHOVEL(Arrays.asList(
        Material.WOODEN_SHOVEL, Material.STONE_SHOVEL,
        Material.IRON_SHOVEL, Material.GOLDEN_SHOVEL,
        Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL
    ));

    private final List<Material> materials;

    WeaponType(List<Material> materials) {
        this.materials = materials;
    }

    /**
     * Determine the WeaponType of an ItemStack, or null if none.
     */
    public static WeaponType matchType(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;
        Material mat = item.getType();
        for (WeaponType wt : values()) {
            if (wt.materials.contains(mat)) return wt;
        }
        return null;
    }

    /**
     * Get the list of Materials associated with this WeaponType.
     */
    public List<Material> getMaterials() {
        return Collections.unmodifiableList(materials);
    }

    /**
     * Class-specific weapon checks
     */
    public static boolean isValidRogueWeapon(ItemStack item) {
        return matchType(item) == SWORD;
    }

    public static boolean isValidWarriorWeapon(ItemStack item) {
        return matchType(item) == SHOVEL;
    }

    public static boolean isValidMageWeapon(ItemStack item) {
        return matchType(item) == WAND;
    }

    public static boolean isValidArcherWeapon(ItemStack item) {
        return matchType(item) == BOW;
    }
}
