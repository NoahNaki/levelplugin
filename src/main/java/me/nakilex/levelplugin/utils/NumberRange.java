package me.nakilex.levelplugin.utils;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Represents an inclusive numeric range that can be rolled to produce a random value.
 * Supports parsing from common YAML representations (single value or "min-max" string).
 */
public final class NumberRange {

    private final double min;
    private final double max;

    public NumberRange(double min, double max) {
        if (Double.isNaN(min) || Double.isNaN(max)) {
            throw new IllegalArgumentException("Range bounds must be numbers");
        }
        if (max < min) {
            throw new IllegalArgumentException("Max must be greater than or equal to min");
        }
        this.min = min;
        this.max = max;
    }

    public static NumberRange fixed(double value) {
        return new NumberRange(value, value);
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    /** Inclusive roll across the range. */
    public double roll() {
        if (min == max) {
            return min;
        }
        double high = Math.nextUp(max);
        return ThreadLocalRandom.current().nextDouble(min, high);
    }

    /** Midpoint used for deterministic fallbacks. */
    public double midpoint() {
        return (min + max) / 2.0;
    }

    /**
     * Parse either a single value ("12.5") or a dash-separated range ("10-25") into a NumberRange.
     */
    public static NumberRange parse(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Range text cannot be null");
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Range text cannot be blank");
        }
        int dashIndex = trimmed.indexOf('-');
        if (dashIndex < 0) {
            double value = Double.parseDouble(trimmed);
            return fixed(value);
        }
        String[] parts = trimmed.split("-");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid range format: " + text);
        }
        double min = Double.parseDouble(parts[0].trim());
        double max = Double.parseDouble(parts[1].trim());
        return new NumberRange(min, max);
    }

    /**
     * Attempt to coerce various YAML representations into a NumberRange.
     */
    public static NumberRange coerce(Object raw, double defaultValue) {
        if (raw == null) {
            return fixed(defaultValue);
        }
        if (raw instanceof Number number) {
            return fixed(number.doubleValue());
        }
        if (raw instanceof String str) {
            return parse(str);
        }
        if (raw instanceof List<?> list && list.size() == 2) {
            Object first = list.get(0);
            Object second = list.get(1);
            if (first instanceof Number a && second instanceof Number b) {
                return new NumberRange(a.doubleValue(), b.doubleValue());
            }
        }
        throw new IllegalArgumentException("Unsupported range format: " + raw);
    }
}

