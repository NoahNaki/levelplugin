package me.nakilex.levelplugin.stageddungeon;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

/**
 * Immutable configuration for a single stage-based dungeon type.
 * New currency dungeons can add another definition without duplicating run logic.
 */
public record StagedDungeonDefinition(
        String id,
        String displayName,
        ChatColor themeColor,
        Material icon,
        String worldPrefix,
        EntityType mobType,
        String mobDisplayName,
        double baseMobHealth,
        double healthPerStage,
        int sweepAttempts,
        String rewardName,
        String rewardGlyph,
        RewardGrant rewardGrant
) {
    public int nextStage(int highestCleared) {
        return Math.max(1, highestCleared + 1);
    }

    public int sweepStage(int highestCleared) {
        return Math.max(1, highestCleared);
    }

    public double mobHealth(int stage) {
        return Math.max(1.0, baseMobHealth + (Math.max(1, stage) - 1) * healthPerStage);
    }

    public int rewardForStage(int stage) {
        return Math.max(1, (int) Math.round(mobHealth(stage) * 0.10D));
    }
}
