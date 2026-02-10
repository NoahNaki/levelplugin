package me.nakilex.levelplugin.pet;

import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.StatType;
import org.bukkit.configuration.ConfigurationSection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public record PetDefinition(String id,
                            String displayName,
                            ItemRarity rarity,
                            List<String> modelIds,
                            Map<StatType, Integer> ownershipStats,
                            Map<StatType, Integer> baseStats,
                            Map<StatType, Integer> perLevelStats,
                            List<PetEffectDefinition> effects,
                            int maxLevel,
                            int xpPerLevel) {

    public Map<StatType, Integer> statsForLevel(int level) {
        return statsForLevel(level, 0);
    }

    public Map<StatType, Integer> statsForLevel(int level, int tier) {
        int safeLevel = Math.max(1, level);
        double tierMultiplier = 1.0 + Math.max(0, tier) * 0.1;
        Map<StatType, Integer> totals = new EnumMap<>(StatType.class);
        for (StatType type : StatType.values()) {
            int base = baseStats.getOrDefault(type, 0);
            int perLevel = perLevelStats.getOrDefault(type, 0);
            int total = base + (safeLevel - 1) * perLevel;
            int scaled = (int) Math.round(total * tierMultiplier);
            if (scaled != 0) {
                totals.put(type, scaled);
            }
        }
        return totals;
    }

    public List<PetEffectDefinition> effectsForLevel(int level) {
        return effectsForLevel(level, 0);
    }

    public List<PetEffectDefinition> effectsForLevel(int level, int tier) {
        if (effects.isEmpty()) {
            return List.of();
        }
        int safeLevel = Math.max(1, level);
        List<PetEffectDefinition> scaled = new ArrayList<>(effects.size());
        for (PetEffectDefinition effect : effects) {
            if (effect == null || effect.type() == null) {
                continue;
            }
            double value = effect.valueForLevel(safeLevel);
            double scaledValue = scaleEffectValue(value, tier, rarity);
            scaledValue = applySpecialScaling(effect.type(), scaledValue, safeLevel, tier);
            scaled.add(new PetEffectDefinition(effect.type(), scaledValue, 0));
        }
        return scaled;
    }

    private double applySpecialScaling(PetEffectType type, double scaledValue, int level, int tier) {
        if (type == PetEffectType.EXTRA_JUMP
                && "skyhopper".equalsIgnoreCase(id)
                && level >= 100
                && tier >= 5) {
            return Math.max(scaledValue, 2.0);
        }
        return scaledValue;
    }

    private static double scaleEffectValue(double base, int tier, ItemRarity rarity) {
        double safeBase = Math.max(0.0, base);
        int safeTier = Math.max(1, tier);
        double tierMultiplier = 1.0 + (safeTier - 1) * 0.15;
        double rarityMultiplier = rarityEffectMultiplier(rarity);
        return safeBase * tierMultiplier * rarityMultiplier;
    }

    private static double rarityEffectMultiplier(ItemRarity rarity) {
        if (rarity == null) {
            return 1.0;
        }
        return switch (rarity) {
            case COMMON -> 1.0;
            case UNCOMMON -> 1.1;
            case RARE -> 1.25;
            case EPIC -> 1.4;
            case LEGENDARY -> 1.6;
            case MYTHIC -> 1.8;
            case FABLED -> 2.0;
        };
    }

    public static PetDefinition fromConfig(String id, ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        String name = section.getString("name", id);
        String rarityToken = section.getString("rarity", ItemRarity.COMMON.name());
        ItemRarity rarity = parseRarity(rarityToken);
        List<String> models = section.getStringList("models");
        int maxLevel = Math.max(1, section.getInt("max-level", 100));
        int xpPerLevel = Math.max(1, section.getInt("xp-per-level", 100));
        Map<StatType, Integer> ownershipStats = parseStats(section.getConfigurationSection("owned-stats"));
        Map<StatType, Integer> base = parseStats(section.getConfigurationSection("stats.base"));
        Map<StatType, Integer> perLevel = parseStats(section.getConfigurationSection("stats.per-level"));
        List<PetEffectDefinition> effects = parseEffects(section.getMapList("effects"));

        return new PetDefinition(id, name, rarity, models, ownershipStats, base, perLevel, effects, maxLevel, xpPerLevel);
    }

    private static Map<StatType, Integer> parseStats(ConfigurationSection section) {
        if (section == null) {
            return Collections.emptyMap();
        }
        Map<StatType, Integer> values = new EnumMap<>(StatType.class);
        for (String key : section.getKeys(false)) {
            StatType type = StatType.fromKey(key);
            if (type == null) {
                continue;
            }
            int value = section.getInt(key, 0);
            if (value != 0) {
                values.put(type, value);
            }
        }
        return values;
    }

    private static List<PetEffectDefinition> parseEffects(List<Map<?, ?>> list) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        List<PetEffectDefinition> effects = new ArrayList<>();
        for (Map<?, ?> entry : list) {
            if (entry == null) {
                continue;
            }
            Object typeRaw = entry.get("type");
            if (typeRaw == null) {
                continue;
            }
            PetEffectType type = PetEffectType.fromToken(typeRaw.toString());
            if (type == null) {
                continue;
            }
            double base = parseDouble(entry.get("base"), 0.0);
            double perLevel = parseDouble(entry.get("per-level"), 0.0);
            effects.add(new PetEffectDefinition(type, base, perLevel));
        }
        return effects;
    }

    private static int parseInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    private static double parseDouble(Object value, double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value != null) {
            try {
                return Double.parseDouble(value.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    private static ItemRarity parseRarity(String token) {
        if (token == null) {
            return ItemRarity.COMMON;
        }
        try {
            return ItemRarity.valueOf(token.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return ItemRarity.COMMON;
        }
    }
}
