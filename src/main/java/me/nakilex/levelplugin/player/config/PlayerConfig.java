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
import java.util.UUID;

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

    public void savePlayerData(UUID uuid) {
        StatsManager.PlayerStats stats = StatsManager.getInstance().getPlayerStats(uuid);
        LevelManager levelManager = LevelManager.getInstance();

        String path = "players." + uuid.toString();
        config.set(path + ".level", levelManager.getLevel(Bukkit.getPlayer(uuid)));
        config.set(path + ".xp", levelManager.getXP(Bukkit.getPlayer(uuid)));
        config.set(path + ".skill_points", stats.skillPoints);
        config.set(path + ".skill_points", stats.skillPoints);
        config.set(path + ".stats.base_strength", stats.baseStrength);
        config.set(path + ".stats.base_agility", stats.baseAgility);
        config.set(path + ".stats.base_intelligence", stats.baseIntelligence);
        config.set(path + ".stats.base_dexterity", stats.baseDexterity);
        config.set(path + ".stats.base_health", stats.baseHealthStat);
        config.set(path + ".stats.base_defense", stats.baseDefenceStat);
        config.set(path + ".class", stats.playerClass.name());

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save player data for " + uuid + ": " + e.getMessage());
        }
    }

    public void loadPlayerData(UUID uuid) {
        if (!config.contains("players." + uuid)) return;

        String path = "players." + uuid.toString();
        PlayerClass playerClass = PlayerClass.valueOf(config.getString(path + ".class", PlayerClass.VILLAGER.name()));
        int level = config.getInt(path + ".level", 1);
        int xp = config.getInt(path + ".xp", 0);
        int skillPoints = config.getInt(path + ".skill_points", 0);

        // Set level and XP using UUID
        LevelManager levelManager = LevelManager.getInstance();
        levelManager.setLevel(uuid, level);
        levelManager.addXP(uuid, xp);

        // Update StatsManager
        StatsManager.PlayerStats stats = StatsManager.getInstance().getPlayerStats(uuid);
        stats.playerClass = playerClass;
        stats.skillPoints = skillPoints;
        stats.skillPoints = config.getInt(path + ".skill_points", 0);
        stats.baseStrength = config.getInt(path + ".stats.base_strength", 0);
        stats.baseAgility = config.getInt(path + ".stats.base_agility", 0);
        stats.baseIntelligence = config.getInt(path + ".stats.base_intelligence", 0);
        stats.baseDexterity = config.getInt(path + ".stats.base_dexterity", 0);
        stats.baseHealthStat = config.getInt(path + ".stats.base_health", 0);
        stats.baseDefenceStat = config.getInt(path + ".stats.base_defense", 0);
    }


    public void saveAllPlayers() {
        for (UUID uuid : StatsManager.getInstance().getAllPlayerUUIDs()) {
            savePlayerData(uuid);
        }
    }

    public void loadAllPlayers() {
        if (!config.contains("players")) return;

        for (String uuidStr : config.getConfigurationSection("players").getKeys(false)) {
            UUID uuid = UUID.fromString(uuidStr);
            loadPlayerData(uuid);
        }
    }
}
