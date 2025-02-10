package me.nakilex.levelplugin.merchants.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.merchants.data.MerchantItem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
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

    // Task ID for our scheduled update (per player)
    private int updateTaskId = -1;

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

        // Fill the border with a placeholder
        fillBorder();

        // Load merchant items from config
        List<?> list = merchantConfig.getList(basePath + ".items");
        if (list != null) {
            for (Object obj : list) {
                if (obj instanceof ConfigurationSection) {
                    ConfigurationSection cs = (ConfigurationSection) obj;
                    loadMerchantItem(cs);
                } else if (obj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) obj;
                    loadMerchantItem(map);
                }
            }
        }

        // For each merchant item, build the corresponding ItemStack with a default price line.
        for (MerchantItem mItem : merchantItems.values()) {
            CustomItem template = ItemManager.getInstance().getTemplateById(mItem.getItemId());
            if (template != null) {
                ItemStack stack = ItemUtil.createItemStackFromCustomItem(template, mItem.getAmount());
                if (stack.hasItemMeta()) {
                    ItemMeta meta = stack.getItemMeta();
                    List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                    lore.add(ChatColor.GRAY + "Price:");
                    // Default: assume not enough coins (this will be updated when the inventory is open)
                    lore.add(ChatColor.RED + "- ✘ " + mItem.getCost() + " ⛃");
                    meta.setLore(lore);
                    stack.setItemMeta(meta);
                }
                inventory.setItem(mItem.getSlot(), stack);
            }
        }
        // Register events for this GUI
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Fills the border (outer rows/columns) of the inventory with a placeholder (black stained glass).
     */
    private void fillBorder() {
        int size = inventory.getSize();
        int columns = 9; // Typical chest width
        int rows = size / columns;
        ItemStack placeholder = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = placeholder.getItemMeta();
        meta.setDisplayName(" ");
        placeholder.setItemMeta(meta);

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                int slot = row * columns + col;
                if (row == 0 || row == rows - 1 || col == 0 || col == columns - 1) {
                    inventory.setItem(slot, placeholder);
                }
            }
        }
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

    /**
     * Update the lore of each merchant item based on the player's current coins.
     */
    private void updatePriceLore(Player player) {
        int playerCoins = economyManager.getBalance(player);
        for (MerchantItem mItem : merchantItems.values()) {
            int slot = mItem.getSlot();
            ItemStack stack = inventory.getItem(slot);
            if (stack == null || !stack.hasItemMeta())
                continue;
            ItemMeta meta = stack.getItemMeta();
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

            // Remove any previous "Price:" block (we assume the lines containing "Price:" or a dash with a symbol)
            lore.removeIf(line -> line.contains("Price:") || line.contains("✘") || line.contains("✔"));

            // Append the new price info:
            lore.add(ChatColor.GOLD + "Price:");
            if (playerCoins < mItem.getCost()) {
                lore.add(ChatColor.GRAY + "- " + ChatColor.RED + "✘ " + mItem.getCost() + " " + ChatColor.GOLD + "⛃");
            } else {
                lore.add(ChatColor.GRAY + "- " + ChatColor.GREEN + "✔ " + mItem.getCost() + " " + ChatColor.GOLD + "⛃");
            }
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
    }


    // Handle clicks so that items aren’t taken and purchases are processed.
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() != null && event.getInventory().equals(inventory)) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null)
                return;
            int slot = event.getRawSlot();
            MerchantItem mItem = merchantItems.get(slot);
            if (mItem == null)
                return;
            Player player = (Player) event.getWhoClicked();
            int cost = mItem.getCost();
            int balance = economyManager.getBalance(player);
            if (balance < cost) {
                player.sendMessage(ChatColor.RED + "You don't have enough coins!");
                return;
            }
            try {
                economyManager.deductCoins(player, cost);
            } catch (IllegalArgumentException ex) {
                player.sendMessage(ChatColor.RED + "Transaction failed: " + ex.getMessage());
                return;
            }
            CustomItem template = ItemManager.getInstance().getTemplateById(mItem.getItemId());
            if (template != null) {
                ItemStack purchasedItem = ItemUtil.createItemStackFromCustomItem(template, mItem.getAmount());
                player.getInventory().addItem(purchasedItem);
                player.sendMessage(ChatColor.GREEN + "You purchased " +
                    purchasedItem.getItemMeta().getDisplayName() + ChatColor.GREEN +
                    " for " + cost + " coins.");
            }
        }
    }

    /**
     * When a player opens this inventory, schedule a repeating task to update the price lore.
     */
    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getInventory() != null && event.getInventory().equals(inventory)) {
            Player player = (Player) event.getPlayer();
            // Schedule a task to update the lore every 5 ticks.
            updateTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                // Ensure the player is still viewing this inventory before updating.
                if (inventory.getViewers().contains(player)) {
                    updatePriceLore(player);
                }
            }, 0L, 5L).getTaskId();
        }
    }

    /**
     * Cancel the repeating update task when the inventory is closed.
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory() != null && event.getInventory().equals(inventory)) {
            if (updateTaskId != -1) {
                Bukkit.getScheduler().cancelTask(updateTaskId);
                updateTaskId = -1;
            }
        }
    }
}
