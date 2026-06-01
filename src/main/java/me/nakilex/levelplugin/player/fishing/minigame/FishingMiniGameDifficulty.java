package me.nakilex.levelplugin.player.fishing.minigame;

/** Ordered fishing mini-game difficulty tiers, from most forgiving to most demanding. */
public enum FishingMiniGameDifficulty {
    EASY,
    NORMAL,
    HARD,
    EXTREME;

    public FishingMiniGameDifficulty shift(int steps) {
        FishingMiniGameDifficulty[] tiers = values();
        return tiers[Math.max(0, Math.min(tiers.length - 1, ordinal() + steps))];
    }
}
