package me.nakilex.levelplugin.storage.listeners;

import me.nakilex.levelplugin.storage.gui.StorageGUI;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;


public class StorageEvents implements Listener {

    private static StorageEvents instance;

    private final Map<Inventory, StorageGUI> guiMap = new HashMap<>();

    public StorageEvents() {
        instance = this;
    }
    // Register a GUI when opened
    public void registerGUI(Inventory inventory, StorageGUI gui) {
        guiMap.put(inventory, gui);
    }

    public static StorageEvents getInstance() {
        return instance;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // DEBUG: Log the clicked inventory name
        System.out.println("DEBUG: Click detected in inventory - " + event.getView().getTitle());

        Inventory inventory = event.getInventory();
        if (guiMap.containsKey(inventory)) {
            System.out.println("DEBUG: Matched inventory to GUI.");
            guiMap.get(inventory).handleClick(event); // Forward the event
        } else {
            System.out.println("DEBUG: Inventory not registered.");
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        if (guiMap.containsKey(inventory)) {
            guiMap.get(inventory).handleClose(event);
            guiMap.remove(inventory); // Clean up after closing
        }
    }
}
