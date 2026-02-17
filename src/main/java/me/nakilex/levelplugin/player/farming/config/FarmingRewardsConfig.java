package me.nakilex.levelplugin.player.farming.config;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.farming.data.FarmingCrop;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Locale;
import java.util.logging.Level;

/** Loads farming_rewards.yml for crop requirements and xp rewards. */
public class FarmingRewardsConfig {
    private final File configFile;
    private FileConfiguration config;

    public FarmingRewardsConfig(Main plugin) {
        configFile = new File(plugin.getDataFolder(), "farming_rewards.yml");
        if (!configFile.exists()) {
            try {
                plugin.saveResource("farming_rewards.yml", true);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save farming_rewards.yml!", e);
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    private String path(FarmingCrop crop) {
        return "crops." + crop.name().toLowerCase(Locale.ROOT);
    }

    public int getLevelRequirement(FarmingCrop crop) {
        return config.getInt(path(crop) + ".level", crop.getLevelRequirement());
    }

    public int getXpReward(FarmingCrop crop) {
        return config.getInt(path(crop) + ".xp", crop.getXpReward());
    }

    public String getQuestId(FarmingCrop crop) {
        return config.getString(path(crop) + ".quest", crop.getQuestId());
    }

    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
    }
}
