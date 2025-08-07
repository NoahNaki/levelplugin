package me.nakilex.levelplugin.cursormenu.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/**
 * Utility for loading YAML configuration files with default merging.
 * Designed for reuse across managers that require simple file configs.
 */
public final class ConfigUtils {

    private ConfigUtils() {}

    /**
     * Loads a configuration file from the plugin data folder, copying the
     * default from the jar if necessary. Missing values are merged from the
     * default resource to keep configs up to date on reload.
     *
     * @param plugin plugin instance
     * @param path   relative resource path inside the jar
     * @return loaded configuration
     */
    public static FileConfiguration loadConfig(JavaPlugin plugin, String path) {
        File file = new File(plugin.getDataFolder(), path);
        if (!file.exists()) {
            plugin.saveResource(path, false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        // merge defaults
        try (Reader reader = new InputStreamReader(plugin.getResource(path), StandardCharsets.UTF_8)) {
            YamlConfiguration def = YamlConfiguration.loadConfiguration(reader);
            config.setDefaults(def);
            config.options().copyDefaults(true);
            config.save(file);
        } catch (Exception ignored) {
        }
        return config;
    }
}
