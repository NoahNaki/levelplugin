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
            value -> {
                int jumps = Math.max(0, (int) Math.floor(value));
                return ChatColor.AQUA + "+" + jumps + ChatColor.GRAY
                        + (jumps == 1 ? " mid-air jump" : " mid-air jumps");
            }),
    FIRST_STRIKE("first_strike", "Ambush",
            value -> "First hit vs full HP: " + ChatColor.GREEN + "+" + formatPercent(value)
                    + ChatColor.GRAY + " dmg"),
    STATIONARY_REGEN("stationary_regen", "Stillness",
            value -> "Regen " + ChatColor.GREEN + formatPercent(value)
                    + ChatColor.GRAY + " HP/sec while still"),
    XP_BOOST("xp_boost", "Mentor",
            value -> "Gain " + ChatColor.GREEN + "+" + formatPercent(value)
                    + ChatColor.GRAY + " combat XP"),
    GATHERING_XP_BOOST("gathering_xp_boost", "Harvester",
            value -> "Gain " + ChatColor.GREEN + "+" + formatPercent(value)
                    + ChatColor.GRAY + " gathering XP"),
    SALVAGE_COINS_BONUS("salvage_coins_bonus", "Scrapper's Cut",
            value -> "Gain " + ChatColor.GREEN + "+" + formatPercent(value)
                    + ChatColor.GRAY + " coins from salvage"),
    SALVAGE_GEMS_BONUS("salvage_gems_bonus", "Gem Dredge",
            value -> "Gain " + ChatColor.GREEN + "+" + formatPercent(value)
                    + ChatColor.GRAY + " gems from salvage"),
    QUEST_COINS_BONUS("quest_coins_bonus", "Contractor",
            value -> "Gain " + ChatColor.GREEN + "+" + formatPercent(value)
                    + ChatColor.GRAY + " quest coin rewards"),
    QUEST_GEMS_BONUS("quest_gems_bonus", "Relic Appraiser",
            value -> "Gain " + ChatColor.GREEN + "+" + formatPercent(value)
                    + ChatColor.GRAY + " quest gem rewards"),
    MERGE_SUCCESS_BONUS("merge_success_bonus", "Fusion Savant",
            value -> "Gain " + ChatColor.GREEN + "+" + formatPercent(value)
                    + ChatColor.GRAY + " merge success chance"),
    GACHA_PITY_REDUCTION("gacha_pity_reduction", "Pitybreaker",
            value -> "Legendary pity triggers " + ChatColor.GREEN + formatFlat(value)
                    + ChatColor.GRAY + " pulls earlier"),
    GACHA_GEM_COST_REDUCTION("gacha_gem_cost_reduction", "Bargain Caller",
            value -> "Pet summons cost " + ChatColor.AQUA + "-" + formatPercent(value)
                    + ChatColor.GRAY + " gems"),
    CUSTOM_MOB_DAMAGE("custom_mob_damage", "Monster Hunter",
            value -> "Deal " + ChatColor.GREEN + "+" + formatPercent(value)
                    + ChatColor.GRAY + " damage to custom mobs"),
    BOSS_DAMAGE("boss_damage", "Boss Breaker",
            value -> "Deal " + ChatColor.GREEN + "+" + formatPercent(value)
                    + ChatColor.GRAY + " damage to boss mobs"),
    HUNT_MARK("hunt_mark", "Hunt Mark",
            value -> "Hits apply Hunt Mark. Each stack gives " + ChatColor.GREEN + "+" + formatPercent(value)
                    + ChatColor.GRAY + " damage vs that custom/boss mob (max 20 stacks)"),
    MARK_RUPTURE("mark_rupture", "Mark Rupture",
            value -> "At 20 Hunt Mark stacks: consume marks to burst for "
                    + ChatColor.GREEN + formatPercent(value)
                    + ChatColor.GRAY + " target max HP"),
    LOW_HEALTH_DAMAGE("low_health_damage", "Berserker Heart",
            value -> "Deal " + ChatColor.GREEN + "+" + formatPercent(value)
                    + ChatColor.GRAY + " damage while below " + ChatColor.RED + "50% HP"),
    HEALTHY_PREY_DAMAGE("healthy_prey_damage", "Opportunist",
            value -> "Deal " + ChatColor.GREEN + "+" + formatPercent(value)
                    + ChatColor.GRAY + " vs targets above " + ChatColor.YELLOW + "70% HP"),
    WOUNDED_PREY_DAMAGE("wounded_prey_damage", "Bloodtrail",
            value -> "Deal " + ChatColor.GREEN + "+" + formatPercent(value)
                    + ChatColor.GRAY + " vs targets below " + ChatColor.RED + "40% HP"),
    HIGH_HEALTH_DAMAGE("high_health_damage", "Peak Form",
            value -> "Deal " + ChatColor.GREEN + "+" + formatPercent(value)
                    + ChatColor.GRAY + " while above " + ChatColor.YELLOW + "80% HP"),
    FULL_HEALTH_GUARD("full_health_guard", "Aegis Opening",
            value -> "Take " + ChatColor.RED + "-" + formatPercent(value)
                    + ChatColor.GRAY + " damage while above " + ChatColor.YELLOW + "90% HP"),
    CUSTOM_MOB_GUARD("custom_mob_guard", "Monster Shell",
            value -> "Take " + ChatColor.RED + "-" + formatPercent(value)
                    + ChatColor.GRAY + " damage from custom mobs"),
    BOSS_GUARD("boss_guard", "Boss Ward",
            value -> "Take " + ChatColor.RED + "-" + formatPercent(value)
                    + ChatColor.GRAY + " damage from bosses"),
    LAST_STAND("last_stand", "Last Stand",
            value -> "On lethal hit: " + ChatColor.RED + "Immune 5s"
                    + ChatColor.GRAY + ", " + ChatColor.GREEN + "+150% dmg"
                    + ChatColor.GRAY + ", " + ChatColor.AQUA + "+25% speed"
                    + ChatColor.GRAY + " (10m cooldown)");

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

    private static String formatFlat(double value) {
        if (Math.abs(value - Math.round(value)) < 0.01) {
            return String.format("%.0f", value);
        }
        return String.format("%.1f", value);
    }

}
