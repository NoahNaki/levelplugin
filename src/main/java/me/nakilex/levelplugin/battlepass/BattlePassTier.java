package me.nakilex.levelplugin.battlepass;

/**
 * Represents a tier within the battle pass containing free and premium rewards.
 */
public record BattlePassTier(int index, BattlePassReward freeReward, BattlePassReward premiumReward) {

    public BattlePassTier {
        if (index <= 0) {
            throw new IllegalArgumentException("Tier index must be positive");
        }
    }
}
