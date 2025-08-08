package me.nakilex.levelplugin.cursormenu.display;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Simplified manager for spawning per-player {@link ItemDisplay} entities that
 * follow the player's view direction. Rotation/animation tasks can be layered
 * on top by external code if desired.
 */
public class ItemDisplayManager extends AbstractDisplayManager<ItemDisplay> {

    @Override
    public void show(Player player, Object data) {
        if (!(data instanceof ItemStack item)) return;
        hide(player);
        Location loc = player.getEyeLocation().add(player.getLocation().getDirection().normalize().multiply(2));
        ItemDisplay display = (ItemDisplay) player.getWorld().spawnEntity(loc, EntityType.ITEM_DISPLAY);
        display.setItemStack(item);
        activeDisplays.put(player.getUniqueId(), display);
    }
}
