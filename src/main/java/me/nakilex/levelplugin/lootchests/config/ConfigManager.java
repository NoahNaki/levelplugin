package me.nakilex.levelplugin.lootchests.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class ConfigManager {

    private final JavaPlugin plugin;

    // The FileConfiguration for lootchests.yml
    private FileConfiguration lootChestsConfig;
    // The actual file on disk
    private File lootChestsFile;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        setupLootChestsConfig();
    }

    /**
     * Creates the lootchests.yml file if it doesn't exist,
     * then loads it into lootChestsConfig.
     */
    private void setupLootChestsConfig() {
        lootChestsFile = new File(plugin.getDataFolder(), "lootchests.yml");
        if (!lootChestsFile.exists()) {
            // If file doesn't exist, save the default from resources
            plugin.saveResource("lootchests.yml", false);
        }
        // Load the file into a FileConfiguration for easy access
        lootChestsConfig = YamlConfiguration.loadConfiguration(lootChestsFile);
    }

    /**
     * Returns the FileConfiguration object for lootchests.yml.
     */
    public FileConfiguration getLootChestsConfig() {
        return lootChestsConfig;
    }

    /**
     * Saves any changes made to lootChestsConfig back to lootchests.yml.
     */
    public void saveLootChestsConfig() {
        try {
            lootChestsConfig.save(lootChestsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save lootchests.yml!");
            e.printStackTrace();
        }
    }

    /**
     * Reloads the lootchests.yml from disk, discarding unsaved changes.
     */
    public void reloadLootChestsConfig() {
        lootChestsConfig = YamlConfiguration.loadConfiguration(lootChestsFile);
    }
}
