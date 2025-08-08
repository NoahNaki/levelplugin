package me.nakilex.levelplugin.cursormenu.display;

import org.bukkit.entity.Player;

import java.util.Set;

/**
 * Generic display manager used to handle different types of on screen displays
 * such as item displays or text displays. Implementations should be thread safe
 * when dealing with player collections as these may be modified from async tasks.
 *
 * @param <T> identifier type for a display
 */
public interface DisplayManager<T> {

    /**
     * Show a display for the given player using the provided id.
     *
     * @param player target player
     * @param id display identifier
     */
    void show(Player player, T id);

    /**
     * Hide all displays currently active for the provided player.
     *
     * @param player target player
     */
    void hide(Player player);

    /**
     * Reload any configuration or cached data used by this manager.
     */
    void reload();

    /**
     * Fetch all registered display identifiers.
     *
     * @return immutable set of all ids
     */
    Set<T> getAllIds();
}
