package me.nakilex.levelplugin.stageddungeon;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.boss.BarColor;

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
        int stageTimeSeconds,
        BarColor bossBarColor,
        StagedDungeonObjective objective,
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

    public boolean isDamageMeter() {
        return objective == StagedDungeonObjective.DAMAGE_METER;
    }

    public boolean supportsSweeps() {
        return sweepAttempts > 0;
    }

    public double runMobHealth(int stage) {
        return isDamageMeter() ? 1_000_000_000.0D : mobHealth(stage);
    }

    public int rewardForStage(int stage) {
        return Math.max(1, rewardFromDamage(mobHealth(stage)));
    }

    public int rewardForSweep(int stage, double bestDamage) {
        return isDamageMeter() ? rewardFromDamage(bestDamage) : rewardForStage(stage);
    }

    public int rewardFromDamage(double damage) {
        return Math.max(0, (int) Math.round(Math.max(0.0D, damage) * 0.10D));
    }
}
