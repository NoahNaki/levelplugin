package me.nakilex.levelplugin.arena;

/**
 * Static helpers for Elo rating calculations.
 */
public final class EloUtil {
    private EloUtil() {}

    public static double expected(int my, int opp) {
        return 1.0 / (1.0 + Math.pow(10.0, (opp - my) / 400.0));
    }

    private static int kFactor(int mmr, boolean placement) {
        if (placement) return 48;
        if (mmr >= 2000) return 24;
        return 32;
    }

    /**
     * Update a rating based on result.
     * @param mmr current rating
     * @param opp opponent rating
     * @param win true if player won
     * @param placement whether still in placement matches
     * @param streak bonus multiplier (1.0 means none)
     * @return new rating rounded to nearest int
     */
    public static int update(int mmr, int opp, boolean win, boolean placement, double streak) {
        double e = expected(mmr, opp);
        double s = win ? 1.0 : 0.0;
        int k = kFactor(mmr, placement);
        double result = mmr + k * (s - e) * streak;
        return (int) Math.round(result);
    }
}
