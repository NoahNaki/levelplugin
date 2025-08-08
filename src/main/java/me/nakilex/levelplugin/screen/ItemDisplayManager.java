package me.nakilex.levelplugin.screen;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Manager for spawning item displays at runtime.
 */
public class ItemDisplayManager extends DisplayManager<ItemDisplay> {

    public ItemDisplay show(Player player, Location location, ItemStack stack) {
        ItemDisplay display = (ItemDisplay) location.getWorld().spawnEntity(location, EntityType.ITEM_DISPLAY);
        display.setItemStack(stack);
        track(player, display);
        return display;
    }
}
