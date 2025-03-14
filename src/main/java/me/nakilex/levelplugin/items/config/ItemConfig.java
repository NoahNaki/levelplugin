package me.nakilex.levelplugin.items.config;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.items.managers.ItemManager;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class ItemConfig {

    private final Main plugin;
    private final File file;
    private final FileConfiguration config;

    public ItemConfig(Main plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "custom_items.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create custom_items.yml: " + e.getMessage());
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public void saveItems() {
        for (CustomItem item : ItemManager.getInstance().getAllItems().values()) {
            String path = "items." + item.getUuid().toString();
            config.set(path + ".id", item.getId());
            config.set(path + ".baseName", item.getBaseName());
            config.set(path + ".rarity", item.getRarity().name());
            config.set(path + ".levelRequirement", item.getLevelRequirement());
            config.set(path + ".classRequirement", item.getClassRequirement());
            config.set(path + ".material", item.getMaterial().name());
            config.set(path + ".hp", item.getHp());
            config.set(path + ".def", item.getDef());
            config.set(path + ".str", item.getStr());
            config.set(path + ".agi", item.getAgi());
            config.set(path + ".intel", item.getIntel());
            config.set(path + ".dex", item.getDex());
            config.set(path + ".upgradeLevel", item.getUpgradeLevel());
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save custom_items.yml: " + e.getMessage());
        }
    }

    public void loadItems() {
        if (!config.contains("items")) return;

        for (String key : config.getConfigurationSection("items").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String path = "items." + key;

                int id = config.getInt(path + ".id");
                String baseName = config.getString(path + ".baseName");
                ItemRarity rarity = ItemRarity.valueOf(config.getString(path + ".rarity"));
                int levelReq = config.getInt(path + ".levelRequirement");
                String classReq = config.getString(path + ".classRequirement");
                Material material = Material.valueOf(config.getString(path + ".material"));

                int hp = config.getInt(path + ".hp");
                int def = config.getInt(path + ".def");
                int str = config.getInt(path + ".str");
                int agi = config.getInt(path + ".agi");
                int intel = config.getInt(path + ".intel");
                int dex = config.getInt(path + ".dex");
                int upgradeLevel = config.getInt(path + ".upgradeLevel");

                CustomItem cItem = new CustomItem(uuid, id, baseName, rarity, levelReq, classReq, material, hp, def, str, agi, intel, dex, upgradeLevel);
                ItemManager.getInstance().addInstance(cItem);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load custom item with UUID: " + key);
                e.printStackTrace();
            }
        }

        plugin.getLogger().info("Loaded " + ItemManager.getInstance().getAllItems().size() + " custom items from file.");
    }
}
