package me.nakilex.levelplugin.spells.gui;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class SpellGUIListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Check the inventory title to identify our GUI
        if (event.getView().getTitle().equals(ChatColor.DARK_GREEN + "Spell Book")) {
            // Prevent them from taking items
            event.setCancelled(true);
        }
    }
}
