package me.nakilex.levelplugin.storage;

import me.nakilex.levelplugin.storage.gui.StorageGUI;
import me.nakilex.levelplugin.storage.listeners.StorageEvents;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class PersonalStorage {

    private final Player owner;
    private final StorageGUI gui;

    public PersonalStorage(Player owner) {
        this.owner = owner;
        this.gui = new StorageGUI(owner); // Use StorageGUI for multi-page support
    }

    // Open the GUI for the player
    public void open(Player player) {
        StorageGUI gui = new StorageGUI(player);
        StorageEvents.getInstance().registerGUI(gui.getInventory(), gui);
        gui.open(player);
    }

    // Handle click events in the GUI
    public void handleClick(InventoryClickEvent event) {
        gui.handleClick(event);
    }

    // Handle close events for saving inventory
    public void handleClose(InventoryCloseEvent event) {
        gui.handleClose(event);
    }

    // Save storage contents (placeholder for persistence)
    public void save() {
        // TODO: Save each page's inventory contents
    }

    // Load storage contents (placeholder for persistence)
    public void load() {
        // TODO: Load each page's inventory contents
    }
}
