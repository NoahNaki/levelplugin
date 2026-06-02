package me.nakilex.levelplugin.player.attributes.lifeskill;

import java.util.UUID;

/** Generic progression contract shared by life-skill managers. */
public interface LifeSkillProgression {
    int getLevel(UUID uuid);
    int getXP(UUID uuid);
    void addXP(UUID uuid, int amount);
    void setLevel(UUID uuid, int level);
    void clearPlayerData(UUID uuid);
    int getXpRequired(int level);
    int getMaxLevel();

    /** Returns lifetime XP, including XP earned after the level cap. */
    default long getTotalXP(UUID uuid) {
        return getTotalXP(getLevel(uuid), getXP(uuid));
    }

    /** Calculates lifetime XP from persisted level and current-level XP values. */
    default long getTotalXP(int level, int currentLevelXp) {
        long total = Math.max(0, currentLevelXp);
        int completedLevels = Math.max(1, Math.min(level, getMaxLevel()));
        for (int completedLevel = 1; completedLevel < completedLevels; completedLevel++) {
            total += getXpRequired(completedLevel);
        }
        return total;
    }
}
