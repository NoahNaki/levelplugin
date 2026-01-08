package me.nakilex.levelplugin.mob.custom;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;

import java.util.List;

public record CustomMobDefinition(String id,
                                  EntityType entityType,
                                  String displayName,
                                  int level,
                                  Double baseHealth,
                                  CustomMobStats stats,
                                  List<String> models,
                                  CustomMobOptions options,
                                  boolean boss) {

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
        int level = cfg.getInt("level", 1);
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
                level,
                health,
                stats,
                models,
                options,
                boss
        );
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
