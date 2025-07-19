package me.nakilex.levelplugin.tips;

import me.nakilex.levelplugin.Main;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

public class TipsConfigManager {
    private final Main plugin;
    private FileConfiguration config;
    private List<String> tips;
    private int delaySeconds;

    public TipsConfigManager(Main plugin) {
        this.plugin = plugin;
        this.tips = new ArrayList<>();
        this.config = plugin.getCustomConfig();
    }

    /**
     * Load tips and delay from the configuration.
     */
    public void load() {
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