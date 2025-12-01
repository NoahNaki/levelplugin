package me.nakilex.levelplugin.mob.utils;

import me.nakilex.levelplugin.items.data.ItemRarity;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility methods for deriving rewards from a mob's combat power.
 */
public final class CombatRewardCalculator {

    private static final double GEAR_SCORE_PORTION = 0.25;
    private static final double GEAR_SCORE_VARIANCE = 0.20;

    private CombatRewardCalculator() {
    }

    /**
     * Roll a target gear score and rarity for a given combat power.
     * The base gear score is 25% of combat power with a ±30% band and
     * equal odds to land on the lower, middle, or upper band. Only
     * rarities up to RARE are returned.
     */
    public static GearTarget rollGearTarget(int combatPower) {
        int base = Math.max(1, (int) Math.round(combatPower * GEAR_SCORE_PORTION));
        double variance = base * GEAR_SCORE_VARIANCE;
        double lowerBound = Math.max(1, base - variance);
        double upperBound = base + variance;

        double roll = ThreadLocalRandom.current().nextDouble();
        double min;
        double max;
        ItemRarity rarity;

        if (roll < 1.0 / 3.0) {
            rarity = ItemRarity.COMMON;
            min = lowerBound;
            max = base;
        } else if (roll < 2.0 / 3.0) {
            rarity = ItemRarity.UNCOMMON;
            min = base * 0.95; // keep the middle band tight around the base
            max = base * 1.05;
        } else {
            rarity = ItemRarity.RARE;
            min = base;
            max = upperBound;
        }

        int targetGearScore = (int) Math.round(ThreadLocalRandom.current().nextDouble(min, max + 1));
        return new GearTarget(targetGearScore, rarity);
    }

    /**
     * XP reward is 10% of the combat power (rounded to the nearest 10),
     * then rounded to the nearest whole number.
     */
    public static int calculateXpReward(int combatPower) {
        int roundedCombatPower = (int) (Math.round(combatPower / 10.0) * 10);
        return (int) Math.round(roundedCombatPower * 0.10);
    }

    /**
     * Coins reward is 10% of combat power, rounded to the nearest whole number.
     */
    public static int calculateCoinReward(int combatPower) {
        return (int) Math.round(combatPower * 0.05);
    }

    /**
     * Simple value object describing the rolled gear target and rarity.
     */
    public record GearTarget(int targetGearScore, ItemRarity rarity) {}
}
