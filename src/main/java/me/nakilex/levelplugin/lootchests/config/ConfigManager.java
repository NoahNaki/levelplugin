package me.nakilex.levelplugin.lootchests.config;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration lootChestsConfig;
    private File lootChestsFile;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        setupLootChestsConfig();
    }

    private void setupLootChestsConfig() {
        lootChestsFile = new File(plugin.getDataFolder(), "lootchests.yml");
        if (!lootChestsFile.exists()) {
            plugin.saveResource("lootchests.yml", false);
        }
        lootChestsConfig = YamlConfiguration.loadConfiguration(lootChestsFile);
    }

    public FileConfiguration getLootChestsConfig() {
        return lootChestsConfig;
    }

    public void saveLootChestsConfig() {
        try {
            lootChestsConfig.save(lootChestsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save lootchests.yml!");
            e.printStackTrace();
        }
    }

    public int addLootChest(Location location, BlockFace facing) {
        if (location == null || location.getWorld() == null) {
            throw new IllegalArgumentException("Location and world must not be null");
        }

        ConfigurationSection section = lootChestsConfig.getConfigurationSection("loot_chests");
        if (section == null) {
            section = lootChestsConfig.createSection("loot_chests");
        }

        int nextId = section.getKeys(false).stream()
                .mapToInt(key -> {
                    try {
                        return Integer.parseInt(key);
                    } catch (NumberFormatException ignored) {
                        return 0;
                    }
                })
                .max()
                .orElse(0) + 1;

        String coords = location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();
        String key = String.valueOf(nextId);
        section.set(key + ".coordinates", coords);
        section.set(key + ".world", location.getWorld().getName());
        section.set(key + ".facing", (facing == null ? BlockFace.NORTH : facing).name());

        saveLootChestsConfig();
        return nextId;
    }

    public void reloadLootChestsConfig() {
        lootChestsConfig = YamlConfiguration.loadConfiguration(lootChestsFile);
    }

    public boolean removeLootChest(int chestId) {
        ConfigurationSection section = lootChestsConfig.getConfigurationSection("loot_chests");
        if (section == null) {
            return false;
        }

        String key = String.valueOf(chestId);
        if (!section.contains(key)) {
            return false;
        }

        section.set(key, null);
        saveLootChestsConfig();
        return true;
    }
}
