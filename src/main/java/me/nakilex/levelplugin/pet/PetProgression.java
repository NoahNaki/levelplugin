package me.nakilex.levelplugin.pet;

public final class PetProgression {
    private PetProgression() {
    }

    public static int levelFromXp(int xp, int xpPerLevel, int maxLevel) {
        int safeXpPerLevel = Math.max(1, xpPerLevel);
        int safeMax = Math.max(1, maxLevel);
        int level = 1 + Math.max(0, xp) / safeXpPerLevel;
        return Math.min(level, safeMax);
    }

    public static int xpForLevel(int level, int xpPerLevel) {
        int safeLevel = Math.max(1, level);
        int safeXpPerLevel = Math.max(1, xpPerLevel);
        return (safeLevel - 1) * safeXpPerLevel;
    }
}
