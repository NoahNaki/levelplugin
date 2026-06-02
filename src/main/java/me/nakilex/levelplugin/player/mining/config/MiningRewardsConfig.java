package me.nakilex.levelplugin.player.mining.config;

import me.nakilex.levelplugin.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.logging.Level;
import java.util.concurrent.ThreadLocalRandom;
import me.nakilex.levelplugin.player.mining.items.MiningNodeVariant;

/**
 * Loads mining_rewards.yml which defines XP, level requirements and drop ranges per ore type.
 */
public class MiningRewardsConfig {

    private File configFile;
    private FileConfiguration config;

    public MiningRewardsConfig(Main plugin) {
        configFile = new File(plugin.getDataFolder(), "mining_rewards.yml");
        if (!configFile.exists()) {
            try {
                plugin.saveResource("mining_rewards.yml", true);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save mining_rewards.yml!", e);
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public int getXP(String ore) {
        return config.getInt("ores." + ore + ".xp", 0);
    }

    public int getLevelRequirement(String ore) {
        return config.getInt("ores." + ore + ".level", 0);
    }

    public int getDropMin(String ore) {
        return config.getInt("ores." + ore + ".drop.min", 1);
    }

    public int getDropMax(String ore) {
        return Math.max(getDropMin(ore), config.getInt("ores." + ore + ".drop.max", getDropMin(ore)));
    }


    public MiningNodeVariant rollNodeVariant() {
        if (ThreadLocalRandom.current().nextDouble() >= config.getDouble("nodes.special-chance", 0.18)) {
            return MiningNodeVariant.NORMAL;
        }
        double totalWeight = 0.0;
        for (MiningNodeVariant variant : MiningNodeVariant.values()) {
            if (variant.isSpecial()) totalWeight += getVariantValue(variant, "weight", 1.0);
        }
        double roll = ThreadLocalRandom.current().nextDouble() * Math.max(0.01, totalWeight);
        for (MiningNodeVariant variant : MiningNodeVariant.values()) {
            if (!variant.isSpecial()) continue;
            roll -= getVariantValue(variant, "weight", 1.0);
            if (roll <= 0.0) return variant;
        }
        return MiningNodeVariant.NORMAL;
    }

    public double getHealthMultiplier(MiningNodeVariant variant) {
        return getVariantValue(variant, "health-multiplier", 1.0);
    }

    public double getDropMultiplier(MiningNodeVariant variant) {
        return getVariantValue(variant, "drop-multiplier", 1.0);
    }

    public double getXpMultiplier(MiningNodeVariant variant) {
        return getVariantValue(variant, "xp-multiplier", 1.0);
    }

    public double getWeakPointChance(MiningNodeVariant variant) {
        return getVariantValue(variant, "weak-point-chance", 0.22);
    }

    public double getWeakPointDamageMultiplier(MiningNodeVariant variant) {
        return getVariantValue(variant, "weak-point-damage-multiplier", 2.5);
    }

    private double getVariantValue(MiningNodeVariant variant, String field, double fallback) {
        MiningNodeVariant safeVariant = variant == null ? MiningNodeVariant.NORMAL : variant;
        return config.getDouble("nodes.variants." + safeVariant.getKey() + "." + field, fallback);
    }

    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
    }
}
