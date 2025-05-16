package me.nakilex.levelplugin.utils;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.InventoryView;

public class FullInventoryListener implements Listener {

    private static final String SALVAGE_TITLE = "Merchant";

    @EventHandler
    public void onEntityPickup(EntityPickupItemEvent event) {
        // Only handle when the entity is a player
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        // 1) If salvage GUI is open, cancel pickup without notification
        InventoryView openView = player.getOpenInventory();
        if (openView != null &&
            ChatColor.stripColor(openView.getTitle()).equalsIgnoreCase(SALVAGE_TITLE)) {
            event.setCancelled(true);
            return;
        }

        // 2) If inventory is full, cancel pickup and notify
        if (player.getInventory().firstEmpty() == -1) {
            event.setCancelled(true);
            sendFullInventoryTitle(player);
        }
        // else: let the pickup proceed normally
    }

    /**
     * Sends a big red "Inventory full!" title to the player.
     */
    private void sendFullInventoryTitle(Player player) {
        String title    = ChatColor.RED + "Inventory full!";
        String subtitle = "";
        int fadeIn  = 10;  // ticks (0.5s)
        int stay    = 70;  // ticks (3.5s)
        int fadeOut = 20;  // ticks (1s)

        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }
}
