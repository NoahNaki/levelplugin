package me.nakilex.levelplugin.utils;

import java.util.Map;
import java.util.Random;

public final class RandomUtil {

    private RandomUtil() {}

    /** Pick a value based on weighted probabilities. */
    public static <T> T pickWeighted(Random random, Map<T, Double> weights) {
        double r = random.nextDouble();
        double cumulative = 0;
        for (var entry : weights.entrySet()) {
            cumulative += entry.getValue();
            if (r <= cumulative) return entry.getKey();
        }
        return weights.keySet().iterator().next();
    }
}
