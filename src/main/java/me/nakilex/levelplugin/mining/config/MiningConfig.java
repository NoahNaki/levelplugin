package me.nakilex.levelplugin.mining.config;

import me.nakilex.levelplugin.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class MiningConfig {
    private final File configFile;
    private FileConfiguration config;

    public MiningConfig(Main plugin) {
        this.configFile = new File(plugin.getDataFolder(), "mining_ores.yml");
        if (!configFile.exists()) {
            try {
                plugin.saveResource("mining_ores.yml", false);
            } catch (Exception e) {
                try { configFile.createNewFile(); } catch (IOException ignored) {}
            }
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public void reload() {
        this.config = YamlConfiguration.loadConfiguration(configFile);
    }
}
