package me.nakilex.levelplugin.storage;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.storage.PersonalStorage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StorageManager {

    // Holds all player storages in memory
    private final Map<UUID, PersonalStorage> storages = new HashMap<>();

    /**
     * Basic constructor—could load data from disk if you want
     * all storages loaded on startup.
     */
    // In StorageManager.java
    public StorageManager() {
        // Optionally load existing data here—do it now!
        File storageFolder = new File(Main.getInstance().getDataFolder(), "storage");

        // Make sure the folder exists (could be empty if no players have created storage yet)
        if (!storageFolder.exists()) {
            storageFolder.mkdirs();
        }

        // For each "player_<UUID>.yml" file, parse out the UUID and load that player's storage
        File[] files = storageFolder.listFiles();
        if (files != null) {
            for (File f : files) {
                String fileName = f.getName();
                if (fileName.startsWith("player_") && fileName.endsWith(".yml")) {
                    // Extract the UUID from "player_<uuid>.yml"
                    String uuidPart = fileName.substring("player_".length(), fileName.length() - 4); // remove .yml
                    try {
                        UUID uuid = UUID.fromString(uuidPart);

                        // Create the personal storage and load items
                        PersonalStorage ps = new PersonalStorage(uuid);
                        ps.load();  // loads their pages/items from disk

                        // Put it in memory
                        storages.put(uuid, ps);
                    } catch (IllegalArgumentException e) {
                        // If it's not a valid UUID, just skip
                        Bukkit.getLogger().warning("[StorageManager] Invalid file name: " + fileName);
                    }
                }
            }
        }
    }

    /**
     * Creates a new PersonalStorage for a player, if they don’t already have one.
     */
    public void createStorage(UUID playerId) {
        if (!storages.containsKey(playerId)) {
            PersonalStorage newStorage = new PersonalStorage(playerId);
            newStorage.load();  // recovers items from an old session if they exist
            storages.put(playerId, newStorage);
        }
    }



    /**
     * Checks if a player already has storage.
     */
    public boolean hasStorage(UUID playerId) {
        return storages.containsKey(playerId);
    }

    /**
     * Retrieves the PersonalStorage instance for a player.
     */
    public PersonalStorage getStorage(UUID playerId) {
        return storages.get(playerId);
    }

    /**
     * Opens the storage for a specific player (if it exists).
     */
    public void openStorage(Player player) {
        PersonalStorage ps = storages.get(player.getUniqueId());
        if (ps != null) {
            // 1) Load from disk, so they get their saved items
            ps.load();
            // 2) Now actually open the GUI
            ps.open(player);
        } else {
            player.sendMessage("You do not have storage! Use /ps create first.");
        }
    }


    /**
     * Save all storages to disk—call on server shutdown, or periodically.
     */
    public void saveAllStorages() {
        for (PersonalStorage ps : storages.values()) {
            ps.save();
        }
    }
}
