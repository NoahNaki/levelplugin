package me.nakilex.levelplugin.utils;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.inventory.InventoryView;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FullInventoryListener implements Listener {

    // Title of the salvage GUI, stripped of color codes
    private static final String SALVAGE_TITLE = "Salvage Items";
    private final Map<UUID, Long> lastAlert = new HashMap<>();

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

    @EventHandler
    public void onAttemptPickup(PlayerAttemptPickupItemEvent event) {
        Player player = event.getPlayer();

        InventoryView openView = player.getOpenInventory();
        if (openView != null &&
            ChatColor.stripColor(openView.getTitle()).equalsIgnoreCase(SALVAGE_TITLE)) {
            event.setCancelled(true);
            return;
        }

        if (player.getInventory().firstEmpty() == -1) {
            event.setCancelled(true);
            sendFullInventoryTitle(player);
        }
    }

    /**
     * Sends a big red "Inventory full!" title to the player.
     */
    private void sendFullInventoryTitle(Player player) {
        long now = System.currentTimeMillis();
        UUID id = player.getUniqueId();
        Long last = lastAlert.get(id);
        if (last != null && now - last < 10_000) {
            return;
        }
        lastAlert.put(id, now);
        String title    = ChatColor.RED + "Inventory full!";
        String subtitle = "Visit a nearby salvager to scrap your items!";
        int fadeIn  = 10;  // ticks (0.5s)
        int stay    = 70;  // ticks (3.5s)
        int fadeOut = 20;  // ticks (1s)

        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }
}
