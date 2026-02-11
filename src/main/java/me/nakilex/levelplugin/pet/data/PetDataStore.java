package me.nakilex.levelplugin.pet.data;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.data.ItemRarity;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
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

    public void clearProfile(UUID uuid) {
        profiles.remove(uuid);
        String root = "players." + uuid;
        config.set(root, null);
        saveConfig();
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
            String autoDiscard = config.getString(root + ".auto-discard", null);
            if (autoDiscard != null && !autoDiscard.isBlank()) {
                try {
                    profile.setAutoDiscardRarity(ItemRarity.valueOf(autoDiscard.toUpperCase()));
                } catch (IllegalArgumentException ignored) {
                    profile.setAutoDiscardRarity(null);
                }
            }
            profile.setAutoSkipSummonAnimation(config.getBoolean(root + ".summon.auto-skip-animation", false));
            String visibility = config.getString(root + ".pet-visibility", PetVisibility.ALL.name());
            try {
                profile.setPetVisibility(PetVisibility.valueOf(visibility.toUpperCase(java.util.Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
                profile.setPetVisibility(PetVisibility.ALL);
            }
            profile.setPityPullsSinceLegendary(config.getInt(root + ".summon.pity-legendary", 0));
            String petRoot = root + ".pets";
            var section = config.getConfigurationSection(petRoot);
            if (section != null) {
                for (String petId : section.getKeys(false)) {
                    int xp = config.getInt(petRoot + "." + petId + ".xp", 0);
                    int tier = config.getInt(petRoot + "." + petId + ".tier", 0);
                    int copies = config.getInt(petRoot + "." + petId + ".copies", 0);
                    profile.setPetXp(petId, xp);
                    profile.setPetTier(petId, tier);
                    profile.setPetCopies(petId, copies);
                    profile.setLastAcquiredAt(petId, config.getLong(petRoot + "." + petId + ".last-acquired", 0L));
                    List<Long> acquiredHistory = new java.util.ArrayList<>();
                    for (Long value : config.getLongList(petRoot + "." + petId + ".acquired-history")) {
                        if (value != null && value > 0L) {
                            acquiredHistory.add(value);
                        }
                    }
                    profile.setPetCopyAcquiredHistory(petId, acquiredHistory);
                    profile.setPetCopyIds(petId, config.getStringList(petRoot + "." + petId + ".copy-ids"));
                }
            }
            for (String locked : config.getStringList(root + ".merge.locked-copies")) {
                profile.setMergeLockedCopy(locked, true);
            }
            var pendingReturn = config.getLocation(root + ".summon.return");
            if (pendingReturn != null) {
                profile.setPendingSummonReturn(pendingReturn);
            }
        }
        String activeId = profile.activePetId();
        if (activeId != null && !activeId.isBlank() && profile.getPetCopies(activeId) <= 0) {
            profile.setPetCopies(activeId, 1);
        }
        return profile;
    }

    private void saveProfile(PetProfile profile) {
        String root = "players." + profile.ownerId();
        config.set(root + ".active", profile.activePetId());
        ItemRarity autoDiscard = profile.autoDiscardRarity();
        config.set(root + ".auto-discard", autoDiscard == null ? null : autoDiscard.name());
        for (Map.Entry<String, Integer> entry : profile.petXp().entrySet()) {
            config.set(root + ".pets." + entry.getKey() + ".xp", entry.getValue());
        }
        for (Map.Entry<String, Integer> entry : profile.petTiers().entrySet()) {
            config.set(root + ".pets." + entry.getKey() + ".tier", entry.getValue());
        }
        for (Map.Entry<String, Integer> entry : profile.petCopies().entrySet()) {
            config.set(root + ".pets." + entry.getKey() + ".copies", entry.getValue());
        }
        for (Map.Entry<String, Long> entry : profile.lastAcquiredAt().entrySet()) {
            config.set(root + ".pets." + entry.getKey() + ".last-acquired", entry.getValue());
        }
        for (Map.Entry<String, java.util.List<Long>> entry : profile.petCopyAcquiredAt().entrySet()) {
            config.set(root + ".pets." + entry.getKey() + ".acquired-history", entry.getValue());
        }
        for (Map.Entry<String, java.util.List<String>> entry : profile.petCopyIds().entrySet()) {
            config.set(root + ".pets." + entry.getKey() + ".copy-ids", entry.getValue());
        }
        config.set(root + ".merge.locked-copies", new java.util.ArrayList<>(profile.mergeLockedCopyIds()));
        config.set(root + ".merge.locked", null);
        config.set(root + ".summon.return", profile.pendingSummonReturn());
        config.set(root + ".summon.auto-skip-animation", profile.autoSkipSummonAnimation());
        config.set(root + ".pet-visibility", profile.petVisibility().name());
        config.set(root + ".summon.pity-legendary", profile.pityPullsSinceLegendary());
    }

    private void saveConfig() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save pet_data.yml: " + e.getMessage());
        }
    }
}
