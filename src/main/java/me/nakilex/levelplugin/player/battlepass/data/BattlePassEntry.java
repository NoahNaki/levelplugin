package me.nakilex.levelplugin.player.battlepass.data;

/**
 * Container for the free and premium rewards associated with a single tier.
 */
public record BattlePassEntry(
        int tier,
        BattlePassReward freeReward,
        BattlePassReward premiumReward
) {
    public BattlePassEntry {
        tier = Math.max(1, tier);
        if (freeReward == null) {
            freeReward = new BattlePassReward("", java.util.List.of(), false, false);
        }
        if (premiumReward == null) {
            premiumReward = new BattlePassReward("", java.util.List.of(), false, false);
        }
    }
}
