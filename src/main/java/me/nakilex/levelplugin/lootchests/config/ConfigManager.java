package me.nakilex.levelplugin.lootchests.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration lootChestsConfig;
    private File lootChestsFile;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        setupLootChestsConfig();
    }

    private void setupLootChestsConfig() {
        lootChestsFile = new File(plugin.getDataFolder(), "lootchests.yml");
        if (!lootChestsFile.exists()) {
            plugin.saveResource("lootchests.yml", false);
        }
        lootChestsConfig = YamlConfiguration.loadConfiguration(lootChestsFile);
    }

    public FileConfiguration getLootChestsConfig() {
        return lootChestsConfig;
    }

    public void saveLootChestsConfig() {
        try {
            lootChestsConfig.save(lootChestsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save lootchests.yml!");
            e.printStackTrace();
        }
    }

    public void reloadLootChestsConfig() {
        lootChestsConfig = YamlConfiguration.loadConfiguration(lootChestsFile);
    }
}
