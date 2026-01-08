package me.nakilex.levelplugin.mob.custom;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;

import java.util.List;

public record CustomMobDefinition(String id,
                                  EntityType entityType,
                                  String displayName,
                                  LevelRange levelRange,
                                  Double baseHealth,
                                  CustomMobStats stats,
                                  List<String> models,
                                  CustomMobOptions options,
                                  boolean boss) {

    public record LevelRange(int min, int max) {
        public LevelRange {
            if (min < 1) {
                min = 1;
            }
            if (max < min) {
                max = min;
            }
        }

        public int pickLevel(java.util.Random random) {
            if (min == max) {
                return min;
            }
            return min + random.nextInt(max - min + 1);
        }

        public String format() {
            return min == max ? String.valueOf(min) : min + "-" + max;
        }
    }

    public record CustomMobOptions(Double movementSpeed,
                                   Double followRange,
                                   Double knockbackResistance,
                                   Double attackDamage,
                                   Double attackSpeed,
                                   boolean ai,
                                   boolean silent,
                                   boolean despawn) {
    }

    public static CustomMobDefinition fromConfig(String fallbackId, FileConfiguration cfg) {
        String id = cfg.getString("id", fallbackId);
        String typeToken = cfg.getString("type", "ZOMBIE");
        EntityType type;
        try {
            type = EntityType.valueOf(typeToken.toUpperCase());
        } catch (IllegalArgumentException ex) {
            type = EntityType.ZOMBIE;
        }
        String display = cfg.getString("display", id);
        LevelRange levelRange = parseLevelRange(cfg);
        Double health = cfg.contains("health") ? cfg.getDouble("health") : null;
        List<String> models = cfg.getStringList("models");
        if (models == null || models.isEmpty()) {
            String model = cfg.getString("model");
            models = model == null ? List.of() : List.of(model);
        }
        CustomMobStats stats = parseStats(cfg.getConfigurationSection("stats"));
        CustomMobOptions options = parseOptions(cfg.getConfigurationSection("options"));
        boolean boss = cfg.getBoolean("boss", false);
        return new CustomMobDefinition(
                id,
                type,
                ChatColor.translateAlternateColorCodes('&', display),
                levelRange,
                health,
                stats,
                models,
                options,
                boss
        );
    }

    private static LevelRange parseLevelRange(ConfigurationSection cfg) {
        if (cfg == null) {
            return new LevelRange(1, 1);
        }
        if (cfg.isConfigurationSection("level-range")) {
            ConfigurationSection range = cfg.getConfigurationSection("level-range");
            int min = range != null ? range.getInt("min", cfg.getInt("level", 1)) : cfg.getInt("level", 1);
            int max = range != null ? range.getInt("max", min) : min;
            return new LevelRange(min, max);
        }
        if (cfg.contains("level-min") || cfg.contains("level-max")) {
            int min = cfg.getInt("level-min", cfg.getInt("level", 1));
            int max = cfg.getInt("level-max", min);
            return new LevelRange(min, max);
        }
        String rangeText = cfg.getString("level-range");
        if (rangeText != null && rangeText.contains("-")) {
            String[] parts = rangeText.split("-", 2);
            try {
                int min = Integer.parseInt(parts[0].trim());
                int max = Integer.parseInt(parts[1].trim());
                return new LevelRange(min, max);
            } catch (NumberFormatException ignored) {
            }
        }
        int level = cfg.getInt("level", 1);
        return new LevelRange(level, level);
    }

    private static CustomMobStats parseStats(ConfigurationSection stats) {
        if (stats == null) {
            return CustomMobStats.empty();
        }
        return new CustomMobStats(
                stats.getInt("vitality", 0),
                stats.getInt("strength", 0),
                stats.getInt("agility", 0),
                stats.getInt("intelligence", 0),
                stats.getInt("dexterity", 0),
                stats.getInt("will", 0),
                stats.getInt("technique", 0)
        );
    }

    private static CustomMobOptions parseOptions(ConfigurationSection options) {
        if (options == null) {
            return new CustomMobOptions(null, null, null, null, null, true, false, false);
        }
        Double move = options.contains("movement-speed") ? options.getDouble("movement-speed") : null;
        Double follow = options.contains("follow-range") ? options.getDouble("follow-range") : null;
        Double knockback = options.contains("knockback-resistance") ? options.getDouble("knockback-resistance") : null;
        Double damage = options.contains("attack-damage") ? options.getDouble("attack-damage") : null;
        Double attackSpeed = options.contains("attack-speed") ? options.getDouble("attack-speed") : null;
        boolean ai = options.getBoolean("ai", true);
        boolean silent = options.getBoolean("silent", false);
        boolean despawn = options.getBoolean("despawn", false);
        return new CustomMobOptions(move, follow, knockback, damage, attackSpeed, ai, silent, despawn);
    }
}
