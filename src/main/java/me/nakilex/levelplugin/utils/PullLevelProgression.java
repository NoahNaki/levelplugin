package me.nakilex.levelplugin.utils;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Shared progression model for summon banners whose pull odds improve as players pull more.
 */
public final class PullLevelProgression {
    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 10;
    private static final int[] PULLS_TO_NEXT_LEVEL = {10, 25, 50, 100, 175, 275, 400, 550, 750};

    private PullLevelProgression() {
    }

    public static int levelForPulls(int pulls) {
        int remaining = Math.max(0, pulls);
        int level = MIN_LEVEL;
        for (int requirement : PULLS_TO_NEXT_LEVEL) {
            if (remaining < requirement) {
                break;
            }
            remaining -= requirement;
            level++;
            if (level >= MAX_LEVEL) {
                return MAX_LEVEL;
            }
        }
        return level;
    }

    public static int progressIntoLevel(int pulls) {
        int remaining = Math.max(0, pulls);
        for (int requirement : PULLS_TO_NEXT_LEVEL) {
            if (remaining < requirement) {
                return remaining;
            }
            remaining -= requirement;
        }
        return requiredForNextLevel(MAX_LEVEL);
    }

    public static int requiredForNextLevel(int level) {
        if (level >= MAX_LEVEL) {
            return 0;
        }
        int index = Math.max(0, Math.min(PULLS_TO_NEXT_LEVEL.length - 1, level - MIN_LEVEL));
        return PULLS_TO_NEXT_LEVEL[index];
    }

    public static <E extends Enum<E>> Map<E, Double> ratesForLevel(List<E> rarityOrder,
                                                                    Map<E, Double> baseWeights,
                                                                    int level) {
        if (rarityOrder == null || rarityOrder.isEmpty() || baseWeights == null || baseWeights.isEmpty()) {
            return Map.of();
        }
        E common = rarityOrder.get(0);
        double t = (Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, level)) - MIN_LEVEL) / (double) (MAX_LEVEL - MIN_LEVEL);
        Map<E, Double> adjusted = new EnumMap<>(common.getDeclaringClass());
        for (E rarity : rarityOrder) {
            double base = Math.max(0.0, baseWeights.getOrDefault(rarity, 0.0));
            double levelOne = rarity == common ? 100.0 : 0.0;
            double value = levelOne + ((base - levelOne) * t);
            if (value > 0.0) {
                adjusted.put(rarity, value);
            }
        }
        return adjusted;
    }
}
