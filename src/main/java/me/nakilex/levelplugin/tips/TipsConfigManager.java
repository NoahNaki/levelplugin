package me.nakilex.levelplugin.tips;

import me.nakilex.levelplugin.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TipsConfigManager {
    private final Main plugin;
    private File configFile;
    private FileConfiguration config;
    private List<String> tips;
    private int delaySeconds;

    public TipsConfigManager(Main plugin) {
        this.plugin = plugin;
        this.tips = new ArrayList<>();
        setupConfig();
    }

    private void setupConfig() {
        configFile = new File(plugin.getDataFolder(), "tips.yml");
        if (!configFile.exists()) {
            plugin.saveResource("tips.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    /**
     * Load tips and delay from the YAML file, with a debug log.
     */
    public void load() {
        tips = config.getStringList("tips");
        delaySeconds = config.getInt("delay", 120);
        plugin.getLogger().info("[Tips] Loaded " + tips.size() + " tips, interval: " + delaySeconds + " seconds.");
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().warning("[Tips] Could not save tips.yml: " + e.getMessage());
        }
    }

    public List<String> getTips() {
        return tips;
    }

    public int getDelaySeconds() {
        return delaySeconds;
    }
}