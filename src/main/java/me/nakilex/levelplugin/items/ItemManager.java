package me.nakilex.levelplugin.items;

import me.nakilex.levelplugin.Main;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        if (!meta.hasDisplayName()) {
            return null; // No display name, not a custom item
        }

        String itemDisplayName = ChatColor.stripColor(meta.getDisplayName());
        Main.getInstance().getLogger().info("Item Display Name (Stripped): " + itemDisplayName);

        for (CustomItem customItem : itemsMap.values()) {
            String customItemName = ChatColor.stripColor(customItem.getName());
            if (itemDisplayName.startsWith(customItemName)) { // Support stars after name
                Main.getInstance().getLogger().info("Custom Item Matched: " + customItem.getName());
                return customItem;
            }
        }

        Main.getInstance().getLogger().info("No matching custom item found.");
        return null;
    }

    public ItemStack updateItem(ItemStack itemStack, CustomItem customItem, int upgradeLevel) {
        customItem.setUpgradeLevel(upgradeLevel); // Update the level
        customItem.increaseStats(); // Recalculate stats
        return ItemUtil.createItemStackFromCustomItem(customItem, itemStack.getAmount()); // Regenerate the item
    }

}
