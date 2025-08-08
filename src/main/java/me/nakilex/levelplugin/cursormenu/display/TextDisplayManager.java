package me.nakilex.levelplugin.cursormenu.display;

import org.bukkit.entity.Player;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Basic text display manager providing a small API surface for showing and
 * removing animated text displays in front of players. The actual animation
 * logic is expected to be implemented externally. This class mainly manages the
 * association between players and active text ids.
 */
public class TextDisplayManager implements DisplayManager<String> {

    private final ConcurrentMap<Player, String> activeTexts = new ConcurrentHashMap<>();

    @Override
    public void show(Player player, String id) {
        activeTexts.put(player, id);
    }

    @Override
    public void hide(Player player) {
        activeTexts.remove(player);
    }

    @Override
    public void reload() {
        // placeholder for configuration reloading
    }

    @Override
    public Set<String> getAllIds() {
        return Set.copyOf(activeTexts.values());
    }

    /**
     * Get the id of the text currently displayed to the player.
     *
     * @param player target player
     * @return id or null if none
     */
    public String getPlayerActiveTextId(Player player) {
        return activeTexts.get(player);
    }
}
