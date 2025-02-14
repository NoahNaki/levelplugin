package me.nakilex.levelplugin.utils;

import me.nakilex.levelplugin.items.utils.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;

public class InventoryOpenCustomItemListener implements Listener {

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        // Make sure the event is triggered by a player.
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getPlayer();

        // Check if the opened inventory is the player's own inventory.
        if (event.getInventory().equals(player.getInventory())) {
            Bukkit.getLogger().info("[CustomItem] " + player.getName() + " opened their inventory. Updating custom item tooltips.");

            // Loop through all items in the player's inventory.
            for (ItemStack item : player.getInventory().getContents()) {
                if (item == null || !item.hasItemMeta()) {
                    continue;
                }
                // Check if this is a custom item (using your persistent data key).
                if (ItemUtil.getCustomItemId(item) != -1) {
                    Bukkit.getLogger().info("[CustomItem] Updating tooltip for item: " + item.getItemMeta().getDisplayName());
                    ItemUtil.updateCustomItemTooltip(item, player);
                }
            }
            // Force the inventory update so the client sees the changes.
            player.updateInventory();
        }
    }
}
