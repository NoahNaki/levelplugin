package me.nakilex.levelplugin.items.tools;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;

public enum ToolTier {
    TIER_I(1, Arrays.asList(Material.WOODEN_PICKAXE)),
    TIER_II(10, Arrays.asList(Material.STONE_PICKAXE)),
    TIER_III(15, Arrays.asList(Material.GOLDEN_PICKAXE)),
    TIER_IV(25, Arrays.asList(Material.IRON_PICKAXE)),
    TIER_V(40, Arrays.asList(Material.DIAMOND_PICKAXE)),
    TIER_VI(60, Arrays.asList(Material.NETHERITE_PICKAXE));

    private final int levelReq;
    private final List<Material> mats;

    ToolTier(int levelReq, List<Material> mats) {
        this.levelReq = levelReq;
        this.mats = mats;
    }

    public int getLevelRequirement() {
        return levelReq;
    }

    public static ToolTier fromMaterial(Material mat) {
        for (ToolTier tier : values()) {
            if (tier.mats.contains(mat)) return tier;
        }
        return null;
    }
}
