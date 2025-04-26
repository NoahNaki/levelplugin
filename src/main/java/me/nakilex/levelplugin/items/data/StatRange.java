package me.nakilex.levelplugin.items.data;

import java.util.concurrent.ThreadLocalRandom;

public class StatRange {
    private final int min;
    private final int max;

    public StatRange(int min, int max) {
        if (max < min) throw new IllegalArgumentException("max must be ≥ min");
        this.min = min;
        this.max = max;
    }

    public int getMin() { return min; }
    public int getMax() { return max; }

    /**
     * Roll a random int in [min…max], inclusive
     */
    public int roll() {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    @Override
    public String toString() {
        return min + "-" + max;
    }

    /**
     * Parse a string of the form "X-Y" into a StatRange
     */
    public static StatRange fromString(String s) {
        String[] parts = s.split("-");
        if (parts.length != 2) throw new IllegalArgumentException("invalid range: " + s);
        int a = Integer.parseInt(parts[0].trim());
        int b = Integer.parseInt(parts[1].trim());
        return new StatRange(a, b);
    }
}

