package me.nakilex.levelplugin.merchants.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.data.StatRange;
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
import java.util.UUID;

public class MerchantGUI implements Listener {
    private final Inventory inventory;
    private final Map<Integer, MerchantItem> merchantItems = new HashMap<>();
    private final EconomyManager economyManager;
    private final Plugin plugin;
    private int updateTaskId = -1;

    /**
     * @param plugin         Your plugin instance
     * @param merchantConfig The loaded merchants.yml configuration
     * @param merchantName   The merchant name (e.g. "rogue_merchant")
     */
    public MerchantGUI(Plugin plugin, FileConfiguration merchantConfig, String merchantName) {
        this.plugin = plugin;
        this.economyManager = Main.getInstance().getEconomyManager();

        String basePath = "merchants." + merchantName;
        String title = merchantConfig.getString(basePath + ".title", "Merchant");
        int size = merchantConfig.getInt(basePath + ".size", 27);
        this.inventory = Bukkit.createInventory(null, size, title);

        fillBorder();

        // Load merchant-items definitions
        List<?> list = merchantConfig.getList(basePath + ".items");
        if (list != null) {
            for (Object obj : list) {
                if (obj instanceof ConfigurationSection) {
                    loadMerchantItem((ConfigurationSection) obj);
                } else if (obj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) obj;
                    loadMerchantItem(map);
                }
            }
        }

        // Now populate slots with stats‐range + price lore
        populateMerchantItems();

        // Register events
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Build each slot’s ItemStack using the template’s StatRange values
     * and a default “unaffordable” price line.
     */
    private void populateMerchantItems() {
        for (MerchantItem mItem : merchantItems.values()) {
            CustomItem template = ItemManager.getInstance().getTemplateById(mItem.getItemId());
            if (template == null) continue;

            // 1) Build the default ItemStack + lore
            ItemStack stack = ItemUtil.createItemStackFromCustomItem(template, mItem.getAmount(), null);
            ItemMeta meta = stack.getItemMeta();
            if (meta == null || !meta.hasLore()) {
                inventory.setItem(mItem.getSlot(), stack);
                continue;
            }

            // 2) Rewrite the stat lines in-place
            List<String> lore = meta.getLore();
            for (int i = 0; i < lore.size(); i++) {
                String line = lore.get(i);

                // Strength
                if (line.contains("☠")) {
                    lore.set(i,
                        ChatColor.BLUE  + "☠ " + ChatColor.GRAY + "Strength: " +
                            ChatColor.WHITE + "+" + template.getStrRange()
                    );
                }
                // Health
                else if (line.contains("❤")) {
                    lore.set(i,
                        ChatColor.RED   + "❤ " + ChatColor.GRAY + "Health: " +
                            ChatColor.RED   + "+" + template.getHpRange()
                    );
                }
                // Defence
                else if (line.contains("⛂")) {
                    lore.set(i,
                        ChatColor.GRAY  + "⛂ " + ChatColor.GRAY + "Defence: " +
                            ChatColor.WHITE + "+" + template.getDefRange()
                    );
                }
                // Agility
                else if (line.contains("≈")) {
                    lore.set(i,
                        ChatColor.GREEN + "≈ " + ChatColor.GRAY + "Agility: " +
                            ChatColor.WHITE + "+" + template.getAgiRange()
                    );
                }
                // Intelligence
                else if (line.contains("♦")) {
                    lore.set(i,
                        ChatColor.AQUA  + "♦ " + ChatColor.GRAY + "Intelligence: " +
                            ChatColor.WHITE + "+" + template.getIntelRange()
                    );
                }
                // Dexterity
                else if (line.contains("➹")) {
                    lore.set(i,
                        ChatColor.YELLOW+ "➹ " + ChatColor.GRAY + "Dexterity: " +
                            ChatColor.WHITE + "+" + template.getDexRange()
                    );
                }
            }

            // 3) Remove any old price block, then append fresh price lines
            lore.removeIf(l -> l.contains("Price:") || l.contains("✘") || l.contains("✔"));
            lore.add("");
            lore.add(ChatColor.GRAY + "Price:");
            lore.add(ChatColor.RED  + "- ✘ " + mItem.getCost() + " " + ChatColor.GOLD + "⛃");

            meta.setLore(lore);
            stack.setItemMeta(meta);

            // 4) Put it into the GUI
            inventory.setItem(mItem.getSlot(), stack);
        }
    }


    /**
     * Fills the border (outer rows/columns) of the inventory with a placeholder.
     */
    private void fillBorder() {
        int size = inventory.getSize();
        int columns = 9;
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
            int slot   = (Integer) map.get("slot");
            int itemId = (Integer) map.get("item_id");
            int amount = (Integer) map.get("amount");
            int cost   = (Integer) map.get("cost");
            merchantItems.put(slot, new MerchantItem(slot, itemId, amount, cost));
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load a merchant item: " + e.getMessage());
        }
    }

    private void loadMerchantItem(ConfigurationSection cs) {
        try {
            int slot    = cs.getInt("slot");
            int itemId  = cs.getInt("item_id");
            int amount  = cs.getInt("amount");
            int cost    = cs.getInt("cost");
            merchantItems.put(slot, new MerchantItem(slot, itemId, amount, cost));
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
            if (stack == null || !stack.hasItemMeta()) continue;

            ItemMeta meta = stack.getItemMeta();
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            // Remove old price lines
            lore.removeIf(line -> line.contains("Price:") || line.contains("✘") || line.contains("✔"));

            lore.add(ChatColor.GRAY + "Price:");
            if (playerCoins < mItem.getCost()) {
                lore.add(ChatColor.GRAY + "- " + ChatColor.RED + "✘ " +
                    mItem.getCost() + " " + ChatColor.GOLD + "⛃");
            } else {
                lore.add(ChatColor.GRAY + "- " + ChatColor.GREEN + "✔ " +
                    mItem.getCost() + " " + ChatColor.GOLD + "⛃");
            }

            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() != null && event.getInventory().equals(inventory)) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;

            int slot = event.getRawSlot();
            MerchantItem mItem = merchantItems.get(slot);
            if (mItem == null) return;

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
                CustomItem newInstance = new CustomItem(
                    template.getId(),
                    template.getBaseName(),
                    template.getRarity(),
                    template.getLevelRequirement(),
                    template.getClassRequirement(),
                    template.getMaterial(),
                    template.getHpRange(),
                    template.getDefRange(),
                    template.getStrRange(),
                    template.getAgiRange(),
                    template.getIntelRange(),
                    template.getDexRange()
                );
                ItemManager.getInstance().addInstance(newInstance);
                ItemStack purchasedItem = ItemUtil.createItemStackFromCustomItem(newInstance, mItem.getAmount(), player);
                player.getInventory().addItem(purchasedItem);
                player.sendMessage(ChatColor.GREEN +
                    "You purchased " +
                    purchasedItem.getItemMeta().getDisplayName() +
                    ChatColor.GREEN + " for " + cost + " coins.");
            }
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getInventory() != null && event.getInventory().equals(inventory)) {
            Player player = (Player) event.getPlayer();
            updateTaskId = Bukkit.getScheduler()
                .runTaskTimer(plugin, () -> {
                    if (inventory.getViewers().contains(player)) {
                        updatePriceLore(player);
                    }
                }, 0L, 5L)
                .getTaskId();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory() != null && event.getInventory().equals(inventory)
            && updateTaskId != -1) {
            Bukkit.getScheduler().cancelTask(updateTaskId);
            updateTaskId = -1;
        }
    }
}
