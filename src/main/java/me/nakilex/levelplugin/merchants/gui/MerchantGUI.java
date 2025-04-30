package me.nakilex.levelplugin.merchants.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.data.StatRange;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.merchants.data.MerchantItem;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
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
import org.bukkit.inventory.ItemFlag;
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
            CustomItem tpl = ItemManager.getInstance().getTemplateById(mItem.getItemId());
            if (tpl == null) continue;

            // Base stack + lore from your existing helper (player=null gives gray req stubs)
            ItemStack stack = ItemUtil.createItemStackFromCustomItem(tpl, mItem.getAmount(), null);
            ItemMeta meta = stack.getItemMeta();
            if (meta == null || !meta.hasLore()) {
                inventory.setItem(mItem.getSlot(), stack);
                continue;
            }

            List<String> lore = meta.getLore();

            // 1) Rewrite each stat line to show the RANGE (white numbers)
            for (int i = 0; i < lore.size(); i++) {
                String line = lore.get(i);
                if (line.contains("☠")) {
                    lore.set(i, ChatColor.BLUE  + "☠ " + ChatColor.GRAY + "Strength: "
                        + ChatColor.WHITE + "+" + tpl.getStrRange());
                } else if (line.contains("❤")) {
                    lore.set(i, ChatColor.RED   + "❤ " + ChatColor.GRAY + "Health: "
                        + ChatColor.WHITE + "+" + tpl.getHpRange());
                } else if (line.contains("⛂")) {
                    lore.set(i, ChatColor.GRAY  + "⛂ " + ChatColor.GRAY + "Defence: "
                        + ChatColor.WHITE + "+" + tpl.getDefRange());
                } else if (line.contains("≈")) {
                    lore.set(i, ChatColor.GREEN + "≈ " + ChatColor.GRAY + "Agility: "
                        + ChatColor.WHITE + "+" + tpl.getAgiRange());
                } else if (line.contains("♦")) {
                    lore.set(i, ChatColor.AQUA  + "♦ " + ChatColor.GRAY + "Intelligence: "
                        + ChatColor.WHITE + "+" + tpl.getIntelRange());
                } else if (line.contains("➹")) {
                    lore.set(i, ChatColor.YELLOW+ "➹ " + ChatColor.GRAY + "Dexterity: "
                        + ChatColor.WHITE + "+" + tpl.getDexRange());
                }
            }

            // 2) Remove any old currency stubs, then re‐add fresh stubs
            lore.removeIf(l -> l.equalsIgnoreCase("Price:") || l.startsWith("✘") || l.startsWith("✔"));
            lore.removeIf(l -> l.equalsIgnoreCase("Gems:")  || l.startsWith("✘") || l.startsWith("✔"));

            // 3) Add new price & gems stubs
            lore.add("");                               // spacer
            lore.add(ChatColor.GOLD + "Price:");        // gold header
            lore.add(ChatColor.GOLD + "- "
                + ChatColor.RED   + "✘ "             // red X by default
                + mItem.getCost()
                + " "
                + ChatColor.GOLD + "⛃");             // gold coin icon

            if (mItem.getGems() > 0) {
                lore.add(ChatColor.GOLD + "Gems:");
                lore.add(ChatColor.GRAY + "- "
                    + ChatColor.RED   + "✘ "
                    + mItem.getGems()
                    + " "
                    + ChatColor.LIGHT_PURPLE + "✦");
            }

            meta.setLore(lore);
            stack.setItemMeta(meta);
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
            int slot   = Integer.parseInt(map.get("slot").toString());
            int itemId = Integer.parseInt(map.get("item_id").toString());
            int amount = Integer.parseInt(map.get("amount").toString());
            int cost   = Integer.parseInt(map.get("cost").toString());
            int gems   = map.containsKey("gems")
                ? Integer.parseInt(map.get("gems").toString())
                : 0;
            merchantItems.put(slot,
                new MerchantItem(slot, itemId, amount, cost, gems));
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load a merchant item: " + e.getMessage());
        }
    }

    private void loadMerchantItem(ConfigurationSection cs) {
        try {
            int slot   = cs.getInt("slot");
            int itemId = cs.getInt("item_id");
            int amount = cs.getInt("amount");
            int cost   = cs.getInt("cost");
            int gems   = cs.contains("gems") ? cs.getInt("gems") : 0;
            merchantItems.put(slot,
                new MerchantItem(slot, itemId, amount, cost, gems));
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
        int coins = economyManager.getBalance(player);

        for (MerchantItem mItem : merchantItems.values()) {
            ItemStack stack = inventory.getItem(mItem.getSlot());
            if (stack == null || !stack.hasItemMeta()) continue;

            ItemMeta meta = stack.getItemMeta();
            List<String> lore = new ArrayList<>(meta.getLore());

            // Find the gold “Price:” header
            int headerIdx = lore.indexOf(ChatColor.GOLD + "Price:");
            if (headerIdx == -1) continue;  // shouldn’t happen

            // Build the new price line
            boolean afford = coins >= mItem.getCost();
            String priceLine = ChatColor.GOLD + "- "
                + (afford
                ? ChatColor.GREEN + "✔ "
                : ChatColor.RED   + "✘ ")
                + mItem.getCost()
                + " "
                + ChatColor.GOLD + "⛃";

            // Replace the line immediately after the header
            int lineIdx = headerIdx + 1;
            if (lineIdx < lore.size()) {
                lore.set(lineIdx, priceLine);
            } else {
                lore.add(priceLine);
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

    private void updateMerchantTooltips(Player player) {
        int lvl       = StatsManager.getInstance().getLevel(player);
        PlayerClass cls   = StatsManager.getInstance()
            .getPlayerStats(player.getUniqueId()).playerClass;
        int coins     = economyManager.getBalance(player);
        int totalGems = Main.getInstance().getGemsManager().getTotalUnits(player);

        for (MerchantItem mItem : merchantItems.values()) {
            ItemStack stack = inventory.getItem(mItem.getSlot());
            if (stack == null || !stack.hasItemMeta()) continue;

            ItemMeta meta = stack.getItemMeta();
            List<String> lore = new ArrayList<>(meta.getLore());
            CustomItem tpl = ItemManager.getInstance().getTemplateById(mItem.getItemId());

            // ── 1) Level Requirement ─────────────────────────
            int lvlIdx = -1;
            for (int i = 0; i < lore.size(); i++) {
                if (lore.get(i).contains("Level Requirement:")) {
                    lvlIdx = i;
                    break;
                }
            }
            if (lvlIdx != -1) {
                boolean ok = lvl >= tpl.getLevelRequirement();
                lore.set(lvlIdx,
                    (ok ? ChatColor.GREEN + "✔ " : ChatColor.RED + "✘ ")
                        + ChatColor.GRAY + "Level Requirement: "
                        + ChatColor.WHITE + tpl.getLevelRequirement()
                );
            }

            // ── 2) Class Requirement ─────────────────────────
            int clsIdx = -1;
            for (int i = 0; i < lore.size(); i++) {
                if (lore.get(i).contains("Class Requirement:")) {
                    clsIdx = i;
                    break;
                }
            }
            if (clsIdx != -1 && !tpl.getClassRequirement().equalsIgnoreCase("ANY")) {
                boolean ok = cls.name().equalsIgnoreCase(tpl.getClassRequirement());
                String cap = tpl.getClassRequirement().substring(0,1).toUpperCase()
                    + tpl.getClassRequirement().substring(1).toLowerCase();
                lore.set(clsIdx,
                    (ok ? ChatColor.GREEN + "✔ " : ChatColor.RED + "✘ ")
                        + ChatColor.GRAY + "Class Requirement: "
                        + ChatColor.WHITE + cap
                );
            }

            // ── 3) Coin Price ────────────────────────────────
            int priceHdr = lore.indexOf(ChatColor.GOLD + "Price:");
            if (priceHdr != -1 && priceHdr + 1 < lore.size()) {
                boolean afford = coins >= mItem.getCost();
                lore.set(priceHdr + 1,
                    ChatColor.GRAY + "- "
                        + (afford ? ChatColor.GREEN + "✔ " : ChatColor.RED + "✘ ")
                        + mItem.getCost()
                        + " "
                        + ChatColor.GOLD + "⛃"
                );
            }

            // ── 4) Gems Price (if any) ───────────────────────
            if (mItem.getGems() > 0) {
                int gemsHdr = lore.indexOf(ChatColor.GOLD + "Gems:");
                if (gemsHdr != -1 && gemsHdr + 1 < lore.size()) {
                    boolean afford = totalGems >= mItem.getGems();
                    lore.set(gemsHdr + 1,
                        ChatColor.GRAY + "- "
                            + (afford ? ChatColor.GREEN + "✔ " : ChatColor.RED + "✘ ")
                            + mItem.getGems()
                            + " "
                            + ChatColor.LIGHT_PURPLE + "✦"
                    );
                }
            }

            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
    }

    // ─── Replace your onInventoryOpen with this ──────────────────────────────
    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player)) return;
        if (!e.getInventory().equals(inventory)) return;

        Player p = (Player)e.getPlayer();

        // Run it immediately once
        updateMerchantTooltips(p);

        // Schedule it every 5 ticks
        updateTaskId = Bukkit.getScheduler()
            .runTaskTimer(plugin, () -> {
                if (inventory.getViewers().contains(p)) {
                    updateMerchantTooltips(p);
                }
            }, 0L, 5L)
            .getTaskId();
    }

    // ─── And make sure your close handler stays as is ────────────────────────
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!e.getInventory().equals(inventory)) return;
        if (updateTaskId != -1) {
            Bukkit.getScheduler().cancelTask(updateTaskId);
            updateTaskId = -1;
        }
    }

}
