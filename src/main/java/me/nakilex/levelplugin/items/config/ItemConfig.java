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
            config.set(path + ".enchantCount", item.getEnchantCount());

            // Persist current durability (maxDurability is static, so no need to save it)
            config.set(path + ".currentDurability", item.getCurrentDurability());
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
                int enchantCount   = config.getInt(base + "enchantCount", 0);

                // Restore the rolled values and original template ranges.
                int hpValue    = config.getInt(base + "hp",    0);
                int defValue   = config.getInt(base + "def",   0);
                int strValue   = config.getInt(base + "str",   0);
                int agiValue   = config.getInt(base + "agi",   0);
                int intelValue = config.getInt(base + "intel", 0);
                int dexValue   = config.getInt(base + "dex",   0);

                CustomItem template = ItemManager.getInstance().getTemplateById(id);
                StatRange hpRange;
                StatRange defRange;
                StatRange strRange;
                StatRange agiRange;
                StatRange intelRange;
                StatRange dexRange;
                if (template != null) {
                    hpRange    = template.getHpRange();
                    defRange   = template.getDefRange();
                    strRange   = template.getStrRange();
                    agiRange   = template.getAgiRange();
                    intelRange = template.getIntelRange();
                    dexRange   = template.getDexRange();
                } else {
                    // Fallback if template is missing
                    hpRange    = new StatRange(hpValue,    hpValue);
                    defRange   = new StatRange(defValue,   defValue);
                    strRange   = new StatRange(strValue,   strValue);
                    agiRange   = new StatRange(agiValue,   agiValue);
                    intelRange = new StatRange(intelValue, intelValue);
                    dexRange   = new StatRange(dexValue,   dexValue);
                }

                CustomItem instance = new CustomItem(
                    uuid, id, baseName, rarity, lvlReq, clsReq, material,
                    hpRange, defRange, strRange, agiRange, intelRange, dexRange,
                    upgLvl, enchantCount
                );

                // Overwrite rolled stats with the saved values
                instance.setBaseHp(hpValue);
                instance.setBaseDef(defValue);
                instance.setBaseStr(strValue);
                instance.setBaseAgi(agiValue);
                instance.setBaseIntel(intelValue);
                instance.setBaseDex(dexValue);

                // Restore saved current durability (default to max if not present)
                int savedDurability = config.getInt(base + "currentDurability", instance.getMaxDurability());
                int toReduce = instance.getMaxDurability() - savedDurability;
                if (toReduce > 0) {
                    instance.reduceDurability(toReduce);
                }

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
