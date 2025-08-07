package me.nakilex.levelplugin.cursormenu.display;

import me.nakilex.levelplugin.cursormenu.util.ConfigUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles animated text displays. This base implementation only tracks which
 * display IDs are active for a player; concrete animation logic can be added by
 * extending classes.
 */
public class TextDisplayManager implements DisplayManager<String>, Listener {
    private final JavaPlugin plugin;
    private final Map<UUID, Set<String>> active = new ConcurrentHashMap<>();
    private FileConfiguration config;

    public TextDisplayManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.config = ConfigUtils.loadConfig(plugin, "cursormenu/text.yml");
    }

    @Override
    public void show(Player player, String id) {
        active.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet()).add(id);
    }

    @Override
    public void hide(Player player) {
        active.remove(player.getUniqueId());
    }

    @Override
    public void reload() {
        this.config = ConfigUtils.loadConfig(plugin, "cursormenu/text.yml");
    }

    @Override
    public Set<String> getAllIds() {
        return config.getKeys(false);
    }

    @Override
    public void cleanup(Player player) {
        hide(player);
    }

    public Set<String> getPlayerActiveText(Player player) {
        return active.getOrDefault(player.getUniqueId(), Collections.emptySet());
    }
}
