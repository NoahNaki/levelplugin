package me.nakilex.levelplugin.leaderboards;

import me.nakilex.levelplugin.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Simple persistence for duel win counts per player.
 */
public class DuelStatsManager {
    private final Main plugin;
    private File file;
    private FileConfiguration config;

    public DuelStatsManager(Main plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        file = new File(plugin.getDataFolder(), "duel_stats.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create duel_stats.yml: " + e.getMessage());
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public int getWins(UUID id) {
        String path = "players." + id + ".wins";
        return config.getInt(path, 0);
    }

    public void addWin(UUID id) {
        String path = "players." + id + ".wins";
        config.set(path, getWins(id) + 1);
        save();
    }

    public java.util.Map<String, Object> getAll() {
        return config.getConfigurationSection("players") != null
            ? config.getConfigurationSection("players").getValues(false)
            : java.util.Collections.emptyMap();
    }

    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save duel_stats.yml: " + e.getMessage());
        }
    }
}
