package me.nakilex.levelplugin.cursormenu.display;

import org.bukkit.entity.Player;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Simple item display manager. This implementation only tracks which item id is
 * currently visible for a player. Actual entity spawning and animation should be
 * implemented by the server using this plugin.
 */
public class ItemDisplayManager implements DisplayManager<String> {

    private final ConcurrentMap<Player, String> activeItems = new ConcurrentHashMap<>();

    @Override
    public void show(Player player, String id) {
        activeItems.put(player, id);
    }

    @Override
    public void hide(Player player) {
        activeItems.remove(player);
    }

    @Override
    public void reload() {
        // placeholder for configuration reloading
    }

    @Override
    public Set<String> getAllIds() {
        return Set.copyOf(activeItems.values());
    }

    /**
     * Retrieve the item id currently displayed to a player.
     *
     * @param player target player
     * @return id or null if none shown
     */
    public String getPlayerActiveItemId(Player player) {
        return activeItems.get(player);
    }
}
