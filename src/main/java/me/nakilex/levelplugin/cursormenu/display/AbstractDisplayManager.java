package me.nakilex.levelplugin.cursormenu.display;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic manager that keeps track of per-player display entities.
 */
public abstract class AbstractDisplayManager<T extends Entity> {
    protected final Map<UUID, T> activeDisplays = new ConcurrentHashMap<>();

    /** Show the display to the player. */
    public abstract void show(Player player, Object data);

    /** Hide and remove any active display for the player. */
    public void hide(Player player) {
        T entity = activeDisplays.remove(player.getUniqueId());
        if (entity != null && !entity.isDead()) {
            entity.remove();
        }
    }

    public void hideAll() {
        activeDisplays.values().forEach(e -> { if (!e.isDead()) e.remove(); });
        activeDisplays.clear();
    }
}
