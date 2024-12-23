package me.nakilex.levelplugin.managers;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.CustomItem;
import me.nakilex.levelplugin.items.ItemRarity;
import me.nakilex.levelplugin.items.ItemUtil;
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
import java.util.HashMap;
import java.util.Map;

import static me.nakilex.levelplugin.items.ItemUtil.ITEM_ID_KEY;

public class ItemManager {

    private static ItemManager instance;

    public static ItemManager getInstance() {
        return instance;
    }

    private final Map<Integer, CustomItem> itemsMap = new HashMap<>();
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
        itemsMap.clear();

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

                CustomItem cItem = new CustomItem(
                    numericId,
                    name,
                    rarity,
                    levelReq,
                    classReq,
                    material,
                    hp, def, str, agi, intel, dex
                );

                itemsMap.put(numericId, cItem);

            } catch (Exception e) {
                Main.getInstance().getLogger().warning("Failed to load item with key: " + key);
                e.printStackTrace();
            }
        }

        Main.getInstance().getLogger().info("Loaded " + itemsMap.size() + " custom items from items.yml.");
    }

    public CustomItem getItemById(int itemId) {
        return itemsMap.get(itemId);
    }

    public CustomItem getCustomItemFromItemStack(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return null; // Item is null or has no meta
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return null;

        // Get the custom item ID from PersistentDataContainer
        int itemId = meta.getPersistentDataContainer().getOrDefault(ITEM_ID_KEY, PersistentDataType.INTEGER, -1);
        if (itemId == -1) {
            Main.getInstance().getLogger().info("No custom item ID found in PersistentDataContainer.");
            return null; // Not a custom item
        }

        // Retrieve the custom item by its ID
        CustomItem customItem = itemsMap.get(itemId);
        if (customItem == null) {
            Main.getInstance().getLogger().info("No matching custom item found for ID: " + itemId);
        } else {
            Main.getInstance().getLogger().info("Custom Item Matched: " + customItem.getBaseName());
        }

        return customItem;
    }

    // In ItemManager.updateItem(...) we remove or comment out the increaseStats call:
    public ItemStack updateItem(ItemStack itemStack, CustomItem customItem, int upgradeLevel) {
        // We still update the persistent data for the new level:
        customItem.setUpgradeLevel(upgradeLevel);

        // REMOVE OR COMMENT OUT THIS LINE:
        // customItem.increaseStats();

        // Just update PDC so the item’s "upgrade level" is stored
        ItemUtil.updateUpgradeLevel(itemStack, upgradeLevel);

        // And rebuild the item stack (name/lore) from the customItem
        return ItemUtil.createItemStackFromCustomItem(customItem, itemStack.getAmount());
    }


    public Map<Integer, CustomItem> getAllItems() {
        return new HashMap<>(itemsMap);
    }
}
