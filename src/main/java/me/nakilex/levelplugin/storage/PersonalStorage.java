package me.nakilex.levelplugin.storage;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class PersonalStorage {

    private final Player owner;
    private final Inventory inventory;

    public PersonalStorage(Player owner) {
        this.owner = owner;
        this.inventory = Bukkit.createInventory(null, 27, owner.getName() + "'s Storage");
    }

    // Open storage GUI for the player
    public void open(Player player) {
        player.openInventory(inventory);
    }

    // Save storage contents (placeholder for persistence)
    public void save() {
        // TODO: Save inventory contents to file
    }

    // Load storage contents (placeholder for persistence)
    public void load() {
        // TODO: Load inventory contents from file
    }

    public Inventory getInventory() {
        return inventory;
    }
}
