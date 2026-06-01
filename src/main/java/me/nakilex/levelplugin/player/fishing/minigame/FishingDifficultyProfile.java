package me.nakilex.levelplugin.player.fishing.minigame;

/** Reusable tuning multipliers applied consistently across fishing mini-games. */
public record FishingDifficultyProfile(
        FishingMiniGameDifficulty tier,
        double durationMultiplier,
        double speedMultiplier,
        double zoneMultiplier,
        double requiredProgressMultiplier,
        double decayMultiplier,
        int sequenceBonusLength
) {
    public FishingDifficultyProfile {
        tier = tier == null ? FishingMiniGameDifficulty.NORMAL : tier;
    }

    public static FishingDifficultyProfile normal() {
        return forTier(FishingMiniGameDifficulty.NORMAL);
    }

    public static FishingDifficultyProfile forTier(FishingMiniGameDifficulty tier) {
        return switch (tier == null ? FishingMiniGameDifficulty.NORMAL : tier) {
            case EASY -> new FishingDifficultyProfile(FishingMiniGameDifficulty.EASY, 1.35, 0.65, 1.45, 0.75, 0.70, -2);
            case HARD -> new FishingDifficultyProfile(FishingMiniGameDifficulty.HARD, 0.85, 1.25, 0.75, 1.25, 1.25, 1);
            case EXTREME -> new FishingDifficultyProfile(FishingMiniGameDifficulty.EXTREME, 0.70, 1.45, 0.60, 1.45, 1.45, 2);
            default -> new FishingDifficultyProfile(FishingMiniGameDifficulty.NORMAL, 1.0, 1.0, 1.0, 1.0, 1.0, 0);
        };
    }

    public FishingDifficultyProfile withRodAssistance(double durationBonus, double zoneBonus, double speedReduction) {
        return new FishingDifficultyProfile(tier,
                durationMultiplier + durationBonus,
                Math.max(0.1, speedMultiplier - speedReduction),
                zoneMultiplier + zoneBonus,
                requiredProgressMultiplier,
                decayMultiplier,
                sequenceBonusLength);
    }
}
