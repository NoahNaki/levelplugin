package me.nakilex.levelplugin.items.tools;

import me.nakilex.levelplugin.items.data.ItemRarity;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;

public enum ToolTier {
    TIER_I(1, ItemRarity.COMMON, 1.0f, 1.05f,
            Arrays.asList(Material.WOODEN_PICKAXE, Material.WOODEN_HOE)),
    TIER_II(10, ItemRarity.UNCOMMON, 1.2f, 1.1f,
            Arrays.asList(Material.STONE_PICKAXE, Material.STONE_HOE)),
    TIER_III(15, ItemRarity.RARE, 1.5f, 1.15f,
            Arrays.asList(Material.GOLDEN_PICKAXE, Material.GOLDEN_HOE)),
    TIER_IV(25, ItemRarity.EPIC, 1.8f, 1.2f,
            Arrays.asList(Material.IRON_PICKAXE, Material.IRON_HOE)),
    TIER_V(40, ItemRarity.LEGENDARY, 2.0f, 1.25f,
            Arrays.asList(Material.DIAMOND_PICKAXE, Material.DIAMOND_HOE)),
    TIER_VI(60, ItemRarity.MYTHIC, 2.2f, 1.3f,
            Arrays.asList(Material.NETHERITE_PICKAXE, Material.NETHERITE_HOE));

    private final int levelReq;
    private final ItemRarity rarity;
    private final float miningSpeed;
    private final float harvestYield;
    private final List<Material> mats;

    ToolTier(int levelReq, ItemRarity rarity, float miningSpeed, float harvestYield, List<Material> mats) {
        this.levelReq = levelReq;
        this.rarity = rarity;
        this.miningSpeed = miningSpeed;
        this.harvestYield = harvestYield;
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

    public float getHarvestYield() {
        return harvestYield;
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
