package me.nakilex.levelplugin.leaderboard;

import me.nakilex.levelplugin.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class DuelStatsManager {
    private final Main plugin;
    private File statsFile;
    private FileConfiguration config;

    public DuelStatsManager(Main plugin) {
        this.plugin = plugin;
        load();
    }

    private void load() {
        statsFile = new File(plugin.getDataFolder(), "duelstats.yml");
        if (!statsFile.exists()) {
            plugin.saveResource("duelstats.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(statsFile);
    }

    public void save() {
        try {
            config.save(statsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getWins(UUID uuid) {
        return config.getInt("wins." + uuid.toString(), 0);
    }

    public void addWin(UUID uuid) {
        String path = "wins." + uuid.toString();
        config.set(path, getWins(uuid) + 1);
        save();
    }
}
