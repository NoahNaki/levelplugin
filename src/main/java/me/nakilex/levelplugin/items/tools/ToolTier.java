package me.nakilex.levelplugin.items.tools;

import me.nakilex.levelplugin.items.data.ItemRarity;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;

public enum ToolTier {
    TIER_I(1, ItemRarity.COMMON, 1.0f, Arrays.asList(Material.WOODEN_PICKAXE)),
    TIER_II(10, ItemRarity.UNCOMMON, 1.2f, Arrays.asList(Material.STONE_PICKAXE)),
    TIER_III(15, ItemRarity.RARE, 1.5f, Arrays.asList(Material.GOLDEN_PICKAXE)),
    TIER_IV(25, ItemRarity.EPIC, 1.8f, Arrays.asList(Material.IRON_PICKAXE)),
    TIER_V(40, ItemRarity.LEGENDARY, 2.0f, Arrays.asList(Material.DIAMOND_PICKAXE)),
    TIER_VI(60, ItemRarity.MYTHIC, 2.2f, Arrays.asList(Material.NETHERITE_PICKAXE));

    private final int levelReq;
    private final ItemRarity rarity;
    private final float miningSpeed;
    private final List<Material> mats;

    ToolTier(int levelReq, ItemRarity rarity, float miningSpeed, List<Material> mats) {
        this.levelReq = levelReq;
        this.rarity = rarity;
        this.miningSpeed = miningSpeed;
        this.mats = mats;
    }

    public int getLevelRequirement() {
        return levelReq;
    }

    public ItemRarity getRarity() {
        return rarity;
    }

    public float getMiningSpeed() {
        return miningSpeed;
    }

    public String getTierName() {
        return name().replace("TIER_", "");
    }

    public static ToolTier fromMaterial(Material mat) {
        for (ToolTier tier : values()) {
            if (tier.mats.contains(mat)) return tier;
        }
        return null;
    }
}
