package me.nakilex.levelplugin.mob;

import me.nakilex.levelplugin.managers.HorseConfigManager;

import java.util.HashMap;
import java.util.UUID;

public class HorseManager {

    private final HorseConfigManager configManager;
    private final HashMap<UUID, HorseData> horses = new HashMap<>();

    // Constructor to accept HorseConfigManager
    public HorseManager(HorseConfigManager configManager) {
        this.configManager = configManager;
    }

    // Example methods
    public HorseData getHorse(UUID uuid) {
        return horses.get(uuid);
    }

    public void rerollHorse(UUID uuid) {
        HorseData newHorse = HorseData.randomHorse(uuid);
        horses.put(uuid, newHorse);
        configManager.saveHorseData(uuid, newHorse); // Persist data
    }
}
