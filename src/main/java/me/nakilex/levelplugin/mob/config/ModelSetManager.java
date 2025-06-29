package me.nakilex.levelplugin.mob.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ModelSetManager {

    private final Map<String, Map<Material, String>> sets = new HashMap<>();

    public ModelSetManager(Plugin plugin) {
        File file = new File(plugin.getDataFolder(), "model_sets.yml");
        if (!file.exists()) {
            plugin.saveResource("model_sets.yml", true);
        }
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = cfg.getConfigurationSection("sets");
        if (root != null) {
            for (String key : root.getKeys(false)) {
                ConfigurationSection sec = root.getConfigurationSection(key);
                if (sec == null) continue;
                Map<Material, String> map = new HashMap<>();
                for (String matKey : sec.getKeys(false)) {
                    try {
                        Material mat = Material.valueOf(matKey);
                        map.put(mat, sec.getString(matKey));
                    } catch (IllegalArgumentException ignore) {
                    }
                }
                sets.put(key.toLowerCase(), map);
            }
        }
    }

    public String getModelId(String setName, Material mat) {
        Map<Material, String> map = sets.get(setName.toLowerCase());
        if (map == null) return null;
        return map.get(mat);
    }
}
