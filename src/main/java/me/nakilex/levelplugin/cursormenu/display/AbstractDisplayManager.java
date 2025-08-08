package me.nakilex.levelplugin.cursormenu.display;

import org.bukkit.Bukkit;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic base manager for display entities tracked per-player.
 * Child managers only need to implement spawn logic.
 */
public abstract class AbstractDisplayManager<T extends Display> implements Listener {
    protected final JavaPlugin plugin;
    protected final Map<UUID, T> playerDisplays = new ConcurrentHashMap<>();

    protected AbstractDisplayManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public T getDisplay(Player player) {
        return playerDisplays.get(player.getUniqueId());
    }

    public void hide(Player player) {
        T display = playerDisplays.remove(player.getUniqueId());
        if (display != null) {
            display.remove();
        }
    }

    public void cleanup() {
        for (UUID id : playerDisplays.keySet()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) hide(p);
        }
    }
}
