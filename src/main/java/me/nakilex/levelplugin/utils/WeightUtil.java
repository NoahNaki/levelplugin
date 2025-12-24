package me.nakilex.levelplugin.utils;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.ToDoubleFunction;

/** Utility helpers for weighted random selection. */
public final class WeightUtil {
    private WeightUtil() {
    }

    public static <T> T pickWeighted(Random random, Map<T, Double> weights) {
        if (weights == null || weights.isEmpty()) {
            return null;
        }
        double total = 0.0;
        for (double weight : weights.values()) {
            total += Math.max(0.0, weight);
        }
        if (total <= 0.0) {
            return weights.keySet().iterator().next();
        }
        double roll = random.nextDouble() * total;
        for (var entry : weights.entrySet()) {
            roll -= Math.max(0.0, entry.getValue());
            if (roll <= 0.0) {
                return entry.getKey();
            }
        }
        return weights.keySet().iterator().next();
    }

    public static <T> T pickWeighted(Random random, List<T> entries, ToDoubleFunction<T> weightFn) {
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        double total = 0.0;
        for (T entry : entries) {
            total += Math.max(0.0, weightFn.applyAsDouble(entry));
        }
        if (total <= 0.0) {
            return entries.getFirst();
        }
        double roll = random.nextDouble() * total;
        for (T entry : entries) {
            roll -= Math.max(0.0, weightFn.applyAsDouble(entry));
            if (roll <= 0.0) {
                return entry;
            }
        }
        return entries.getFirst();
    }
}
