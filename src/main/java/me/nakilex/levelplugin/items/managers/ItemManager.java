package me.nakilex.levelplugin.items.managers;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.data.StatRange;
import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.mob.utils.CombatRewardCalculator.GearTarget;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import me.nakilex.levelplugin.items.generator.ProceduralItemGenerator;

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
    private final Map<Integer, UUID> holderMap = new HashMap<>();

    /**
     * Procedurally generated items all receive unique negative IDs. We keep a
     * counter that starts at -1 and decrements for each generated item so the
     * IDs never collide with the positive template IDs from items.yml.
     */
    private int nextGeneratedId = -1;

    private final ProceduralItemGenerator generator;

    private FileConfiguration itemsConfig;

    public ItemManager(Plugin plugin) {
        instance = this;
        loadItemsConfig(plugin);
        loadItems();
        generator = new ProceduralItemGenerator(Main.getInstance());
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
                // Normalize class requirement based on weapon family if mismatched
                me.nakilex.levelplugin.items.data.WeaponType wt =
                        me.nakilex.levelplugin.items.data.WeaponType.matchType(new org.bukkit.inventory.ItemStack(material));
                if (wt != null) {
                    classReq = switch (wt) {
                        case WAND -> "MAGE";
                        case BOW -> "ARCHER";
                        case SHOVEL, AXE -> "WARRIOR";
                        case SWORD -> "ROGUE";
                    };
                }

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
                StatRange wilRange   = StatRange.fromString(
                    itemsConfig.getString(path + "wil", "0-0"));
                StatRange tecRange   = StatRange.fromString(
                    itemsConfig.getString(path + "tec", "0-0"));

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
                    dexRange,
                    wilRange,
                    tecRange
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
            tpl.getDexRange(),
            tpl.getWilRange(),
            tpl.getTecRange()
        );
        addInstance(inst);
        return inst;
    }

    /**
     * Obtain the next unique ID for a procedurally generated item. IDs start at
     * -1 and move downward to avoid colliding with positive template IDs.
     */
    public synchronized int getNextGeneratedId() {
        return nextGeneratedId--;
    }

    /**
     * Generate a brand new procedural item using the generator utility.
     */
    public CustomItem generateItem(String mobType, int level) {
        return generator.generate(mobType, level);
    }

    public CustomItem generateItemWithMaxRarity(String mobType, int level, ItemRarity maxRarity) {
        return generator.generateWithMaxRarity(mobType, level, maxRarity);
    }

    public CustomItem generateItemForGearScore(String mobType, int targetGearScore, ItemRarity rarity) {
        return generator.generateForGearScore(mobType, new GearTarget(targetGearScore, rarity));
    }

    public CustomItem getCustomItem(int id) {
        return getTemplateById(id);
    }

    public Map<UUID, CustomItem> getAllItems() {
        return new HashMap<>(itemsMap);
    }

    public void registerHolder(int itemID, UUID puuid) {
        holderMap.put(itemID, puuid);
    }

    public void unregisterHolder(int itemID) {
        holderMap.remove(itemID);
    }

    public Player getHolderOf(int itemID) {
        UUID puuid = holderMap.get(itemID);
        if (puuid == null) return null;
        return Bukkit.getPlayer(puuid);
    }
}
