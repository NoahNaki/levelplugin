package me.nakilex.levelplugin.tips;

import me.nakilex.levelplugin.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TipsConfigManager {
    private final Main plugin;
    private final File configFile;
    private FileConfiguration config;
    private List<String> tips;
    private int delaySeconds;

    public TipsConfigManager(Main plugin) {
        this.plugin = plugin;
        this.tips = new ArrayList<>();
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
        this.config = YamlConfiguration.loadConfiguration(configFile);
    }

    /**
     * Load tips and delay from config.yml.
     */
    public void load() {
        config = YamlConfiguration.loadConfiguration(configFile);
        tips = config.getStringList("tips.messages");
        delaySeconds = config.getInt("tips.delay", 120);
        plugin.getLogger().info("[Tips] Loaded " + tips.size() + " tips, interval: " + delaySeconds + " seconds.");
    }

    public List<String> getTips() {
        return tips;
    }

    public int getDelaySeconds() {
        return delaySeconds;
    }
}