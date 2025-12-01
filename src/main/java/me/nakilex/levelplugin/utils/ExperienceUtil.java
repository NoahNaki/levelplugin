package me.nakilex.levelplugin.utils;

/** Utility methods for scaling and adjusting experience rewards. */
public final class ExperienceUtil {
    private ExperienceUtil() {}

    /**
     * Scale a base XP value according to how far a player's level exceeds the mob's level.
     * Full XP is awarded when the player is within five levels of the mob or lower level.
     * Beyond that, XP is reduced by 4% per level difference with a floor at 50% of the
     * base value to keep rewards predictable for high-combat-power mobs that are set to
     * low MythicMob levels.
     *
     * @param baseExp     original XP value derived from combat power
     * @param playerLevel player's current level
     * @param mobLevel    mob's level
     * @return scaled XP amount
     */
    public static int scaleExperience(int baseExp, int playerLevel, double mobLevel) {
        int roundedMobLevel = Math.max(0, (int) Math.round(mobLevel));
        int levelLead = playerLevel - roundedMobLevel;
        if (levelLead <= 5) {
            return baseExp;
        }

        double multiplier = 1.0 - 0.04 * (levelLead - 5);
        if (multiplier < 0.50) {
            multiplier = 0.50;
        }
        return (int) Math.round(baseExp * multiplier);
    }

    /**
     * Apply a party bonus to an XP amount based on nearby party size.
     * 2 members -> +10%, 3 members -> +25%, 4 or more members -> +40%.
     *
     * @param exp       scaled XP value
     * @param partySize number of nearby party members including the player
     * @return XP with party bonus applied
     */
    public static int applyPartyBonus(int exp, int partySize) {
        int bonusPercent;
        switch (partySize) {
            case 2 -> bonusPercent = 10;
            case 3 -> bonusPercent = 25;
            default -> bonusPercent = partySize >= 4 ? 40 : 0;
        }
        return exp + (exp * bonusPercent) / 100;
    }
}
