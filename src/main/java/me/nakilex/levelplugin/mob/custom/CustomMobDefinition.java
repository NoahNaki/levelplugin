package me.nakilex.levelplugin.mob.custom;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;

import java.util.List;

public record CustomMobDefinition(String id,
                                  EntityType entityType,
                                  String displayName,
                                  LevelRange levelRange,
                                  Double baseHealth,
                                  CustomMobStats stats,
                                  List<String> models,
                                  List<CustomMobSpell> spells,
                                  CustomMobOptions options,
                                  boolean boss) {

    public record CustomMobSpell(String id,
                                 int intervalTicks,
                                 double damage,
                                 double range,
                                 double speed,
                                 int burnTicks) {
    }

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

    public record CustomMobAttributes(double maxHealth,
                                      double movementSpeed,
                                      Double attackDamage,
                                      Double attackSpeed,
                                      Double followRange,
                                      Double knockbackResistance) {
    }

    public CustomMobAttributes computeAttributes() {
        CustomMobStats stats = stats();
        CustomMobOptions opts = options();
        if (stats == null) {
            stats = CustomMobStats.empty();
        }
        if (opts == null) {
            opts = new CustomMobOptions(null, null, null, null, null, true, false, false);
        }
        double baseHealthValue = baseHealth != null ? baseHealth : StatsManager.BASE_HEALTH;
        double maxHealthValue = stats.computeMaxHealth(baseHealthValue);
        double moveSpeedValue = opts.movementSpeed() != null
                ? opts.movementSpeed()
                : 0.2 + stats.agility() * 0.002;
        Double attackDamageValue = opts.attackDamage() != null
                ? opts.attackDamage()
                : (stats.strength() > 0 ? 1.0 + stats.strength() * 0.5 : null);
        Double attackSpeedValue = opts.attackSpeed() != null
                ? opts.attackSpeed()
                : (stats.technique() > 0 ? 0.5 * (1.0 + 0.0075 * stats.technique()) * 8.0 : null);
        return new CustomMobAttributes(
                maxHealthValue,
                moveSpeedValue,
                attackDamageValue,
                attackSpeedValue,
                opts.followRange(),
                opts.knockbackResistance()
        );
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
        List<CustomMobSpell> spells = parseSpells(cfg.getConfigurationSection("spells"));
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
                spells,
                options,
                boss
        );
    }

    private static List<CustomMobSpell> parseSpells(ConfigurationSection cfg) {
        if (cfg == null) {
            return List.of();
        }
        List<CustomMobSpell> spells = new java.util.ArrayList<>();
        for (String key : cfg.getKeys(false)) {
            ConfigurationSection node = cfg.getConfigurationSection(key);
            if (node == null) {
                continue;
            }
            String id = node.getString("id", key).trim().toLowerCase(java.util.Locale.ROOT);
            int intervalTicks = Math.max(1, node.getInt("interval-ticks", 40));
            double damage = Math.max(0.0, node.getDouble("damage", 4.0));
            double range = Math.max(1.0, node.getDouble("range", 20.0));
            double speed = Math.max(0.1, node.getDouble("speed", 0.9));
            int burnTicks = Math.max(0, node.getInt("burn-ticks", 0));
            spells.add(new CustomMobSpell(id, intervalTicks, damage, range, speed, burnTicks));
        }
        return spells;
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
