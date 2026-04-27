package me.nakilex.levelplugin.utils;

import me.nakilex.levelplugin.Main;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Central helper for runtime feature flags stored in custom config.
 */
public final class FeatureFlagUtil {
    private FeatureFlagUtil() {
    }

    public static boolean isEnabled(String path, boolean defaultValue) {
        Main plugin = Main.getInstance();
        if (plugin == null) {
            return defaultValue;
        }
        FileConfiguration config = plugin.getCustomConfig();
        if (config == null) {
            return defaultValue;
        }
        return config.getBoolean(path, defaultValue);
    }
}
