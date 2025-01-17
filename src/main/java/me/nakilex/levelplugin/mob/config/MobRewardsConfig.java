package me.nakilex.levelplugin.mob.config;

import me.nakilex.levelplugin.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class MobRewardsConfig {

    private File configFile;
    private FileConfiguration config;

    public MobRewardsConfig(Main plugin) {
        configFile = new File(plugin.getDataFolder(), "mob_rewards.yml");

        if (!configFile.exists()) {
            try {
                plugin.saveResource("mob_rewards.yml", true);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save mob_rewards.yml!", e);
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
