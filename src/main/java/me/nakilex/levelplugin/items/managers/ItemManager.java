package me.nakilex.levelplugin.items.managers;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
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
    private final Map<UUID, CustomItem> itemsMap = new HashMap<>(); // Instances by UUID
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
        templatesMap.clear(); // Clear templates map
        itemsMap.clear();     // Clear instances map

        if (!itemsConfig.contains("items")) {
            Main.getInstance().getLogger().warning("No items found in items.yml!");
            return;
        }

        for (String key : itemsConfig.getConfigurationSection("items").getKeys(false)) {
            try {
                int numericId = Integer.parseInt(key);
                String path = "items." + key;

                String name = itemsConfig.getString(path + ".name", "Unknown Item");
                String rarityStr = itemsConfig.getString(path + ".rarity", "COMMON");
                int levelReq = itemsConfig.getInt(path + ".level_requirement", 1);
                String classReq = itemsConfig.getString(path + ".class_requirement", "ANY");
                String materialStr = itemsConfig.getString(path + ".material", "STONE");

                int hp = itemsConfig.getInt(path + ".hp", 0);
                int def = itemsConfig.getInt(path + ".def", 0);
                int str = itemsConfig.getInt(path + ".str", 0);
                int agi = itemsConfig.getInt(path + ".agi", 0);
                int intel = itemsConfig.getInt(path + ".int", 0);
                int dex = itemsConfig.getInt(path + ".dex", 0);

                ItemRarity rarity = ItemRarity.valueOf(rarityStr.toUpperCase());
                Material material = Material.valueOf(materialStr.toUpperCase());

                // Generate a base item (template)
                CustomItem cItem = new CustomItem(
                    numericId,
                    name,
                    rarity,
                    levelReq,
                    classReq,
                    material,
                    hp, def, str, agi, intel, dex
                );

                // Store template by ID
                templatesMap.put(numericId, cItem); // Add template to ID map

            } catch (Exception e) {
                Main.getInstance().getLogger().warning("Failed to load item with key: " + key);
                e.printStackTrace();
            }
        }

        Main.getInstance().getLogger().info("Loaded " + templatesMap.size() + " custom items from items.yml.");
    }


    // Get template by numeric ID
    public CustomItem getTemplateById(int id) {
        return templatesMap.get(id);
    }

    // Add an instance with UUID
    public void addInstance(CustomItem instance) {
        itemsMap.put(instance.getUuid(), instance);
    }

    // Retrieve instance by UUID
    public CustomItem getItemByUUID(UUID uuid) {
        return itemsMap.get(uuid);
    }


    public CustomItem getItemById(int itemId) {
        return templatesMap.get(itemId); // Use templatesMap for lookup by ID
    }

    public CustomItem getCustomItemFromItemStack(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return null; // Item is null or has no meta
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return null;

        // Get the custom item UUID from PersistentDataContainer
        UUID itemUUID = ItemUtil.getItemUUID(itemStack);
        if (itemUUID == null) {
            Main.getInstance().getLogger().info("No custom item UUID found in PersistentDataContainer.");
            return null; // Not a custom item
        }

        // Retrieve the custom item by its UUID
        CustomItem customItem = itemsMap.get(itemUUID);
        if (customItem == null) {
            Main.getInstance().getLogger().info("No matching custom item found for UUID: " + itemUUID);
        } else {
            Main.getInstance().getLogger().info("Custom Item Matched: " + customItem.getBaseName());
        }

        return customItem;
    }

    public Map<UUID, CustomItem> getAllItems() {
        return new HashMap<>(itemsMap);
    }
}
