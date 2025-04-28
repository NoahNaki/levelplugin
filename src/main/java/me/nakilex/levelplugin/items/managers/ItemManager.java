package me.nakilex.levelplugin.items.managers;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.data.StatRange;
import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ItemManager {

    private static ItemManager instance;

    public static ItemManager getInstance() {
        return instance;
    }

    private final Map<Integer, CustomItem> templatesMap = new HashMap<>(); // Templates by ID
    private final Map<UUID, CustomItem> itemsMap     = new HashMap<>(); // Instances by UUID

    private FileConfiguration itemsConfig;

    public ItemManager(Plugin plugin) {
        instance = this;
        loadItemsConfig(plugin);
        loadItems();
    }

    private void loadItemsConfig(Plugin plugin) {
        File file = new File(plugin.getDataFolder(), "items.yml");
        if (!file.exists()) {
            plugin.saveResource("items.yml", true);
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
        templatesMap.clear();
        itemsMap.clear();

        if (!itemsConfig.contains("items")) {
            Main.getInstance().getLogger().warning("No items found in items.yml!");
            return;
        }

        for (String key : itemsConfig.getConfigurationSection("items").getKeys(false)) {
            try {
                int numericId = Integer.parseInt(key);
                String path = "items." + key + ".";

                // Basic fields
                String name       = itemsConfig.getString(path + "name", "Unknown Item");
                ItemRarity rarity = ItemRarity.valueOf(
                    itemsConfig.getString(path + "rarity", "COMMON").toUpperCase());
                int levelReq      = itemsConfig.getInt(path + "level_requirement", 1);
                String classReq   = itemsConfig.getString(path + "class_requirement", "ANY");
                Material material = Material.valueOf(
                    itemsConfig.getString(path + "material", "STONE").toUpperCase());

                // === NEW: parse StatRanges instead of ints ===
                StatRange hpRange    = StatRange.fromString(
                    itemsConfig.getString(path + "hp", "0-0"));
                StatRange defRange   = StatRange.fromString(
                    itemsConfig.getString(path + "def", "0-0"));
                StatRange strRange   = StatRange.fromString(
                    itemsConfig.getString(path + "str", "0-0"));
                StatRange agiRange   = StatRange.fromString(
                    itemsConfig.getString(path + "agi", "0-0"));
                StatRange intelRange = StatRange.fromString(
                    itemsConfig.getString(path + "intel", "0-0"));
                StatRange dexRange   = StatRange.fromString(
                    itemsConfig.getString(path + "dex", "0-0"));

                // Build the template (rolls will happen when creating instances)
                CustomItem template = new CustomItem(
                    numericId,
                    name,
                    rarity,
                    levelReq,
                    classReq,
                    material,
                    hpRange,
                    defRange,
                    strRange,
                    agiRange,
                    intelRange,
                    dexRange
                );

                templatesMap.put(numericId, template);

            } catch (Exception e) {
                Main.getInstance().getLogger().warning("Failed to load item with key: " + key);
                e.printStackTrace();
            }
        }

        Main.getInstance().getLogger()
            .info("Loaded " + templatesMap.size() + " custom item templates from items.yml.");
    }

    /** Returns a new map of templates, keyed by numeric ID */
    public Map<Integer, CustomItem> getAllTemplates() {
        return new HashMap<>(templatesMap);
    }

    /** Fetch the template (with ranges) for a given numeric ID */
    public CustomItem getTemplateById(int id) {
        return templatesMap.get(id);
    }

    /** Register a freshly‐rolled instance */
    public void addInstance(CustomItem instance) {
        itemsMap.put(instance.getUuid(), instance);
    }

    /** Lookup a live instance by its UUID */
    public CustomItem getItemByUUID(UUID uuid) {
        return itemsMap.get(uuid);
    }

    /**
     * Given an ItemStack with our PDC UUID tag, pull out the matching
     * CustomItem instance.
     */
    public CustomItem getCustomItemFromItemStack(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) return null;

        ItemMeta meta = itemStack.getItemMeta();
        UUID uuid = ItemUtil.getItemUUID(itemStack);
        if (uuid == null) return null;

        CustomItem ci = itemsMap.get(uuid);
        if (ci == null) {
            Main.getInstance().getLogger()
                .info("No custom item found for UUID: " + uuid);
        }
        return ci;
    }

    public CustomItem rollNewInstance(int templateId) {
        CustomItem tpl = templatesMap.get(templateId);
        if (tpl == null) return null;

        CustomItem inst = new CustomItem(
            tpl.getId(),
            tpl.getBaseName(),
            tpl.getRarity(),
            tpl.getLevelRequirement(),
            tpl.getClassRequirement(),
            tpl.getMaterial(),
            tpl.getHpRange(),
            tpl.getDefRange(),
            tpl.getStrRange(),
            tpl.getAgiRange(),
            tpl.getIntelRange(),
            tpl.getDexRange()
        );
        addInstance(inst);
        return inst;
    }

    public CustomItem getCustomItem(int id) {
        return getTemplateById(id);
    }

    /** Return all active instances */
    public Map<UUID, CustomItem> getAllItems() {
        return new HashMap<>(itemsMap);
    }
}
