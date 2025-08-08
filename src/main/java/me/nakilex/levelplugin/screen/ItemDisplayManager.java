package me.nakilex.levelplugin.screen;

import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Manager for spawning item based displays for players.
 */
public class ItemDisplayManager extends AbstractDisplayManager<ItemDisplay> {

    /**
     * Show an {@link ItemDisplay} to the player with the provided item stack.
     *
     * @param player viewer
     * @param location spawn location
     * @param item item to display
     * @return spawned display
     */
    public ItemDisplay show(Player player, Location location, ItemStack item) {
        hide(player);
        ItemDisplay display = DisplayUtil.spawn(location, ItemDisplay.class, id -> {
            id.setItemStack(item);
            id.setBillboard(Display.Billboard.CENTER);
        });
        activeDisplays.put(player.getUniqueId(), display);
        return display;
    }
}
