package me.nakilex.levelplugin.dungeon.rating;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple manager to store and retrieve dungeon ratings.
 */
public class DungeonRatingManager {
    private final Plugin plugin;
    private final File file;
    private final FileConfiguration config;
    private final Map<String, RatingData> ratings = new HashMap<>();

    private static class RatingData {
        double total;
        int count;
        RatingData(double t, int c) { total = t; count = c; }
    }

    public DungeonRatingManager(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "dungeon_ratings.yml");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        this.config = YamlConfiguration.loadConfiguration(file);
        load();
    }

    private void load() {
        if (config.isConfigurationSection("ratings")) {
            for (String key : config.getConfigurationSection("ratings").getKeys(false)) {
                double total = config.getDouble("ratings." + key + ".total", 0);
                int count = config.getInt("ratings." + key + ".count", 0);
                ratings.put(key, new RatingData(total, count));
            }
        }
    }

    /**
     * Add a new rating entry for the given dungeon key.
     */
    public synchronized void addRating(String key, double rating) {
        RatingData data = ratings.getOrDefault(key, new RatingData(0, 0));
        data.total += rating;
        data.count += 1;
        ratings.put(key, data);
        save();
    }

    /**
     * Get the average rating for the given dungeon key.
     */
    public synchronized double getAverage(String key) {
        RatingData data = ratings.get(key);
        if (data == null || data.count == 0) return 0.0;
        return data.total / data.count;
    }

    private synchronized void save() {
        for (var entry : ratings.entrySet()) {
            String path = "ratings." + entry.getKey();
            config.set(path + ".total", entry.getValue().total);
            config.set(path + ".count", entry.getValue().count);
        }
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
