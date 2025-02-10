package me.nakilex.levelplugin.merchants.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.merchants.data.MerchantItem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MerchantGUI implements Listener {
    private final Inventory inventory;
    private final Map<Integer, MerchantItem> merchantItems = new HashMap<>();
    private final EconomyManager economyManager;
    private final Plugin plugin;

    /**
     * @param plugin         Your plugin instance
     * @param merchantConfig The loaded merchants.yml configuration
     * @param merchantName   The merchant name (e.g. "rogue_merchant")
     */
    public MerchantGUI(Plugin plugin, FileConfiguration merchantConfig, String merchantName) {
        this.plugin = plugin;
        // Get a reference to the EconomyManager (assumes Main holds a reference)
        this.economyManager = Main.getInstance().getEconomyManager();

        // Load merchant section from config
        String basePath = "merchants." + merchantName;
        String title = merchantConfig.getString(basePath + ".title", "Merchant");
        int size = merchantConfig.getInt(basePath + ".size", 27);
        this.inventory = Bukkit.createInventory(null, size, title);

        // Expect the merchant items to be defined as a list under merchants.<name>.items
        List<?> list = merchantConfig.getList(basePath + ".items");
        if (list != null) {
            for (Object obj : list) {
                if (obj instanceof ConfigurationSection) {
                    // (This branch would work if the items were defined as sections.)
                    ConfigurationSection cs = (ConfigurationSection) obj;
                    loadMerchantItem(cs);
                } else if (obj instanceof Map) {
                    // In most cases, the YAML list is loaded as a List of Map objects.
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) obj;
                    loadMerchantItem(map);
                }
            }
        }

        // For each merchant item, build the corresponding ItemStack
        for (MerchantItem mItem : merchantItems.values()) {
            // Get the custom item template from your ItemManager (by id)
            CustomItem template = ItemManager.getInstance().getTemplateById(mItem.getItemId());
            if (template != null) {
                // Use your utility to create an ItemStack from the CustomItem
                ItemStack stack = ItemUtil.createItemStackFromCustomItem(template, mItem.getAmount());
                // Append the cost to the item lore (as the last line)
                if (stack.hasItemMeta()) {
                    ItemMeta meta = stack.getItemMeta();
                    List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                    lore.add(ChatColor.GOLD + "Cost: " + ChatColor.WHITE + mItem.getCost() + " coins");
                    meta.setLore(lore);
                    stack.setItemMeta(meta);
                }
                inventory.setItem(mItem.getSlot(), stack);
            }
        }
        // Register the listener so that clicks in this inventory are handled
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private void loadMerchantItem(Map<String, Object> map) {
        try {
            int slot = (Integer) map.get("slot");
            int itemId = (Integer) map.get("item_id");
            int amount = (Integer) map.get("amount");
            int cost = (Integer) map.get("cost");
            MerchantItem mItem = new MerchantItem(slot, itemId, amount, cost);
            merchantItems.put(slot, mItem);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load a merchant item: " + e.getMessage());
        }
    }

    private void loadMerchantItem(ConfigurationSection cs) {
        try {
            int slot = cs.getInt("slot");
            int itemId = cs.getInt("item_id");
            int amount = cs.getInt("amount");
            int cost = cs.getInt("cost");
            MerchantItem mItem = new MerchantItem(slot, itemId, amount, cost);
            merchantItems.put(slot, mItem);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load a merchant item: " + e.getMessage());
        }
    }

    public Inventory getInventory() {
        return inventory;
    }

    // Handle clicks in the merchant GUI
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Check if the clicked inventory is our merchant GUI
        if (event.getInventory() != null && event.getInventory().equals(inventory)) {
            event.setCancelled(true); // prevent item moving
            if (event.getCurrentItem() == null) {
                return;
            }
            int slot = event.getRawSlot();
            MerchantItem mItem = merchantItems.get(slot);
            if (mItem == null) {
                return;
            }
            Player player = (Player) event.getWhoClicked();
            int cost = mItem.getCost();
            // Check if the player has enough coins
            int balance = economyManager.getBalance(player);
            if (balance < cost) {
                player.sendMessage(ChatColor.RED + "You don't have enough coins!");
                return;
            }
            // Deduct coins (your EconomyManager will handle persistence)
            try {
                economyManager.deductCoins(player, cost);
            } catch (IllegalArgumentException ex) {
                player.sendMessage(ChatColor.RED + "Transaction failed: " + ex.getMessage());
                return;
            }
            // Create a new ItemStack to give to the player
            CustomItem template = ItemManager.getInstance().getTemplateById(mItem.getItemId());
            if (template != null) {
                ItemStack purchasedItem = ItemUtil.createItemStackFromCustomItem(template, mItem.getAmount());
                player.getInventory().addItem(purchasedItem);
                player.sendMessage(ChatColor.GREEN + "You purchased " + purchasedItem.getItemMeta().getDisplayName() +
                    ChatColor.GREEN + " for " + cost + " coins.");
            }
        }
    }
}
