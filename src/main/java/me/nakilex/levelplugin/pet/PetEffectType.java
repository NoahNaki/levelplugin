package me.nakilex.levelplugin.pet;

import org.bukkit.ChatColor;

import java.util.Locale;

public enum PetEffectType {
    DAMAGE_BOOST("damage_boost", "Fury",
            value -> "Deal " + ChatColor.GREEN + "+" + formatPercent(value)
                    + ChatColor.GRAY + " damage"),
    DAMAGE_REDUCTION("damage_reduction", "Bulwark",
            value -> "Take " + ChatColor.RED + "-" + formatPercent(value)
                    + ChatColor.GRAY + " damage"),
    LIFE_STEAL("life_steal", "Life Siphon",
            value -> "Heal " + ChatColor.GREEN + formatPercent(value)
                    + ChatColor.GRAY + " of damage dealt"),
    EXECUTE("execute", "Cull",
            value -> "Deal " + ChatColor.GREEN + "+" + formatPercent(value)
                    + ChatColor.GRAY + " vs targets below "
                    + ChatColor.RED + formatPercent(0.3) + ChatColor.GRAY),
    MOVEMENT_MANA_REDUCTION("movement_mana_reduction", "Swiftstep",
            value -> "Movement spells cost " + ChatColor.AQUA + "-" + formatPercent(value)
                    + ChatColor.GRAY),
    CRIT_CHANCE("crit_chance", "Predator Instinct",
            value -> "Critical chance " + ChatColor.GREEN + "+" + formatPercent(value)
                    + ChatColor.GRAY),
    COIN_DAMAGE("coin_damage", "Gilded Edge",
            value -> "Deal " + ChatColor.GREEN + "+1%/1k coins"
                    + ChatColor.GRAY + " (cap " + ChatColor.GOLD + formatPercent(value)
                    + ChatColor.GRAY + ")"),
    EXECUTE_NON_BOSS("execute_non_boss", "Reaper",
            value -> "Execute non-bosses below " + ChatColor.RED + formatPercent(value)),
    EXTRA_JUMP("extra_jump", "Skybound",
            value -> ChatColor.AQUA + "+1" + ChatColor.GRAY + " max jump"),
    FIRST_STRIKE("first_strike", "Ambush",
            value -> "First hit vs full HP: " + ChatColor.GREEN + "+" + formatPercent(value)
                    + ChatColor.GRAY + " dmg"),
    STATIONARY_REGEN("stationary_regen", "Stillness",
            value -> "Regen " + ChatColor.GREEN + formatPercent(value)
                    + ChatColor.GRAY + " HP/sec while still"),
    LAST_STAND("last_stand", "Last Stand",
            value -> "Fatal hit: " + ChatColor.RED + "Immune 5s"
                    + ChatColor.GRAY + ", " + ChatColor.GREEN + "+150% dmg"
                    + ChatColor.GRAY + ", " + ChatColor.AQUA + "+25% speed"
                    + ChatColor.GRAY + " (10m cd)");

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
        return 0.3;
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
