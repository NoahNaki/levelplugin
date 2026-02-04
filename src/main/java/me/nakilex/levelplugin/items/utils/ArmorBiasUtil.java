package me.nakilex.levelplugin.items.utils;

import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.items.data.StatRange;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public final class ArmorBiasUtil {
    private static final double MIN_FOCUS_SHARE = 0.60;
    private static final double MAX_FOCUS_SHARE = 0.70;
    private static final double RARITY_STEP = 1.4;
    private static final double RANGE_VARIANCE = 0.05;

    public enum ArmorBias {
        CLOTH("Cloth"),
        LEATHER("Leather"),
        PLATED("Plated");

        private final String label;

        ArmorBias(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        public static ArmorBias fromPrefix(String name) {
            if (name == null) return null;
            String lower = name.toLowerCase(Locale.ROOT);
            for (ArmorBias bias : values()) {
                if (lower.startsWith(bias.label.toLowerCase(Locale.ROOT) + " ")) {
                    return bias;
                }
            }
            return null;
        }
    }

    public enum ArmorStat {
        HP,
        DEF,
        STR,
        AGI,
        INT,
        DEX,
        WIL,
        TEC
    }

    private ArmorBiasUtil() {}

    public static ArmorBias randomBias(Random random) {
        Random rng = random != null ? random : new Random();
        ArmorBias[] values = ArmorBias.values();
        return values[rng.nextInt(values.length)];
    }

    public static String applyPrefix(String baseName, ArmorBias bias) {
        if (baseName == null || baseName.isBlank() || bias == null) {
            return baseName;
        }
        String label = bias.label();
        String lower = baseName.toLowerCase(Locale.ROOT);
        if (lower.startsWith(label.toLowerCase(Locale.ROOT) + " ")) {
            return baseName;
        }
        return label + " " + baseName;
    }

    public static int estimateTotal(int level, ItemRarity rarity) {
        int safeLevel = Math.max(1, Math.min(level, 100));
        double multiplier = Math.pow(RARITY_STEP, rarity == null ? 0 : rarity.ordinal());
        return (int) Math.ceil(safeLevel * 8 * multiplier);
    }

    public static Map<ArmorStat, Integer> allocate(int total, int level, ArmorBias bias) {
        int safeTotal = Math.max(1, total);
        double focusShare = Math.min(MAX_FOCUS_SHARE, MIN_FOCUS_SHARE + (Math.max(1, Math.min(level, 100)) / 100.0) * 0.10);
        double focusTotal = safeTotal * focusShare;
        double otherTotal = safeTotal - focusTotal;

        Map<ArmorStat, Integer> values = new EnumMap<>(ArmorStat.class);
        for (ArmorStat stat : ArmorStat.values()) {
            values.put(stat, 0);
        }

        if (bias == ArmorBias.PLATED) {
            double strTotal = focusTotal * 0.5;
            double vitTotal = focusTotal * 0.5;
            values.put(ArmorStat.STR, (int) Math.round(strTotal));
            values.put(ArmorStat.HP, (int) Math.round(vitTotal * 0.6));
            values.put(ArmorStat.DEF, (int) Math.round(vitTotal * 0.4));
            distributeOther(values, otherTotal, List.of(
                    ArmorStat.AGI,
                    ArmorStat.INT,
                    ArmorStat.DEX,
                    ArmorStat.WIL,
                    ArmorStat.TEC
            ));
        } else if (bias == ArmorBias.LEATHER) {
            double agiTotal = focusTotal * 0.5;
            double dexTotal = focusTotal * 0.5;
            values.put(ArmorStat.AGI, (int) Math.round(agiTotal));
            values.put(ArmorStat.DEX, (int) Math.round(dexTotal));
            distributeOther(values, otherTotal, List.of(
                    ArmorStat.HP,
                    ArmorStat.DEF,
                    ArmorStat.STR,
                    ArmorStat.INT,
                    ArmorStat.WIL,
                    ArmorStat.TEC
            ));
        } else {
            double intTotal = focusTotal * 0.5;
            double wilTotal = focusTotal * 0.5;
            values.put(ArmorStat.INT, (int) Math.round(intTotal));
            values.put(ArmorStat.WIL, (int) Math.round(wilTotal));
            distributeOther(values, otherTotal, List.of(
                    ArmorStat.HP,
                    ArmorStat.DEF,
                    ArmorStat.STR,
                    ArmorStat.AGI,
                    ArmorStat.DEX,
                    ArmorStat.TEC
            ));
        }

        return values;
    }

    private static void distributeOther(Map<ArmorStat, Integer> values, double total, List<ArmorStat> stats) {
        if (stats.isEmpty()) {
            return;
        }
        double per = total / stats.size();
        int remainder = (int) Math.round(total - (per * stats.size()));
        for (ArmorStat stat : stats) {
            values.put(stat, values.get(stat) + (int) Math.floor(per));
        }
        for (int i = 0; i < remainder; i++) {
            ArmorStat stat = stats.get(i % stats.size());
            values.put(stat, values.get(stat) + 1);
        }
    }

    public static StatRange toRange(int value) {
        if (value <= 0) {
            return new StatRange(0, 0);
        }
        int min = Math.max(0, (int) Math.round(value * (1 - RANGE_VARIANCE)));
        int max = Math.max(min + 1, (int) Math.round(value * (1 + RANGE_VARIANCE)));
        return new StatRange(min, max);
    }
}
