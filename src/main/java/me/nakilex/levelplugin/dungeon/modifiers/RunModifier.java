package me.nakilex.levelplugin.dungeon.modifiers;

import me.nakilex.levelplugin.mob.custom.CustomMobDefinition;

/**
 * Generic per-run modifier hook shared by dungeon-like activities.
 * Existing dungeon callers can keep using reward/drop/mob hooks while
 * Stronghold can also reuse wave, damage, score, and elite objective hooks.
 */
public interface RunModifier {
    String id();

    default String displayName() {
        return id();
    }

    default int modifyRewardCoins(int baseCoins) {
        return baseCoins;
    }

    default double modifyDropChance(double baseChance) {
        return baseChance;
    }

    default CustomMobDefinition modifyMob(CustomMobDefinition base) {
        return base;
    }

    default int modifyWaveMobCount(int baseCount) {
        return baseCount;
    }

    default double modifyDamageTaken(double baseDamage) {
        return baseDamage;
    }

    default double modifyScoreMultiplier(double baseMultiplier) {
        return baseMultiplier;
    }

    default double modifyEliteObjectiveChance(double baseChance) {
        return baseChance;
    }
}
