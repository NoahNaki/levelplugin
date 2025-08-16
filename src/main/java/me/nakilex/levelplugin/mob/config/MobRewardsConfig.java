package me.nakilex.levelplugin.mob.config;

import me.nakilex.levelplugin.Main;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
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

    /**
     * Retrieve the rewards section for a MythicMob ID, ignoring case.
     *
     * @param mobType the MythicMob identifier
     * @return configuration section for that mob or {@code null} if not found
     */
    public ConfigurationSection getMobSection(String mobType) {
        ConfigurationSection mobs = config.getConfigurationSection("mobs");
        if (mobs == null) {
            return null;
        }
        for (String key : mobs.getKeys(false)) {
            if (key.equalsIgnoreCase(mobType)) {
                return mobs.getConfigurationSection(key);
            }
        }
        return null;
    }
}
