package me.nakilex.levelplugin.utils;

/** Utility methods for scaling and adjusting experience rewards. */
public final class ExperienceUtil {
    private ExperienceUtil() {}

    /**
     * Scale a base XP value according to the level gap between a player and a mob.
     * Full XP is awarded when the level difference is within five levels. Beyond
     * that, XP is reduced by 4% per level and floored at 10% of the base value to
     * discourage farming content far below or above the player's level.
     *
     * @param baseExp     original XP value from configuration
     * @param playerLevel player's current level
     * @param mobLevel    mob's level
     * @return scaled XP amount
     */
    public static int scaleExperience(int baseExp, int playerLevel, double mobLevel) {
        int diff = Math.abs(playerLevel - (int)Math.round(mobLevel));
        if (diff <= 5) {
            return baseExp;
        }
        double multiplier = 1.0 - 0.04 * (diff - 5);
        if (multiplier < 0.10) {
            multiplier = 0.10;
        }
        return (int)Math.round(baseExp * multiplier);
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
        return applyPartyBonus(exp, partySize, 1.0);
    }

    /**
     * Apply a party bonus and optional composition multiplier.
     *
     * @param exp scaled XP value
     * @param partySize number of nearby party members including the player
     * @param synergyMultiplier multiplier from composition systems (1.0 for none)
     * @return XP with party and synergy bonus applied
     */
    public static int applyPartyBonus(int exp, int partySize, double synergyMultiplier) {
        int bonusPercent;
        switch (partySize) {
            case 2 -> bonusPercent = 10;
            case 3 -> bonusPercent = 25;
            default -> bonusPercent = partySize >= 4 ? 40 : 0;
        }
        double base = exp + (exp * bonusPercent) / 100.0;
        double synergy = Math.max(1.0, synergyMultiplier);
        return (int) Math.round(base * synergy);
    }
}
