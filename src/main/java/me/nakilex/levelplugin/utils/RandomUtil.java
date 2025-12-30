package me.nakilex.levelplugin.utils;

import java.util.Map;
import java.util.Random;

public final class RandomUtil {

    private RandomUtil() {}

    /** Pick a value based on weighted probabilities. */
    public static <T> T pickWeighted(Random random, Map<T, Double> weights) {
        double total = 0;
        for (double w : weights.values()) {
            total += Math.max(0.0, w);
        }
        if (total <= 0) {
            return weights.keySet().iterator().next();
        }
        double r = random.nextDouble() * total;
        double cumulative = 0;
        for (var entry : weights.entrySet()) {
            cumulative += Math.max(0.0, entry.getValue());
            if (r <= cumulative) return entry.getKey();
        }
        return weights.keySet().iterator().next();
    }
}
