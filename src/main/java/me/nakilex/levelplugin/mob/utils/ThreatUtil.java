package me.nakilex.levelplugin.mob.utils;

/** Utility to map combat power values to threat levels. */
public final class ThreatUtil {
    private ThreatUtil() {}

    /**
     * Convert a raw combat power value into a threat level between 1 and 5.
     */
    public static int levelForPower(int power) {
        if (power <= 1000) return 1;
        if (power <= 5000) return 2;
        if (power <= 10000) return 3;
        if (power <= 15000) return 4;
        return 5;
    }
}
