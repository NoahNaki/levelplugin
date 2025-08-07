package me.nakilex.levelplugin.cursormenu.display;

import me.nakilex.levelplugin.cursormenu.util.ConfigUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simplified manager for item displays. In this generic implementation we only
 * track which display ID is active for a player. Actual entity spawning can be
 * added by plugins extending this project.
 */
public class ItemDisplayManager implements DisplayManager<String>, Listener {
    private final JavaPlugin plugin;
    private final Map<UUID, String> active = new ConcurrentHashMap<>();
    private FileConfiguration config;

    public ItemDisplayManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.config = ConfigUtils.loadConfig(plugin, "cursormenu/items.yml");
    }

    @Override
    public void show(Player player, String id) {
        active.put(player.getUniqueId(), id);
        // Spawning logic omitted; this base class focuses on bookkeeping
    }

    @Override
    public void hide(Player player) {
        active.remove(player.getUniqueId());
    }

    @Override
    public void reload() {
        this.config = ConfigUtils.loadConfig(plugin, "cursormenu/items.yml");
    }

    @Override
    public Set<String> getAllIds() {
        return config.getKeys(false);
    }

    @Override
    public void cleanup(Player player) {
        hide(player);
    }

    public String getPlayerActiveItemId(Player player) {
        return active.get(player.getUniqueId());
    }
}
