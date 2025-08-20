package me.nakilex.levelplugin.guild.quests;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * Simple click handler for the guild quest menu to prevent players from
 * moving items out of the GUI. More advanced interactions (e.g. rerolling
 * quests) can be layered on top later.
 */
public class GuildQuestGUIListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GuildQuestGUI.TITLE)) return;
        if (event.getClickedInventory() == event.getView().getTopInventory()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!event.getView().getTitle().equals(GuildQuestGUI.TITLE)) return;
        event.setCancelled(true);
    }
}
