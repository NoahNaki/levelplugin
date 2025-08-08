package me.nakilex.levelplugin.screen;

import org.bukkit.entity.Display;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base manager keeping track of spawned {@link Display} instances per player.
 * Subclasses are expected to provide methods to show specific display types.
 */
public abstract class AbstractDisplayManager<T extends Display> {

    protected final Map<UUID, T> activeDisplays = new ConcurrentHashMap<>();

    /**
     * Remove and hide any display currently shown to the player.
     *
     * @param player viewer whose display should be removed
     */
    public void hide(Player player) {
        T display = activeDisplays.remove(player.getUniqueId());
        if (display != null && !display.isDead()) {
            display.remove();
        }
    }

    /**
     * Remove all tracked displays. Typically called on plugin disable.
     */
    public void cleanup() {
        activeDisplays.values().forEach(d -> {
            if (d != null && !d.isDead()) {
                d.remove();
            }
        });
        activeDisplays.clear();
    }
}
