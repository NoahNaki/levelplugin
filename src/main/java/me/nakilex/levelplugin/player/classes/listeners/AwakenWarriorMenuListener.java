package me.nakilex.levelplugin.player.classes.listeners;

import me.nakilex.levelplugin.player.classes.gui.AwakenWarriorMenu;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class AwakenWarriorMenuListener implements Listener {
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = ChatColor.stripColor(event.getView().getTitle());
        if (!"Warrior Awakening".equalsIgnoreCase(title)) return;
        event.setCancelled(true);
        AwakenWarriorMenu.handleSelect(player, event.getCurrentItem());
    }
}
