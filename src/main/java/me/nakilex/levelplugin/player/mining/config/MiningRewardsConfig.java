package me.nakilex.levelplugin.player.mining.config;

import me.nakilex.levelplugin.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.logging.Level;

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

    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
    }
}
