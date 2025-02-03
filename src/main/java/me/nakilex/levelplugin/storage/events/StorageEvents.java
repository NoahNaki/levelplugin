package me.nakilex.levelplugin.storage.events;

import me.nakilex.levelplugin.storage.gui.StorageGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;

/**
 * Listens for inventory interactions and delegates them to
 * the appropriate StorageGUI instance if it’s a player’s
 * personal storage.
 */
public class StorageEvents implements Listener {

    /**
     * Tracks which Inventory objects belong to which StorageGUI instance.
     */
    private final Map<Inventory, StorageGUI> trackedInventories = new HashMap<>();

    /**
     * Registers a StorageGUI’s Inventory so we can handle clicks.
     */
    public void registerInventory(StorageGUI storageGUI, Inventory inventory) {
        trackedInventories.put(inventory, storageGUI);
    }

    /**
     * Unregisters an inventory when it’s closed or no longer needed.
     */
    public void unregisterInventory(Inventory inventory) {
        trackedInventories.remove(inventory);
    }

    /**
     * Listen for clicks inside any tracked inventory.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory clickedInventory = event.getInventory();
        if (trackedInventories.containsKey(clickedInventory)) {
            // Cancel if it's a navigation slot, or pass the event to the GUI
            StorageGUI gui = trackedInventories.get(clickedInventory);
            gui.handleClick(event);
        }
    }

    /**
     * Listen for the moment an inventory is closed; trigger saving logic.
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory closedInventory = event.getInventory();
        if (trackedInventories.containsKey(closedInventory)) {
            HumanEntity human = event.getPlayer();
            if (human instanceof Player) {
                Player player = (Player) human;

                // Optionally save the player's storage on close
                StorageGUI gui = trackedInventories.get(closedInventory);
                gui.saveToDisk(); // Or pass to PersonalStorage for saving

                // Unregister this inventory to free up references
                unregisterInventory(closedInventory);
            }
        }
    }
}
