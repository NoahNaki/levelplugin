package me.nakilex.levelplugin.items.config;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.items.data.StatRange;
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
            String path = "items." + item.getUuid();
            config.set(path + ".id",               item.getId());
            config.set(path + ".baseName",         item.getBaseName());
            config.set(path + ".rarity",           item.getRarity().name());
            config.set(path + ".levelRequirement", item.getLevelRequirement());
            config.set(path + ".classRequirement", item.getClassRequirement());
            config.set(path + ".material",         item.getMaterial().name());

            // Persist *rolled* stats as plain ints:
            config.set(path + ".hp",    item.getHp());
            config.set(path + ".def",   item.getDef());
            config.set(path + ".str",   item.getStr());
            config.set(path + ".agi",   item.getAgi());
            config.set(path + ".intel", item.getIntel());
            config.set(path + ".dex",   item.getDex());

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
                String base = "items." + key + ".";

                int id             = config.getInt(base + "id");
                String baseName    = config.getString(base + "baseName");
                ItemRarity rarity  = ItemRarity.valueOf(config.getString(base + "rarity"));
                int lvlReq         = config.getInt(base + "levelRequirement");
                String clsReq      = config.getString(base + "classRequirement");
                Material material  = Material.valueOf(config.getString(base + "material"));
                int upgLvl         = config.getInt(base + "upgradeLevel", 0);

                // Parse each rolled int as a single-value range:
                StatRange hpRange    = StatRange.fromString(config.getString(base + "hp",    "0"));
                StatRange defRange   = StatRange.fromString(config.getString(base + "def",   "0"));
                StatRange strRange   = StatRange.fromString(config.getString(base + "str",   "0"));
                StatRange agiRange   = StatRange.fromString(config.getString(base + "agi",   "0"));
                StatRange intelRange = StatRange.fromString(config.getString(base + "intel", "0"));
                StatRange dexRange   = StatRange.fromString(config.getString(base + "dex",   "0"));

                CustomItem instance = new CustomItem(
                    uuid, id, baseName, rarity, lvlReq, clsReq, material,
                    hpRange, defRange, strRange, agiRange, intelRange, dexRange,
                    upgLvl
                );

                ItemManager.getInstance().addInstance(instance);

            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load custom item [" + key + "]: " + e.getMessage());
            }
        }

        plugin.getLogger().info(
            "Loaded " + ItemManager.getInstance().getAllItems().size()
                + " custom items from custom_items.yml."
        );
    }

}
