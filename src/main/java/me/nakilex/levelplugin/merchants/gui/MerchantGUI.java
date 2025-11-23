package me.nakilex.levelplugin.merchants.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.data.StatRange;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.tools.CustomTool;
import me.nakilex.levelplugin.items.tools.ToolManager;
import me.nakilex.levelplugin.items.tools.ToolTier;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.merchants.data.MerchantItem;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.guild.GuildManager;
import me.nakilex.levelplugin.guild.TownPerk;
import me.nakilex.levelplugin.guild.TownPerkManager;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
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

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import static me.nakilex.levelplugin.utils.ChatMessageUtil.send;

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
        this.inventory = GuiBuilder.create(size, title)
                .filler(Material.BLACK_STAINED_GLASS_PANE)
                .fillEmptySlots(false)
                .border()
                .build();

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
            if (mItem.isTool()) {
                CustomTool tool = mItem.getTool();
                if (tool == null) {
                    continue;
                }
                ItemStack stack = new ItemStack(tool.getMaterial(), mItem.getAmount());
                ItemMeta meta = stack.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.GREEN + tool.getName());
                    stack.setItemMeta(meta);
                }
                ItemUtil.updateCustomToolTooltip(stack, null);
                meta = stack.getItemMeta();
                if (meta != null) {
                    List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                    addPriceStub(lore, mItem);
                    meta.setLore(lore);
                    meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
                    stack.setItemMeta(meta);
                }
                inventory.setItem(mItem.getSlot(), stack);
            } else {
                CustomItem tpl = ItemManager.getInstance().getTemplateById(mItem.getItemId());
                if (tpl == null) continue;

                // Base stack using the template's rolled stats.
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
                    if (line.contains("<glyph:str>")) {
                        lore.set(i, GuiUtil.formatStatName(StatsManager.StatType.STR) + ": "
                                + ChatColor.GREEN + "+" + tpl.getStrRange());
                    } else if (line.contains("<glyph:vit>")) {
                        lore.set(i, GuiUtil.formatStatName(StatsManager.StatType.VIT) + ": "
                                + ChatColor.RED + "+" + tpl.getHpRange());
                    } else if (line.contains("⛂")) {
                        lore.set(i, ChatColor.GRAY  + "⛂ " + ChatColor.GRAY + "Defence: "
                                + ChatColor.GREEN + "+" + tpl.getDefRange());
                    } else if (line.contains("<glyph:agi>")) {
                        lore.set(i, GuiUtil.formatStatName(StatsManager.StatType.AGI) + ": "
                                + ChatColor.GREEN + "+" + tpl.getAgiRange());
                    } else if (line.contains("<glyph:int>")) {
                        lore.set(i, GuiUtil.formatStatName(StatsManager.StatType.INT) + ": "
                                + ChatColor.GREEN + "+" + tpl.getIntelRange());
                    } else if (line.contains("<glyph:dex>")) {
                        lore.set(i, GuiUtil.formatStatName(StatsManager.StatType.DEX) + ": "
                                + ChatColor.GREEN + "+" + tpl.getDexRange());
                    }
                }

                // 2) Remove any old currency stubs, then re‐add fresh stubs
                lore.removeIf(l -> l.equalsIgnoreCase("Price:") || l.startsWith("✘") || l.startsWith("✔"));
                lore.removeIf(l -> l.equalsIgnoreCase("Gems:")  || l.startsWith("✘") || l.startsWith("✔"));

                addPriceStub(lore, mItem);

                meta.setLore(lore);
                stack.setItemMeta(meta);
                inventory.setItem(mItem.getSlot(), stack);
            }
        }
    }

    private void addPriceStub(List<String> lore, MerchantItem mItem) {
        lore.add("");                               // spacer
        lore.add(ChatColor.GOLD + "Price:");        // unified price header

        lore.add(ChatColor.GRAY + "- "
            + ChatColor.RED + "✘ "
            + mItem.getCost()
            + " "
            + ChatColor.GOLD + "<glyph:coins_icon>");

        if (mItem.getGems() > 0) {
            lore.add(ChatColor.GRAY + "- "
                + ChatColor.RED + "✘ "
                + mItem.getGems()
                + " "
                + ChatColor.LIGHT_PURPLE + "<glyph:purple_orb_icon>");
        }
    }





    private void loadMerchantItem(Map<String, Object> map) {
        try {
            int slot   = Integer.parseInt(map.get("slot").toString());
            int amount = Integer.parseInt(map.get("amount").toString());
            int cost   = Integer.parseInt(map.get("cost").toString());
            // ← safely pull gems (defaults to 0 if missing)
            int gems   = map.containsKey("gems")
                ? Integer.parseInt(map.get("gems").toString())
                : 0;
            String tierName = map.containsKey("tool_tier") ? map.get("tool_tier").toString() : null;
            if (tierName != null) {
                ToolTier tier = ToolTier.valueOf(tierName.toUpperCase());
                CustomTool tool = ToolManager.getInstance().getTool(tier);
                if (tool != null) {
                    merchantItems.put(slot, new MerchantItem(slot, tool, amount, cost, gems));
                }
            } else {
                int itemId = Integer.parseInt(map.get("item_id").toString());
                merchantItems.put(slot, new MerchantItem(slot, itemId, amount, cost, gems));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load a merchant item from Map: "
                + e.getMessage());
        }
    }

    private void loadMerchantItem(ConfigurationSection cs) {
        try {
            int slot   = cs.getInt("slot");
            int amount = cs.getInt("amount");
            int cost   = cs.getInt("cost");
            // ← same guard here
            int gems   = cs.contains("gems")
                ? cs.getInt("gems")
                : 0;
            String tierName = cs.getString("tool_tier");
            if (tierName != null && !tierName.isBlank()) {
                ToolTier tier = ToolTier.valueOf(tierName.toUpperCase());
                CustomTool tool = ToolManager.getInstance().getTool(tier);
                if (tool != null) {
                    merchantItems.put(slot, new MerchantItem(slot, tool, amount, cost, gems));
                }
            } else {
                int itemId = cs.getInt("item_id");
                merchantItems.put(slot, new MerchantItem(slot, itemId, amount, cost, gems));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load a merchant item from Section: "
                + e.getMessage());
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
            List<String> lore = (meta != null && meta.hasLore()) ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

            // Find the gold “Price:” header
            int headerIdx = ensurePriceHeader(lore, mItem);
            if (headerIdx == -1) continue;  // shouldn’t happen

            // Build the new price line
            boolean afford = coins >= mItem.getCost();
            String priceLine = ChatColor.GOLD + "- "
                + (afford
                ? ChatColor.GREEN + "✔ "
                : ChatColor.RED   + "✘ ")
                + mItem.getCost()
                + " "
                + ChatColor.GOLD + "<glyph:coins_icon>";

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

            int coinCost = TownPerkManager.getInstance().applyDiscount(
                    GuildManager.getInstance().getGuild(player.getUniqueId()),
                    TownPerk.MERCHANT_DISCOUNT,
                    mItem.getCost());
            int gemCost = mItem.getGems();

            int coinBalance = economyManager.getBalance(player);
            int gemBalance = Main.getInstance().getGemsManager().getTotalUnits(player);

            // Check coin requirement
            if (coinBalance < coinCost) {
                send(player, MessageType.ERROR, "You don't have enough coins!");
                return;
            }

            // Check gem requirement
            if (gemCost > 0 && gemBalance < gemCost) {
                send(player, MessageType.ERROR, "You don't have enough gems!");
                return;
            }

            if (player.getInventory().firstEmpty() == -1) {
                player.sendTitle(ChatColor.RED + "Inventory full!", "", 10, 70, 20);
                return;
            }

            // Deduct coins
            try {
                economyManager.deductCoins(player, coinCost);
            } catch (IllegalArgumentException ex) {
                send(player, MessageType.ERROR, "Transaction failed: " + ex.getMessage());
                return;
            }

            // Deduct gems if needed
            if (gemCost > 0) {
                Main.getInstance().getGemsManager().deductUnits(player, gemCost);
            }

            // Give item to player
            if (mItem.isTool()) {
                CustomTool tool = mItem.getTool();
                if (tool != null) {
                    ItemStack purchasedItem = new ItemStack(tool.getMaterial(), mItem.getAmount());
                    ItemMeta meta = purchasedItem.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName(ChatColor.GREEN + tool.getName());
                        purchasedItem.setItemMeta(meta);
                    }
                    ItemUtil.updateCustomToolTooltip(purchasedItem, player);
                    player.getInventory().addItem(purchasedItem);
                    send(player, MessageType.SUCCESS,
                            "You purchased " +
                                    purchasedItem.getItemMeta().getDisplayName() +
                                    ChatColor.GREEN + " for " +
                                    ChatColor.YELLOW + coinCost + " <glyph:coins_icon> coins" +
                                    (gemCost > 0 ? ChatColor.GRAY + " and " + ChatColor.LIGHT_PURPLE + gemCost + "<glyph:purple_orb_icon>" : "") +
                                    ChatColor.GREEN + ".");
                }
            } else {
                CustomItem template = ItemManager.getInstance().getTemplateById(mItem.getItemId());
                if (template != null) {

                    CustomItem newInstance = ItemManager.getInstance().rollNewInstance(template.getId());
                    ItemStack purchasedItem = ItemUtil.createItemStackFromCustomItem(newInstance, mItem.getAmount(), player);
                    player.getInventory().addItem(purchasedItem);
                    Main.getInstance().getQuestManager().handleBuy(player, String.valueOf(mItem.getItemId()));
                    send(player, MessageType.SUCCESS,
                            "You purchased " +
                                    purchasedItem.getItemMeta().getDisplayName() +
                                    ChatColor.GREEN + " for " +
                                    ChatColor.YELLOW + coinCost + " <glyph:coins_icon> coins" +
                                    (gemCost > 0 ? ChatColor.GRAY + " and " + ChatColor.LIGHT_PURPLE + gemCost + "<glyph:purple_orb_icon>" : "") +
                                    ChatColor.GREEN + ".");
                }
            }
        }
    }

    private void updateMerchantTooltips(Player player) {
        int lvl       = StatsManager.getInstance().getLevel(player);
        int coins     = economyManager.getBalance(player);
        int totalGems = Main.getInstance().getGemsManager().getTotalUnits(player);
        me.nakilex.levelplugin.guild.Guild g = GuildManager.getInstance().getGuild(player.getUniqueId());

        for (MerchantItem mItem : merchantItems.values()) {
            ItemStack stack = inventory.getItem(mItem.getSlot());
            if (stack == null || !stack.hasItemMeta()) continue;

            ItemMeta meta = stack.getItemMeta();
            List<String> lore = (meta != null && meta.hasLore()) ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            if (mItem.isTool()) {
                ItemUtil.updateCustomToolTooltip(stack, player);
                meta = stack.getItemMeta();
                lore = meta != null && meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                int priceHdr = ensurePriceHeader(lore, mItem);
                if (priceHdr != -1 && priceHdr + 1 < lore.size()) {
                    int discounted = TownPerkManager.getInstance().applyDiscount(g, TownPerk.MERCHANT_DISCOUNT, mItem.getCost());
                    boolean afford = coins >= discounted;
                    lore.set(priceHdr + 1,
                            ChatColor.GRAY + ""
                                    + (afford ? ChatColor.GREEN + "✔ " : ChatColor.RED + "✘ ")
                                    + discounted + " "
                                    + ChatColor.GOLD + "<glyph:coins_icon>");
                }
                if (mItem.getGems() > 0 && priceHdr != -1) {
                    int gemLineIdx = priceHdr + 2;
                    if (gemLineIdx < lore.size()) {
                        boolean afford = totalGems >= mItem.getGems();
                        lore.set(gemLineIdx,
                                ChatColor.GRAY + ""
                                        + (afford ? ChatColor.GREEN + "✔ " : ChatColor.RED + "✘ ")
                                        + mItem.getGems()
                                        + " "
                                        + ChatColor.LIGHT_PURPLE + "<glyph:purple_orb_icon>");
                    }
                }
                if (meta != null) {
                    meta.setLore(lore);
                    stack.setItemMeta(meta);
                }
            } else {
                CustomItem tpl = ItemManager.getInstance().getTemplateById(mItem.getItemId());

                // ── 1) Level Requirement ─────────────────────────
                int lvlIdx = -1;
                for (int i = 0; i < lore.size(); i++) {
                    if (lore.get(i).contains("Level Requirement:")) {
                        lvlIdx = i;
                        break;
                    }
                }
                if (lvlIdx != -1 && tpl != null) {
                    boolean ok = lvl >= tpl.getLevelRequirement();
                    lore.set(lvlIdx,
                        (ok ? ChatColor.GREEN + "✔ " : ChatColor.RED + "✘ ")
                            + ChatColor.GRAY + "Level Requirement: "
                            + ChatColor.WHITE + tpl.getLevelRequirement()
                    );
                }


                // ── 3) Coin Price ────────────────────────────────
                int priceHdr = ensurePriceHeader(lore, mItem);
                if (priceHdr != -1 && priceHdr + 1 < lore.size()) {
                    int discounted = TownPerkManager.getInstance().applyDiscount(g, TownPerk.MERCHANT_DISCOUNT, mItem.getCost());
                    boolean afford = coins >= discounted;
                    lore.set(priceHdr + 1,
                        ChatColor.GRAY + ""
                            + (afford ? ChatColor.GREEN + "✔ " : ChatColor.RED + "✘ ")
                            + discounted + " "
                            + ChatColor.GOLD + "<glyph:coins_icon>"
                    );
                }

                // ── 4) Gems Price (if any) ───────────────────────
                if (mItem.getGems() > 0 && priceHdr != -1) {
                    int gemLineIdx = priceHdr + 2;
                    if (gemLineIdx < lore.size()) {
                        boolean afford = totalGems >= mItem.getGems();
                        lore.set(gemLineIdx,
                            ChatColor.GRAY + ""
                                + (afford ? ChatColor.GREEN + "✔ " : ChatColor.RED + "✘ ")
                                + mItem.getGems()
                                + " "
                                + ChatColor.LIGHT_PURPLE + "<glyph:purple_orb_icon>"
                        );
                    }
                }


                meta.setLore(lore);
                stack.setItemMeta(meta);
            }
        }
    }

    private int ensurePriceHeader(List<String> lore, MerchantItem mItem) {
        int headerIdx = lore.indexOf(ChatColor.GOLD + "Price:");
        if (headerIdx == -1) {
            addPriceStub(lore, mItem);
            headerIdx = lore.indexOf(ChatColor.GOLD + "Price:");
        }
        return headerIdx;
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
