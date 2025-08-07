package me.nakilex.levelplugin.cursormenu.display;

import org.bukkit.entity.Player;

import java.util.Set;

/**
 * Generic contract for managing temporary displays (items, text, etc.).
 * Implementations should be thread-safe as they may be manipulated from
 * async tasks.
 *
 * @param <ID> identifier type used to show specific displays
 */
public interface DisplayManager<ID> {
    void show(Player player, ID id);
    void hide(Player player);
    void reload();
    Set<ID> getAllIds();
    void cleanup(Player player);
}
