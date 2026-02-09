package me.nakilex.levelplugin.pet.data;

import me.nakilex.levelplugin.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PetDataStore {
    private final Main plugin;
    private final File file;
    private final FileConfiguration config;
    private final Map<UUID, PetProfile> profiles = new HashMap<>();

    public PetDataStore(Main plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "pet_data.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create pet_data.yml: " + e.getMessage());
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public PetProfile getProfile(UUID uuid) {
        return profiles.computeIfAbsent(uuid, this::loadProfile);
    }

    public void saveProfile(UUID uuid) {
        PetProfile profile = profiles.get(uuid);
        if (profile != null) {
            saveProfile(profile);
        }
    }

    public void saveAll() {
        for (PetProfile profile : profiles.values()) {
            saveProfile(profile);
        }
        saveConfig();
    }

    private PetProfile loadProfile(UUID uuid) {
        PetProfile profile = new PetProfile(uuid);
        String root = "players." + uuid;
        if (config.contains(root)) {
            profile.setActivePetId(config.getString(root + ".active", null));
            String petRoot = root + ".pets";
            var section = config.getConfigurationSection(petRoot);
            if (section != null) {
                for (String petId : section.getKeys(false)) {
                    int xp = config.getInt(petRoot + "." + petId + ".xp", 0);
                    int tier = config.getInt(petRoot + "." + petId + ".tier", 0);
                    int copies = config.getInt(petRoot + "." + petId + ".copies", 1);
                    profile.setPetXp(petId, xp);
                    profile.setPetTier(petId, tier);
                    if (copies > 1) {
                        profile.addPetCopies(petId, copies - 1);
                    }
                }
            }
        }
        return profile;
    }

    private void saveProfile(PetProfile profile) {
        String root = "players." + profile.ownerId();
        config.set(root + ".active", profile.activePetId());
        for (Map.Entry<String, Integer> entry : profile.petXp().entrySet()) {
            config.set(root + ".pets." + entry.getKey() + ".xp", entry.getValue());
        }
        for (Map.Entry<String, Integer> entry : profile.petTiers().entrySet()) {
            config.set(root + ".pets." + entry.getKey() + ".tier", entry.getValue());
        }
        for (Map.Entry<String, Integer> entry : profile.petCopies().entrySet()) {
            config.set(root + ".pets." + entry.getKey() + ".copies", entry.getValue());
        }
    }

    private void saveConfig() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save pet_data.yml: " + e.getMessage());
        }
    }
}
