package me.nakilex.levelplugin.storage.data;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

/**
 * Loads and provides global plugin settings such as page cost,
 * max pages, etc. from config.yml.
 */
public class StorageConfig {

    private static StorageConfig instance;
    private final Plugin plugin;
    private double pageCost;
    private int maxPages;

    /**
     * Private constructor to enforce singleton usage.
     */
    private StorageConfig(Plugin plugin) {
        this.plugin = plugin;
        reloadConfig();
    }

    /**
     * Initialize the StorageConfig singleton. Call this once in your
     * main plugin class (onEnable) or wherever you handle initialization.
     */
    public static void init(Plugin plugin) {
        if (instance == null) {
            instance = new StorageConfig(plugin);
        }
    }

    /**
     * Retrieves the instance of this config manager.
     */
    public static StorageConfig getInstance() {
        return instance;
    }

    /**
     * Loads values from the plugin’s config.yml.
     */
    public void reloadConfig() {
        plugin.saveDefaultConfig();
        FileConfiguration config = plugin.getConfig();

        // Load your desired settings here. If they don't exist, you can set defaults.
        this.pageCost = config.getDouble("storage.page-cost", 100.0);
        this.maxPages = config.getInt("storage.max-pages", 5);

        // Save back to config if needed
        plugin.saveConfig();
    }

    public double getPageCost() {
        return pageCost;
    }

    public int getMaxPages() {
        return maxPages;
    }
}
