package me.nakilex.levelplugin.spells.deck;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.input.SpellInputType;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class SpellDeckDataStore {
    private final Main plugin;
    private final File file;
    private final YamlConfiguration config;
    private final Map<UUID, SpellDeckProfile> profiles = new HashMap<>();

    public SpellDeckDataStore(Main plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "spell_decks.yml");
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public SpellDeckProfile getProfile(UUID ownerId) {
        return profiles.computeIfAbsent(ownerId, this::loadProfile);
    }

    public void saveProfile(UUID ownerId) {
        if (ownerId == null) {
            return;
        }
        SpellDeckProfile profile = profiles.get(ownerId);
        if (profile == null) {
            return;
        }
        saveProfile(profile);
        saveConfig();
    }

    public void saveAll() {
        for (SpellDeckProfile profile : profiles.values()) {
            saveProfile(profile);
        }
        saveConfig();
    }

    private SpellDeckProfile loadProfile(UUID uuid) {
        SpellDeckProfile profile = new SpellDeckProfile(uuid);
        String root = "players." + uuid;
        profile.setPityPullsSinceLegendary(config.getInt(root + ".pulls.pity-legendary", 0));

        var owned = config.getConfigurationSection(root + ".owned");
        if (owned != null) {
            for (String cardId : owned.getKeys(false)) {
                profile.setCopies(cardId, owned.getInt(cardId, 0));
            }
        }

        var invested = config.getConfigurationSection(root + ".invested");
        if (invested != null) {
            for (String cardId : invested.getKeys(false)) {
                profile.setInvestedCopies(cardId, invested.getInt(cardId, 0));
            }
        }

        var equipped = config.getConfigurationSection(root + ".equipped");
        if (equipped != null) {
            for (String key : equipped.getKeys(false)) {
                try {
                    SpellInputType inputType = SpellInputType.valueOf(key.toUpperCase(Locale.ROOT));
                    profile.equip(inputType, equipped.getString(key));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return profile;
    }

    private void saveProfile(SpellDeckProfile profile) {
        String root = "players." + profile.ownerId();
        config.set(root + ".pulls.pity-legendary", profile.pityPullsSinceLegendary());
        config.set(root + ".owned", null);
        for (Map.Entry<String, Integer> entry : profile.ownedCopies().entrySet()) {
            config.set(root + ".owned." + entry.getKey(), Math.max(0, entry.getValue()));
        }
        config.set(root + ".invested", null);
        for (Map.Entry<String, Integer> entry : profile.investedCopies().entrySet()) {
            config.set(root + ".invested." + entry.getKey(), Math.max(0, entry.getValue()));
        }
        config.set(root + ".equipped", null);
        for (Map.Entry<SpellInputType, String> entry : profile.equippedCards().entrySet()) {
            config.set(root + ".equipped." + entry.getKey().name(), entry.getValue());
        }
    }

    private void saveConfig() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save spell_decks.yml: " + e.getMessage());
        }
    }
}
