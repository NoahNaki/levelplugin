package me.nakilex.levelplugin.screen;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * High level facade combining text and item displays to present a simple
 * cursor driven menu in front of a player. The implementation is intentionally
 * lightweight and focuses on reusability.
 */
public class CursorMenuManager {

    private final TextDisplayManager textManager = new TextDisplayManager();
    private final ItemDisplayManager itemManager = new ItemDisplayManager();

    /**
     * Show a menu entry comprised of an item icon with an optional caption.
     *
     * @param player viewer
     * @param item icon to display
     * @param caption optional text caption, may be null
     * @param location base location for the displays
     */
    public void open(Player player, ItemStack item, String caption, Location location) {
        itemManager.show(player, location, item);
        if (caption != null && !caption.isEmpty()) {
            // spawn text slightly above the item
            textManager.show(player, location.clone().add(0, 0.4, 0), caption);
        } else {
            textManager.hide(player);
        }
    }

    /**
     * Remove any displays currently shown for the player.
     */
    public void close(Player player) {
        itemManager.hide(player);
        textManager.hide(player);
    }

    /**
     * Cleanup any remaining displays, e.g. on plugin disable.
     */
    public void cleanup() {
        itemManager.cleanup();
        textManager.cleanup();
    }
}
