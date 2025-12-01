package me.nakilex.levelplugin.merchants.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.potions.managers.PotionManager;
import me.nakilex.levelplugin.potions.data.PotionInstance;
import me.nakilex.levelplugin.potions.data.PotionTemplate;
import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
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

import java.io.InputStream;
import java.util.*;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import static me.nakilex.levelplugin.utils.ChatMessageUtil.send;

public class PotionMerchantGUI implements Listener {
    private final Inventory inventory;
    private final Map<Integer, PotionTemplate> potionItems = new HashMap<>();
    private final Map<Integer, Integer> potionCosts = new HashMap<>();
    private final EconomyManager economyManager;
    private final PotionManager potionManager;
    private final Plugin plugin;
    private int updateTaskId = -1;

    public PotionMerchantGUI(Plugin plugin, FileConfiguration merchantConfig) {
        this.plugin = plugin;
        this.economyManager = Main.getInstance().getEconomyManager();
        this.potionManager = Main.getInstance().getPotionManager();

        String basePath;
        if (merchantConfig.getConfigurationSection("merchants.potion_merchant") != null
                || merchantConfig.contains("merchants.potion_merchant")) {
            basePath = "merchants.potion_merchant";
        } else {
            basePath = "potion_merchant";
        }
        String title = merchantConfig.getString(basePath + ".title", "Potion Merchant");
        int size = merchantConfig.getInt(basePath + ".size", 27);
        this.inventory = GuiBuilder.create(size, title)
                .filler(Material.BLACK_STAINED_GLASS_PANE)
                .fillEmptySlots(false)
                .border()
                .build();

        Bukkit.getLogger().info("[PotionMerchantGUI] Initializing Potion Merchant GUI...");

        List<?> list = merchantConfig.getList(basePath + ".items");
        if (list == null && basePath.startsWith("merchants.")) {
            // Fallback in case the caller passed an already-scoped config
            list = merchantConfig.getList(basePath.substring("merchants.".length()) + ".items");
        }
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

        // If the live merchants.yml is missing the potion shop entries, merge defaults from the bundled resource
        // so the GUI never opens empty.
        if (potionItems.isEmpty()) {
            copyDefaultsFromResource(plugin, basePath);
        }
        if (potionItems.isEmpty()) {
            seedFromPotionTemplates();
        }

        for (Map.Entry<Integer, PotionTemplate> entry : potionItems.entrySet()) {
            PotionTemplate potion = entry.getValue();
            Bukkit.getLogger().info("[PotionMerchantGUI] Adding potion '" + potion.getId() + "' to slot " + entry.getKey());
            ItemStack potionItem = createPotionPreview(entry.getKey(), potion);
            inventory.setItem(entry.getKey(), potionItem);
        }

        Bukkit.getPluginManager().registerEvents(this, plugin);
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
                potionCosts.put(slot, cost);
                Bukkit.getLogger().info("[PotionMerchantGUI] Successfully loaded potion: " + potion.getName() + " at slot: " + slot);
            } else {
                Bukkit.getLogger().warning("[PotionMerchantGUI] Potion with ID '" + potionId + "' not found in PotionManager.");
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[PotionMerchantGUI] Failed to load a potion item: " + e.getMessage());
        }
    }

    private void copyDefaultsFromResource(Plugin plugin, String basePath) {
        try (InputStream in = plugin.getResource("merchants.yml")) {
            if (in == null) {
                Bukkit.getLogger().warning("[PotionMerchantGUI] No bundled merchants.yml found; cannot seed potion shop.");
                return;
            }
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(in));
            List<?> fallback = defaults.getList(basePath + ".items");
            if (fallback == null || fallback.isEmpty()) {
                Bukkit.getLogger().warning("[PotionMerchantGUI] Bundled merchants.yml missing potion_merchant items.");
                return;
            }
            for (Object obj : fallback) {
                if (obj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) obj;
                    loadPotionItem(map);
                }
            }
            Bukkit.getLogger().info("[PotionMerchantGUI] Seeded potion shop items from bundled defaults (" + potionItems.size() + " items).");
        } catch (Exception ex) {
            Bukkit.getLogger().warning("[PotionMerchantGUI] Failed to seed potion merchant defaults: " + ex.getMessage());
        }
    }

    /**
     * As a last resort, populate the shop directly from loaded potion templates so the GUI never opens empty.
     */
    private void seedFromPotionTemplates() {
        List<PotionTemplate> templates = new ArrayList<>(potionManager.getAllTemplates());
        if (templates.isEmpty()) {
            Bukkit.getLogger().warning("[PotionMerchantGUI] PotionManager returned no templates; cannot seed shop.");
            return;
        }
        templates.sort(Comparator.comparingInt(PotionTemplate::getTier).thenComparing(PotionTemplate::getId));

        int slot = 10;
        for (PotionTemplate template : templates) {
            if (slot >= inventory.getSize()) break;
            potionItems.put(slot, template);
            potionCosts.put(slot, Math.max(50, template.getCooldownSeconds()));
            slot++;
            if (slot == 13) slot = 14; // skip spacer between healing/mana if present
        }
        Bukkit.getLogger().info("[PotionMerchantGUI] Seeded potion shop from PotionManager templates (" + potionItems.size() + " items).");
    }


    public Inventory getInventory() {
        return inventory;
    }

    private ItemStack createPotionPreview(int slot, PotionTemplate potion) {
        PotionInstance instance = new PotionInstance(potion);
        ItemStack potionItem = instance.toItemStack((JavaPlugin) plugin);
        ItemMeta meta = potionItem.getItemMeta();
        List<String> lore = meta != null && meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.removeIf(line -> ChatColor.stripColor(line).startsWith("Left-click")
                || ChatColor.stripColor(line).startsWith("Right-click"));
        lore.add("");
        int cost = potionCosts.getOrDefault(slot, potion.getCooldownSeconds());
        lore.add(ChatColor.GOLD + "Price: " + ChatColor.GREEN + cost + " <glyph:coins_icon>");
        lore.addAll(TooltipUtil.clickInstructions("to purchase", null));
        if (meta != null) {
            String name = meta.hasDisplayName() ? meta.getDisplayName()
                    : ChatColor.translateAlternateColorCodes('&', potion.getName());
            meta.setDisplayName(name);
            meta.setLore(lore);
            potionItem.setItemMeta(meta);
        }
        ItemRarity rarity = ItemRarity.fromTier(potion.getTier());
        ItemUtil.applyRarityTooltipStyle(potionItem, rarity);
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
            int cost = potionCosts.getOrDefault(slot, potion.getCooldownSeconds());
            int balance = economyManager.getBalance(player);

            if (balance < cost) {
                send(player, MessageType.ERROR, "You don't have enough coins!");
                return;
            }

            if (player.getInventory().firstEmpty() == -1) {
                player.sendTitle(ChatColor.RED + "Inventory full!", "", 10, 70, 20);
                return;
            }

            try {
                economyManager.deductCoins(player, cost);
            } catch (IllegalArgumentException ex) {
                send(player, MessageType.ERROR, "Transaction failed: " + ex.getMessage());
                return;
            }

            PotionInstance instance = new PotionInstance(potion);
            ItemStack purchasedPotion = instance.toItemStack((JavaPlugin) plugin);
            player.getInventory().addItem(purchasedPotion);
            send(player, MessageType.SUCCESS, "You purchased " +
                purchasedPotion.getItemMeta().getDisplayName() + ChatColor.GREEN +
                "for " + cost + " coins.");
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
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.removeIf(line -> ChatColor.stripColor(line).startsWith("Price:"));
            lore.removeIf(line -> line.isEmpty());
            lore.add("");
            int cost = potionCosts.getOrDefault(slot, potion.getCooldownSeconds());
            if (playerCoins < cost) {
                lore.add(ChatColor.GOLD + "Price: " + ChatColor.RED + "✘ " + cost + " <glyph:coins_icon>");
            } else {
                lore.add(ChatColor.GOLD + "Price: " + ChatColor.GREEN + "✔ " + cost + " <glyph:coins_icon>");
            }
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
    }
}
