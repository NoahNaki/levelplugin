package me.nakilex.levelplugin.player.config;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Manages persistence of per-player data, including stats, level, and equipped runes.
 */
public class PlayerConfig {

    private final Main plugin;
    private final File file;
    private final FileConfiguration config;

    public PlayerConfig(Main plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "player_data.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create player_data.yml: " + e.getMessage());
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    /** Saves stats and level for one player. */
    public void savePlayerData(UUID uuid) {
        StatsManager.PlayerStats stats = StatsManager.getInstance().getPlayerStats(uuid);
        LevelManager levelManager = LevelManager.getInstance();

        String path = "players." + uuid.toString();
        // Use UUID-based lookups so offline players save correctly
        config.set(path + ".level", levelManager.getLevel(uuid));
        config.set(path + ".xp",    levelManager.getXP(uuid));
        config.set(path + ".skill_points", stats.skillPoints);
        config.set(path + ".stats.base_strength", stats.baseStrength);
        config.set(path + ".stats.base_agility", stats.baseAgility);
        config.set(path + ".stats.base_intelligence", stats.baseIntelligence);
        config.set(path + ".stats.base_dexterity", stats.baseDexterity);
        config.set(path + ".stats.base_health", stats.baseHealthStat);
        config.set(path + ".stats.base_defense", stats.baseDefenceStat);
        config.set(path + ".class", stats.playerClass.name());

        saveConfig();
    }

    /** Loads stats and level for one player. */
    public void loadPlayerData(UUID uuid) {
        String root = "players." + uuid.toString();
        if (!config.contains(root)) return;

        PlayerClass playerClass = PlayerClass.valueOf(
            config.getString(root + ".class", PlayerClass.VILLAGER.name())
        );
        int level = config.getInt(root + ".level", 1);
        int xp = config.getInt(root + ".xp", 0);
        int skillPoints = config.getInt(root + ".skill_points", 0);

        LevelManager lm = LevelManager.getInstance();
        lm.setLevel(uuid, level);
        lm.addXP(uuid, xp);

        StatsManager.PlayerStats stats = StatsManager.getInstance().getPlayerStats(uuid);
        stats.playerClass = playerClass;
        stats.skillPoints = skillPoints;
        stats.baseStrength      = config.getInt(root + ".stats.base_strength", 0);
        stats.baseAgility       = config.getInt(root + ".stats.base_agility", 0);
        stats.baseIntelligence  = config.getInt(root + ".stats.base_intelligence", 0);
        stats.baseDexterity     = config.getInt(root + ".stats.base_dexterity", 0);
        stats.baseHealthStat    = config.getInt(root + ".stats.base_health", 0);
        stats.baseDefenceStat   = config.getInt(root + ".stats.base_defense", 0);
    }

    /** Saves data for all players. */
    public void saveAllPlayers() {
        for (UUID uuid : StatsManager.getInstance().getAllPlayerUUIDs()) {
            savePlayerData(uuid);
        }
    }

    /** Loads data for all players. */
    public void loadAllPlayers() {
        if (!config.contains("players")) return;
        for (String uuidStr : config.getConfigurationSection("players").getKeys(false)) {
            UUID uuid = UUID.fromString(uuidStr);
            loadPlayerData(uuid);
        }
    }

    public List<String> getEquippedRunes(UUID playerUuid) {
        String path = "players." + playerUuid + ".equippedRunes";
        List<String> runes = config.getStringList(path);
        return runes != null ? runes : Collections.emptyList();
    }

    public void setEquippedRunes(UUID playerUuid, List<String> runeIds) {
        String path = "players." + playerUuid + ".equippedRunes";
        config.set(path, runeIds);
        saveConfig();
    }

    private void saveConfig() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save player_data.yml: " + e.getMessage());
        }
    }

    public void savePlayer(UUID playerUuid) {
        // Persist stats and equipped runes
        savePlayerData(playerUuid);
        saveConfig();
    }
}
