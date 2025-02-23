package me.nakilex.levelplugin.merchants.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.potions.managers.PotionManager;
import me.nakilex.levelplugin.potions.data.PotionInstance;
import me.nakilex.levelplugin.potions.data.PotionTemplate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class PotionMerchantGUI implements Listener {
    private final Inventory inventory;
    private final Map<Integer, PotionTemplate> potionItems = new HashMap<>();
    private final EconomyManager economyManager;
    private final PotionManager potionManager;
    private final Plugin plugin;
    private int updateTaskId = -1;

    public PotionMerchantGUI(Plugin plugin, FileConfiguration merchantConfig) {
        this.plugin = plugin;
        this.economyManager = Main.getInstance().getEconomyManager();
        this.potionManager = Main.getInstance().getPotionManager();

        String basePath = "merchants.potion_merchant";
        String title = merchantConfig.getString(basePath + ".title", "Potion Merchant");
        int size = merchantConfig.getInt(basePath + ".size", 27);
        this.inventory = Bukkit.createInventory(null, size, title);

        Bukkit.getLogger().info("[PotionMerchantGUI] Initializing Potion Merchant GUI...");
        fillBorder();

        List<?> list = merchantConfig.getList(basePath + ".items");
        if (list != null) {
            Bukkit.getLogger().info("[PotionMerchantGUI] Found " + list.size() + " items in configuration.");
            for (Object obj : list) {
                if (obj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) obj;
                    loadPotionItem(map);
                }
            }
        } else {
            Bukkit.getLogger().warning("[PotionMerchantGUI] No items found in merchants.yml for potion_merchant.");
        }

        for (Map.Entry<Integer, PotionTemplate> entry : potionItems.entrySet()) {
            PotionTemplate potion = entry.getValue();
            Bukkit.getLogger().info("[PotionMerchantGUI] Adding potion '" + potion.getId() + "' to slot " + entry.getKey());
            ItemStack potionItem = createPotionPreview(potion);
            inventory.setItem(entry.getKey(), potionItem);
        }

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

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

    private void loadPotionItem(Map<String, Object> map) {
        try {
            // Debugging: Print raw values from YAML
            Bukkit.getLogger().info("[PotionMerchantGUI] Raw slot value: " + map.get("slot"));
            Bukkit.getLogger().info("[PotionMerchantGUI] Raw item_id value: " + map.get("item_id"));
            Bukkit.getLogger().info("[PotionMerchantGUI] Raw cost value: " + map.get("cost"));

            // Ensure slot & cost are integers, regardless of how YAML reads them
            int slot = (map.get("slot") instanceof Integer) ? (int) map.get("slot") : Integer.parseInt(map.get("slot").toString());
            int cost = (map.get("cost") instanceof Integer) ? (int) map.get("cost") : Integer.parseInt(map.get("cost").toString());
            String potionId = map.get("item_id").toString();

            Bukkit.getLogger().info("[PotionMerchantGUI] Loading potion ID: " + potionId + " at slot: " + slot);

            PotionTemplate potion = potionManager.getTemplate(potionId);
            if (potion != null) {
                potionItems.put(slot, potion);
                Bukkit.getLogger().info("[PotionMerchantGUI] Successfully loaded potion: " + potion.getName() + " at slot: " + slot);
            } else {
                Bukkit.getLogger().warning("[PotionMerchantGUI] Potion with ID '" + potionId + "' not found in PotionManager.");
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[PotionMerchantGUI] Failed to load a potion item: " + e.getMessage());
        }
    }


    public Inventory getInventory() {
        return inventory;
    }

    private ItemStack createPotionPreview(PotionTemplate potion) {
        ItemStack potionItem = new ItemStack(potion.getMaterial());
        ItemMeta meta = potionItem.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + potion.getName());
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Charges: " + ChatColor.YELLOW + potion.getCharges());
        lore.add(ChatColor.GRAY + "Cooldown: " + ChatColor.AQUA + potion.getCooldownSeconds() + "s");
        lore.add("");
        lore.add(ChatColor.GOLD + "Price: " + ChatColor.GREEN + potion.getCooldownSeconds() + " ⛃");
        meta.setLore(lore);
        potionItem.setItemMeta(meta);
        return potionItem;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() != null && event.getInventory().equals(inventory)) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;

            int slot = event.getRawSlot();
            PotionTemplate potion = potionItems.get(slot);
            if (potion == null) return;

            Player player = (Player) event.getWhoClicked();
            int cost = potion.getCooldownSeconds();
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

            PotionInstance instance = new PotionInstance(potion);
            ItemStack purchasedPotion = instance.toItemStack((JavaPlugin) plugin);
            player.getInventory().addItem(purchasedPotion);
            player.sendMessage(ChatColor.GREEN + "You purchased " +
                purchasedPotion.getItemMeta().getDisplayName() + ChatColor.GREEN +
                " for " + cost + " coins.");
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getInventory() != null && event.getInventory().equals(inventory)) {
            Player player = (Player) event.getPlayer();
            updateTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (inventory.getViewers().contains(player)) {
                    updatePriceLore(player);
                }
            }, 0L, 5L).getTaskId();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory() != null && event.getInventory().equals(inventory)) {
            if (updateTaskId != -1) {
                Bukkit.getScheduler().cancelTask(updateTaskId);
                updateTaskId = -1;
            }
        }
    }

    private void updatePriceLore(Player player) {
        int playerCoins = economyManager.getBalance(player);
        for (Map.Entry<Integer, PotionTemplate> entry : potionItems.entrySet()) {
            int slot = entry.getKey();
            PotionTemplate potion = entry.getValue();
            ItemStack stack = inventory.getItem(slot);
            if (stack == null || !stack.hasItemMeta()) continue;

            ItemMeta meta = stack.getItemMeta();
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Charges: " + ChatColor.YELLOW + potion.getCharges());
            lore.add(ChatColor.GRAY + "Cooldown: " + ChatColor.AQUA + potion.getCooldownSeconds() + "s");
            lore.add("");

            if (playerCoins < potion.getCooldownSeconds()) {
                lore.add(ChatColor.GOLD + "Price: " + ChatColor.RED + "✘ " + potion.getCooldownSeconds() + " ⛃");
            } else {
                lore.add(ChatColor.GOLD + "Price: " + ChatColor.GREEN + "✔ " + potion.getCooldownSeconds() + " ⛃");
            }
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
    }
}
