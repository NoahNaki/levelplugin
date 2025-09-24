package me.nakilex.levelplugin.trinkets.data;

import me.nakilex.levelplugin.trinkets.effects.TrinketEffectType;

/**
 * Immutable value object representing a single trinket effect.
 */
public class TrinketEffectDefinition {

    private final TrinketEffectType type;
    private final double magnitude;
    private final double durationSeconds;

    public TrinketEffectDefinition(TrinketEffectType type, double magnitude, double durationSeconds) {
        this.type = type;
        this.magnitude = magnitude;
        this.durationSeconds = durationSeconds;
    }

    public TrinketEffectType getType() {
        return type;
    }

    public double getMagnitude() {
        return magnitude;
    }

    public double getDurationSeconds() {
        return durationSeconds;
    }

    public int getMagnitudeTier() {
        return type.resolveMagnitudeTier(magnitude);
    }

    public int getDurationTier() {
        return type.resolveDurationTier(durationSeconds);
    }

    public String formatMagnitude() {
        return type.formatMagnitude(this);
    }
}
