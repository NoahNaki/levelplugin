package me.nakilex.levelplugin.storage.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import me.nakilex.levelplugin.storage.gui.StorageGUI;

import java.util.HashMap;
import java.util.Map;

public class StorageEvents implements Listener {

    private final Map<Inventory, StorageGUI> storageGUIs = new HashMap<>();

    public void registerGUI(Inventory inventory, StorageGUI gui) {
        storageGUIs.put(inventory, gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (storageGUIs.containsKey(event.getInventory())) {
            storageGUIs.get(event.getInventory()).handleClick(event);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (storageGUIs.containsKey(event.getInventory())) {
            storageGUIs.get(event.getInventory()).handleClose(event);
            storageGUIs.remove(event.getInventory()); // Remove after closing
        }
    }
}
