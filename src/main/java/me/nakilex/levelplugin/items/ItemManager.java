package me.nakilex.levelplugin.items;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ItemManager {

    private static ItemManager instance;
    public static ItemManager getInstance() { return instance; }

    private Map<Integer, CustomItem> itemsMap = new HashMap<>();
    private FileConfiguration itemsConfig;

    public ItemManager(Plugin plugin) {
        instance = this;
        loadItemsConfig(plugin);
        loadItems();
    }

    private void loadItemsConfig(Plugin plugin) {
        File file = new File(plugin.getDataFolder(), "items.yml");
        if (!file.exists()) {
            plugin.saveResource("items.yml", false);
        }
        itemsConfig = YamlConfiguration.loadConfiguration(file);

        InputStream defStream = plugin.getResource("items.yml");
        if (defStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defStream, StandardCharsets.UTF_8));
            itemsConfig.setDefaults(defConfig);
        }
    }

    private void loadItems() {
        itemsMap.clear();

        if (!itemsConfig.contains("items")) {
            return;
        }

        for (String key : itemsConfig.getConfigurationSection("items").getKeys(false)) {
            int numericId;
            try {
                numericId = Integer.parseInt(key);
            } catch (NumberFormatException e) {
                continue; // skip invalid numeric
            }

            String path = "items." + key;

            String name = itemsConfig.getString(path + ".name", "Unknown Item");
            String rarityStr = itemsConfig.getString(path + ".rarity", "COMMON");
            int levelReq = itemsConfig.getInt(path + ".level_requirement", 1);
            String classReq = itemsConfig.getString(path + ".class_requirement", "ANY");
            String materialStr = itemsConfig.getString(path + ".material", "STONE"); // default ?

            int hp  = itemsConfig.getInt(path + ".hp", 0);
            int def = itemsConfig.getInt(path + ".def", 0);
            int str = itemsConfig.getInt(path + ".str", 0);
            int agi = itemsConfig.getInt(path + ".agi", 0);
            int intel = itemsConfig.getInt(path + ".int", 0);
            int dex = itemsConfig.getInt(path + ".dex", 0);

            ItemRarity rarity;
            try {
                rarity = ItemRarity.valueOf(rarityStr.toUpperCase());
            } catch (Exception ex) {
                rarity = ItemRarity.COMMON;
            }

            Material mat;
            try {
                mat = Material.valueOf(materialStr.toUpperCase());
            } catch (Exception ex) {
                mat = Material.STONE; // fallback if invalid
            }

            CustomItem cItem = new CustomItem(
                numericId,
                name,
                rarity,
                levelReq,
                classReq,
                mat,
                hp, def, str, agi, intel, dex
            );

            itemsMap.put(numericId, cItem);
        }
    }

    public CustomItem getItemById(int itemId) {
        return itemsMap.get(itemId);
    }
}
