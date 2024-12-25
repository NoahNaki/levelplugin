package me.nakilex.levelplugin.storage;

import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;

public class StorageManager {

    // Map to track storages by player UUID
    private final Map<String, PersonalStorage> storageMap = new HashMap<>();

    // Singleton instance
    private static StorageManager instance;

    private StorageManager() {
        // Private constructor for Singleton
    }

    // Get the singleton instance
    public static StorageManager getInstance() {
        if (instance == null) {
            instance = new StorageManager();
        }
        return instance;
    }

    // Create a new storage for a player
    public void createStorage(Player player) {
        if (storageMap.containsKey(player.getUniqueId().toString())) {
            player.sendMessage("You already have a storage!");
            return;
        }

        PersonalStorage storage = new PersonalStorage(player);
        storageMap.put(player.getUniqueId().toString(), storage);
        player.sendMessage("Storage created successfully!");
    }

    // Open a player's storage
    public void openStorage(Player player) {
        if (!storageMap.containsKey(player.getUniqueId().toString())) {
            player.sendMessage("You don't have a storage! Use /ps create.");
            return;
        }

        storageMap.get(player.getUniqueId().toString()).open(player);
    }

    // Get a player's storage
    public PersonalStorage getStorage(Player player) {
        return storageMap.get(player.getUniqueId().toString());
    }

    // Save all storages (placeholder for persistence)
    public void saveAll() {
        for (PersonalStorage storage : storageMap.values()) {
            storage.save();
        }
    }

    // Load all storages (placeholder for persistence)
    public void loadAll() {
        // TODO: Implement loading logic from files
    }
}
