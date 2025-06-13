package me.nakilex.levelplugin.player.mining.config;

import me.nakilex.levelplugin.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.logging.Level;

/**
 * Loads mining_rewards.yml which defines XP per ore type.
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

    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
    }
}
