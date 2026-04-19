package me.nakilex.levelplugin.settings.environment;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public final class PersonalEnvironmentKeys {
    private PersonalEnvironmentKeys() {
    }

    public static NamespacedKey PLAYER_WEATHER;
    public static NamespacedKey PLAYER_TIME;

    public static void init(JavaPlugin plugin) {
        PLAYER_WEATHER = new NamespacedKey(plugin, "player_weather");
        PLAYER_TIME = new NamespacedKey(plugin, "player_time");
    }
}
