package me.nakilex.levelplugin.pet;

import java.util.Locale;

public enum PetEffectType {
    DAMAGE_BOOST("damage_boost", "Fury",
            value -> "Deal +" + formatPercent(value) + " damage"),
    DAMAGE_REDUCTION("damage_reduction", "Bulwark",
            value -> "Take -" + formatPercent(value) + " damage"),
    LIFE_STEAL("life_steal", "Life Siphon",
            value -> "Heal for " + formatPercent(value) + " of damage dealt"),
    EXECUTE("execute", "Cull",
            value -> "Deal +" + formatPercent(value) + " damage to targets below "
                    + formatPercent(EXECUTE_THRESHOLD) + " health"),
    MOVEMENT_MANA_REDUCTION("movement_mana_reduction", "Swiftstep",
            value -> "Movement spells cost " + formatPercent(value) + " less mana"),
    CRIT_CHANCE("crit_chance", "Predator Instinct",
            value -> "Critical chance +" + formatPercent(value));

    private static final double EXECUTE_THRESHOLD = 0.3;

    private final String id;
    private final String displayName;
    private final java.util.function.Function<Double, String> formatter;

    PetEffectType(String id, String displayName, java.util.function.Function<Double, String> formatter) {
        this.id = id;
        this.displayName = displayName;
        this.formatter = formatter;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public String formatDescription(double value) {
        return formatter.apply(Math.max(0.0, value));
    }

    public double executeThreshold() {
        return EXECUTE_THRESHOLD;
    }

    public static PetEffectType fromToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String normalized = token.trim().toLowerCase(Locale.ROOT);
        for (PetEffectType type : values()) {
            if (type.id.equalsIgnoreCase(normalized) || type.name().equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        return null;
    }

    private static String formatPercent(double value) {
        double percent = value * 100.0;
        if (Math.abs(percent - Math.round(percent)) < 0.01) {
            return String.format("%.0f%%", percent);
        }
        return String.format("%.1f%%", percent);
    }
}
