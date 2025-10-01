package me.nakilex.levelplugin.player.battlepass.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Snapshot of a player's battle pass progress used for rendering the GUI.
 */
public record BattlePassView(
        List<BattlePassEntry> entries,
        int currentTier,
        int currentProgress,
        int requiredProgress,
        boolean premiumActive,
        String seasonLabel,
        String seasonEnds,
        String timeRemaining,
        int totalTiers,
        int claimedFreeRewards,
        int claimedPremiumRewards
) {
    public BattlePassView {
        if (entries == null || entries.isEmpty()) {
            entries = List.of();
        } else {
            entries = Collections.unmodifiableList(new ArrayList<>(entries));
        }
        currentTier = Math.max(0, currentTier);
        currentProgress = Math.max(0, currentProgress);
        requiredProgress = Math.max(0, requiredProgress);
        seasonLabel = seasonLabel == null ? "Battle Pass" : seasonLabel;
        seasonEnds = seasonEnds == null ? "Unknown" : seasonEnds;
        timeRemaining = timeRemaining == null ? "" : timeRemaining;
        totalTiers = Math.max(0, totalTiers);
        claimedFreeRewards = Math.max(0, claimedFreeRewards);
        claimedPremiumRewards = Math.max(0, claimedPremiumRewards);
    }

    public double progressFraction() {
        if (requiredProgress <= 0) {
            return 1.0;
        }
        double frac = (double) currentProgress / (double) requiredProgress;
        return Math.max(0.0, Math.min(1.0, frac));
    }

    public BattlePassEntry entryForTier(int tier) {
        for (BattlePassEntry entry : entries) {
            if (entry.tier() == tier) {
                return entry;
            }
        }
        return null;
    }
}
