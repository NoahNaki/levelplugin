package me.nakilex.levelplugin.items.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class StaticItemListener implements Listener {

    private static final ItemStack STATIC_ITEM;

    static {
        // Initialize the static item
        STATIC_ITEM = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = STATIC_ITEM.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Stats Viewer");
            meta.setLore(java.util.Collections.singletonList(ChatColor.GRAY + "Right-click to view your stats."));
            STATIC_ITEM.setItemMeta(meta);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Set the static item in the last slot of the hotbar
        player.getInventory().setItem(8, STATIC_ITEM);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Prevent the static item from being moved
        ItemStack currentItem = event.getCurrentItem();
        if (currentItem != null && currentItem.isSimilar(STATIC_ITEM)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        // Prevent the static item from being dropped
        ItemStack droppedItem = event.getItemDrop().getItemStack();
        if (droppedItem.isSimilar(STATIC_ITEM)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Check if the player right-clicked with the static item
        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (itemInHand.isSimilar(STATIC_ITEM)) {
            // Execute the /stats command
            player.performCommand("stats");
            event.setCancelled(true);
        }
    }
}
