package me.nakilex.levelplugin.pet;

import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.StatType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffectType;

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
        int tierBonus = Math.max(0, tier);
        List<PetEffectDefinition> scaled = new ArrayList<>(effects.size());
        for (PetEffectDefinition effect : effects) {
            if (effect == null || effect.type() == null) {
                continue;
            }
            int amp = effect.amplifierForLevel(safeLevel) + tierBonus;
            scaled.add(new PetEffectDefinition(effect.type(), amp, 0));
        }
        return scaled;
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
        Map<StatType, Integer> base = parseStats(section.getConfigurationSection("stats.base"));
        Map<StatType, Integer> perLevel = parseStats(section.getConfigurationSection("stats.per-level"));
        List<PetEffectDefinition> effects = parseEffects(section.getMapList("effects"));

        return new PetDefinition(id, name, rarity, models, base, perLevel, effects, maxLevel, xpPerLevel);
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
            String typeName = typeRaw.toString().toUpperCase(Locale.ROOT);
            PotionEffectType type = PotionEffectType.getByName(typeName);
            if (type == null) {
                continue;
            }
            int base = parseInt(entry.get("base"), 0);
            int perLevel = parseInt(entry.get("per-level"), 0);
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
