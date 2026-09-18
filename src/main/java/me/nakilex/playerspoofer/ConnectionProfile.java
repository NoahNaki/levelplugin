package me.nakilex.playerspoofer;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

enum ConnectionProfile {
    EXCELLENT,
    GOOD,
    AVERAGE,
    SPIKY,
    POOR;

    static ConnectionProfile randomWeighted() {
        // Deliberately healthy population: ~95.5% are normally <= 300ms.
        // SPIKY normally looks healthy and only occasionally has a temporary bad sample.
        int roll = ThreadLocalRandom.current().nextInt(10_000);
        if (roll < 7_000) return EXCELLENT;      // 70.0%
        if (roll < 9_550) return GOOD;           // 25.5%
        if (roll < 9_900) return AVERAGE;        // 3.5%
        if (roll < 9_980) return SPIKY;           // 0.8%
        return POOR;                              // 0.2%
    }

    static ConnectionProfile fromStored(String value, int legacyPing) {
        if (value != null) {
            try {
                return valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (legacyPing < 150) return EXCELLENT;
        if (legacyPing < 300) return GOOD;
        if (legacyPing < 600) return AVERAGE;
        return POOR;
    }

    int randomBasePing() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        return switch (this) {
            case EXCELLENT -> r.nextInt(35, 111);
            case GOOD -> r.nextInt(105, 221);
            case AVERAGE -> r.nextInt(210, 401);
            case SPIKY -> r.nextInt(45, 151);
            case POOR -> r.nextInt(650, 1201);
        };
    }

    int initialPing(int basePing) {
        return normalSample(basePing);
    }

    int nextPing(int basePing) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        if (this == SPIKY && r.nextInt(100) < 7) {
            // A short-lived spike: next jitter sample normally drops back to the good baseline.
            return r.nextInt(320, 751);
        }
        return normalSample(basePing);
    }

    private int normalSample(int basePing) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        int spread = switch (this) {
            case EXCELLENT -> 9;
            case GOOD -> 16;
            case AVERAGE -> 24;
            case SPIKY -> 13;
            case POOR -> 55;
        };
        int sample = basePing + r.nextInt(-spread, spread + 1);
        return switch (this) {
            case EXCELLENT -> clamp(sample, 30, 145);
            case GOOD -> clamp(sample, 90, 285);
            case AVERAGE -> clamp(sample, 180, 520);
            case SPIKY -> clamp(sample, 35, 180);
            case POOR -> clamp(sample, 600, 1300);
        };
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
