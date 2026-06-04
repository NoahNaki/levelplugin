package me.nakilex.levelplugin.items.tools;

import me.nakilex.levelplugin.items.data.ItemRarity;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;

public enum ToolTier {
    TIER_I(1, ItemRarity.COMMON, 1.0f, 1.05f, 1.05f, 1.02f,
            Arrays.asList(Material.WOODEN_PICKAXE, Material.WOODEN_HOE, Material.WOODEN_AXE)),
    TIER_II(10, ItemRarity.UNCOMMON, 1.2f, 1.1f, 1.1f, 1.05f,
            Arrays.asList(Material.STONE_PICKAXE, Material.STONE_HOE, Material.STONE_AXE)),
    TIER_III(15, ItemRarity.RARE, 1.5f, 1.15f, 1.15f, 1.08f,
            Arrays.asList(Material.GOLDEN_PICKAXE, Material.GOLDEN_HOE, Material.GOLDEN_AXE)),
    TIER_IV(25, ItemRarity.EPIC, 1.8f, 1.2f, 1.2f, 1.12f,
            Arrays.asList(Material.IRON_PICKAXE, Material.IRON_HOE, Material.IRON_AXE)),
    TIER_V(40, ItemRarity.LEGENDARY, 2.0f, 1.25f, 1.25f, 1.16f,
            Arrays.asList(Material.DIAMOND_PICKAXE, Material.DIAMOND_HOE, Material.DIAMOND_AXE)),
    TIER_VI(60, ItemRarity.MYTHIC, 2.2f, 1.3f, 1.35f, 1.2f,
            Arrays.asList(Material.NETHERITE_PICKAXE, Material.NETHERITE_HOE, Material.FISHING_ROD, Material.NETHERITE_AXE));

    private final int levelReq;
    private final ItemRarity rarity;
    private final float miningSpeed;
    private final float harvestYield;
    private final float fishingSpeed;
    private final float fishRarityBonus;
    private final List<Material> mats;

    ToolTier(int levelReq, ItemRarity rarity, float miningSpeed, float harvestYield,
             float fishingSpeed, float fishRarityBonus, List<Material> mats) {
        this.levelReq = levelReq;
        this.rarity = rarity;
        this.miningSpeed = miningSpeed;
        this.harvestYield = harvestYield;
        this.fishingSpeed = fishingSpeed;
        this.fishRarityBonus = fishRarityBonus;
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

    /** Converts the shared tier speed curve into ore-entity mining damage. */
    public int getMiningDamage() {
        return Math.max(1, Math.round(2.0f + ((miningSpeed - 1.0f) * 4.0f)));
    }

    public float getHarvestYield() {
        return harvestYield;
    }

    public float getFishingSpeed() {
        return fishingSpeed;
    }

    public float getFishRarityBonus() {
        return fishRarityBonus;
    }

    public String getTierName() {
        return name().replace("TIER_", "");
    }

    public boolean isHighestTier() {
        return this == highest();
    }

    public static ToolTier highest() {
        ToolTier[] tiers = values();
        return tiers[tiers.length - 1];
    }

    public static ToolTier fromMaterial(Material mat) {
        for (ToolTier tier : values()) {
            if (tier.mats.contains(mat)) return tier;
        }
        return null;
    }
}
