package me.nakilex.levelplugin.items.v2;

import java.text.DecimalFormat;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public final class StatValue {
    public enum Kind {
        FIXED,
        RANGE
    }

    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("0.##");

    private final Kind kind;
    private final double min;
    private final double max;

    private StatValue(Kind kind, double min, double max) {
        this.kind = kind;
        this.min = min;
        this.max = max;
    }

    public static StatValue fixed(double value) {
        return new StatValue(Kind.FIXED, value, value);
    }

    public static StatValue range(double min, double max) {
        if (max < min) {
            throw new IllegalArgumentException("max must be >= min");
        }
        return new StatValue(Kind.RANGE, min, max);
    }

    public static StatValue fromLegacyRangeString(String value) {
        if (value == null || value.isBlank()) {
            return StatValue.fixed(0);
        }
        String[] parts = value.split("-", 2);
        if (parts.length != 2) {
            double parsed = Double.parseDouble(value.trim());
            return StatValue.fixed(parsed);
        }
        double min = Double.parseDouble(parts[0].trim());
        double max = Double.parseDouble(parts[1].trim());
        if (Double.compare(min, max) == 0) {
            return StatValue.fixed(min);
        }
        return StatValue.range(min, max);
    }

    public Kind kind() {
        return kind;
    }

    public double min() {
        return min;
    }

    public double max() {
        return max;
    }

    public boolean isFixed() {
        return kind == Kind.FIXED;
    }

    public double roll(Random random) {
        Random rng = random != null ? random : ThreadLocalRandom.current();
        if (isFixed()) {
            return min;
        }
        return min + (max - min) * rng.nextDouble();
    }

    public String formatForLore() {
        if (isFixed()) {
            return formatNumber(min);
        }
        return formatNumber(min) + "-" + formatNumber(max);
    }

    private String formatNumber(double value) {
        return NUMBER_FORMAT.format(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StatValue that)) return false;
        return Double.compare(that.min, min) == 0
                && Double.compare(that.max, max) == 0
                && kind == that.kind;
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, min, max);
    }

    @Override
    public String toString() {
        return formatForLore();
    }
}
