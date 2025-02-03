package me.nakilex.levelplugin.storage;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.storage.gui.StorageGUI;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PersonalStorage {

    private final UUID ownerId;
    private StorageGUI storageGUI;

    public PersonalStorage(UUID ownerId) {
        // Fix #1: Assign the ownerId field
        this.ownerId = ownerId;

        // Pass both arguments to StorageGUI
        this.storageGUI = new StorageGUI(
            ownerId,
            Main.getInstance().getStorageEvents()
        );
    }

    public void open(Player player) {
        if (storageGUI == null) {
            // Fix #2: Also pass both arguments here
            this.storageGUI = new StorageGUI(
                ownerId,
                Main.getInstance().getStorageEvents()
            );
        }
        storageGUI.open(player);
    }

    public void save() {
        if (storageGUI != null) {
            storageGUI.saveToDisk();  // Writes to <uuid>.yml
        }
    }
    public void load() {
        if (storageGUI == null) {
            storageGUI = new StorageGUI(
                ownerId,
                Main.getInstance().getStorageEvents()
            );
        }
        storageGUI.loadFromDisk();
    }


    public UUID getOwnerId() {
        return ownerId;
    }

    public StorageGUI getStorageGUI() {
        return storageGUI;
    }
}
