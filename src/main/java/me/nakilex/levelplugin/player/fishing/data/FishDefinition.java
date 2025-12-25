package me.nakilex.levelplugin.player.fishing.data;

import me.nakilex.levelplugin.items.data.ItemRarity;

public record FishDefinition(
        String id,
        String displayName,
        int minSize,
        int maxSize,
        ItemRarity rarity,
        int xpReward,
        int sellValue,
        double weight,
        int minLevel,
        boolean requiresLava,
        boolean requiresHighestTier
) {
}
