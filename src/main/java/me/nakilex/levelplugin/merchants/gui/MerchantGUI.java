package me.nakilex.levelplugin.merchants.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.data.StatRange;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.tools.CustomTool;
import me.nakilex.levelplugin.items.tools.ToolDiscipline;
import me.nakilex.levelplugin.items.tools.ToolManager;
import me.nakilex.levelplugin.items.tools.ToolTier;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.merchants.data.MerchantItem;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.guild.GuildManager;
import me.nakilex.levelplugin.guild.TownPerk;
import me.nakilex.levelplugin.guild.TownPerkManager;
import me.nakilex.levelplugin.player.classes.essence.ClassEssence;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
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
import static me.nakilex.levelplugin.utils.ChatMessageUtil.sendPurchaseMessage;

public class MerchantGUI implements Listener {
    private final Inventory inventory;
    private final Map<Integer, MerchantItem> merchantItems = new HashMap<>();
    private final EconomyManager economyManager;
    private final Plugin plugin;
    private final me.nakilex.levelplugin.player.config.PlayerConfig playerConfig;
    private final String merchantName;
    private int updateTaskId = -1;

    /**
     * @param plugin         Your plugin instance
     * @param merchantConfig The loaded merchants.yml configuration
     * @param merchantName   The merchant name (e.g. "rogue_merchant")
     */
    public MerchantGUI(Plugin plugin, FileConfiguration merchantConfig, String merchantName) {
        this.plugin = plugin;
        this.economyManager = Main.getInstance().getEconomyManager();
        this.playerConfig = Main.getInstance().getPlayerConfig();
        this.merchantName = merchantName;

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
            if (mItem.isEssence()) {
                ItemStack essence = createEssenceStack(mItem.getEssenceData());
                if (essence == null) {
                    continue;
                }
                addPriceStubToStack(essence, mItem);
                inventory.setItem(mItem.getSlot(), essence);
            } else if (mItem.isTool()) {
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

    private ItemStack createEssenceStack(me.nakilex.levelplugin.items.data.GameItem.EssenceData data) {
        PlayerClass clazz = PlayerClass.fromString(data.clazz());
        ItemRarity rarity = data.rarity();
        if (clazz == null || rarity == null) {
            return null;
        }
        return ClassEssence.generateEssence(clazz, rarity, data.stars());
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

        if (mItem.getAccountLimit() > 0) {
            lore.add(TooltipUtil.accountLimitLine(mItem.getAccountLimit()));
        }
    }





    private void addPriceStubToStack(ItemStack stack, MerchantItem mItem) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        addPriceStub(lore, mItem);
        meta.setLore(lore);
        stack.setItemMeta(meta);
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
            int accountLimit = map.containsKey("account_limit")
                ? Integer.parseInt(map.get("account_limit").toString())
                : 0;
            String essenceClass = map.containsKey("essence_class") ? map.get("essence_class").toString() : null;
            if (essenceClass != null) {
                PlayerClass clazz = PlayerClass.fromString(essenceClass);
                ItemRarity rarity = map.containsKey("essence_rarity")
                        ? ItemRarity.valueOf(map.get("essence_rarity").toString().toUpperCase())
                        : ItemRarity.COMMON;
                int stars = map.containsKey("essence_stars")
                        ? Integer.parseInt(map.get("essence_stars").toString())
                        : 0;
                if (clazz != null) {
                    merchantItems.put(slot, new MerchantItem(slot,
                            MerchantItem.essence(clazz, rarity, stars), amount, cost, gems, accountLimit));
                }
                return;
            }
            String tierName = map.containsKey("tool_tier") ? map.get("tool_tier").toString() : null;
            if (tierName != null) {
                ToolTier tier = ToolTier.valueOf(tierName.toUpperCase());
                ToolDiscipline discipline = map.containsKey("tool_discipline")
                        ? ToolDiscipline.valueOf(map.get("tool_discipline").toString().toUpperCase())
                        : ToolDiscipline.MINING;
                CustomTool tool = ToolManager.getInstance().getTool(tier, discipline);
                if (tool != null) {
                    merchantItems.put(slot, new MerchantItem(slot, tool, amount, cost, gems, accountLimit));
                }
            } else {
                int itemId = Integer.parseInt(map.get("item_id").toString());
                merchantItems.put(slot, new MerchantItem(slot, itemId, amount, cost, gems, accountLimit));
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
            int accountLimit = cs.getInt("account_limit", 0);
            String essenceClass = cs.getString("essence_class");
            if (essenceClass != null && !essenceClass.isBlank()) {
                PlayerClass clazz = PlayerClass.fromString(essenceClass);
                ItemRarity rarity = ItemRarity.valueOf(cs.getString("essence_rarity", "COMMON").toUpperCase());
                int stars = cs.getInt("essence_stars", 0);
                if (clazz != null) {
                    merchantItems.put(slot, new MerchantItem(slot,
                            MerchantItem.essence(clazz, rarity, stars), amount, cost, gems, accountLimit));
                }
                return;
            }
            String tierName = cs.getString("tool_tier");
            if (tierName != null && !tierName.isBlank()) {
                ToolTier tier = ToolTier.valueOf(tierName.toUpperCase());
                ToolDiscipline discipline = ToolDiscipline.valueOf(cs.getString("tool_discipline", "MINING").toUpperCase());
                CustomTool tool = ToolManager.getInstance().getTool(tier, discipline);
                if (tool != null) {
                    merchantItems.put(slot, new MerchantItem(slot, tool, amount, cost, gems, accountLimit));
                }
            } else {
                int itemId = cs.getInt("item_id");
                merchantItems.put(slot, new MerchantItem(slot, itemId, amount, cost, gems, accountLimit));
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

            boolean limitReached = hasReachedLimit(player, mItem);

            // Build the new price line
            boolean afford = !limitReached && coins >= mItem.getCost();
            String priceLine = ChatColor.GOLD + "- "
                + (limitReached
                ? ChatColor.RED + "✘ Limit reached"
                : (afford
                ? ChatColor.GREEN + "✔ "
                : ChatColor.RED   + "✘ ")
                + mItem.getCost() + " " + ChatColor.GOLD + "<glyph:coins_icon>");

            // Replace the line immediately after the header
            int lineIdx = headerIdx + 1;
            if (lineIdx < lore.size()) {
                lore.set(lineIdx, priceLine);
            } else {
                lore.add(priceLine);
            }

            if (limitReached && mItem.getGems() > 0 && headerIdx + 2 < lore.size()) {
                lore.set(headerIdx + 2, ChatColor.GRAY + "- " + ChatColor.RED + "✘ Limit reached");
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

            if (hasReachedLimit(player, mItem)) {
                send(player, MessageType.ERROR, "You have already bought the maximum allowed for this offer.");
                return;
            }

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
            if (mItem.isEssence()) {
                ItemStack purchasedItem = createEssenceStack(mItem.getEssenceData());
                if (purchasedItem != null) {
                    Main.getInstance().getQuestManager().handleBuy(player, "essence:" + mItem.getEssenceData().clazz());
                    player.getInventory().addItem(purchasedItem);
                    sendPurchaseMessage(player, purchasedItem.getItemMeta().getDisplayName(), coinCost, gemCost);
                    recordPurchase(player, mItem);
                }
            } else if (mItem.isTool()) {
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
                    sendPurchaseMessage(player, purchasedItem.getItemMeta().getDisplayName(), coinCost, gemCost);
                    recordPurchase(player, mItem);
                }
            } else {
                CustomItem template = ItemManager.getInstance().getTemplateById(mItem.getItemId());
                if (template != null) {

                    CustomItem newInstance = ItemManager.getInstance().rollNewInstance(template.getId());
                    ItemStack purchasedItem = ItemUtil.createItemStackFromCustomItem(newInstance, mItem.getAmount(), player);
                    player.getInventory().addItem(purchasedItem);
                    Main.getInstance().getQuestManager().handleBuy(player, String.valueOf(mItem.getItemId()));
                    sendPurchaseMessage(player, purchasedItem.getItemMeta().getDisplayName(), coinCost, gemCost);
                    recordPurchase(player, mItem);
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
            boolean limitReached = hasReachedLimit(player, mItem);
            if (mItem.isEssence()) {
                int priceHdr = ensurePriceHeader(lore, mItem);
                if (priceHdr != -1 && priceHdr + 1 < lore.size()) {
                    int discounted = TownPerkManager.getInstance().applyDiscount(g, TownPerk.MERCHANT_DISCOUNT, mItem.getCost());
                    boolean afford = !limitReached && coins >= discounted;
                    lore.set(priceHdr + 1,
                            limitReached
                                    ? ChatColor.GRAY + "- " + ChatColor.RED + "✘ Limit reached"
                                    : ChatColor.GRAY + "" + (afford ? ChatColor.GREEN + "✔ " : ChatColor.RED + "✘ ")
                                    + discounted + " " + ChatColor.GOLD + "<glyph:coins_icon>");
                }
                if (mItem.getGems() > 0 && priceHdr != -1) {
                    int gemLineIdx = priceHdr + 2;
                    if (gemLineIdx < lore.size()) {
                        boolean afford = !limitReached && totalGems >= mItem.getGems();
                        lore.set(gemLineIdx,
                                limitReached
                                        ? ChatColor.GRAY + "- " + ChatColor.RED + "✘ Limit reached"
                                        : ChatColor.GRAY + "" + (afford ? ChatColor.GREEN + "✔ " : ChatColor.RED + "✘ ")
                                        + mItem.getGems() + " " + ChatColor.LIGHT_PURPLE + "<glyph:purple_orb_icon>");
                    }
                }
                if (meta != null) {
                    meta.setLore(lore);
                    stack.setItemMeta(meta);
                }
            } else if (mItem.isTool()) {
                ItemUtil.updateCustomToolTooltip(stack, player);
                meta = stack.getItemMeta();
                lore = meta != null && meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                int priceHdr = ensurePriceHeader(lore, mItem);
                if (priceHdr != -1 && priceHdr + 1 < lore.size()) {
                    int discounted = TownPerkManager.getInstance().applyDiscount(g, TownPerk.MERCHANT_DISCOUNT, mItem.getCost());
                    boolean afford = !limitReached && coins >= discounted;
                    lore.set(priceHdr + 1,
                            limitReached
                                    ? ChatColor.GRAY + "- " + ChatColor.RED + "✘ Limit reached"
                                    : ChatColor.GRAY + ""
                                    + (afford ? ChatColor.GREEN + "✔ " : ChatColor.RED + "✘ ")
                                    + discounted + " "
                                    + ChatColor.GOLD + "<glyph:coins_icon>");
                }
                if (mItem.getGems() > 0 && priceHdr != -1) {
                    int gemLineIdx = priceHdr + 2;
                    if (gemLineIdx < lore.size()) {
                        boolean afford = !limitReached && totalGems >= mItem.getGems();
                        lore.set(gemLineIdx,
                                limitReached
                                        ? ChatColor.GRAY + "- " + ChatColor.RED + "✘ Limit reached"
                                        : ChatColor.GRAY + ""
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
                    boolean afford = !limitReached && coins >= discounted;
                    lore.set(priceHdr + 1,
                        limitReached
                                ? ChatColor.GRAY + "- " + ChatColor.RED + "✘ Limit reached"
                                : ChatColor.GRAY + ""
                            + (afford ? ChatColor.GREEN + "✔ " : ChatColor.RED + "✘ ")
                            + discounted + " "
                            + ChatColor.GOLD + "<glyph:coins_icon>"
                    );
                }

                // ── 4) Gems Price (if any) ───────────────────────
                if (mItem.getGems() > 0 && priceHdr != -1) {
                    int gemLineIdx = priceHdr + 2;
                    if (gemLineIdx < lore.size()) {
                        boolean afford = !limitReached && totalGems >= mItem.getGems();
                        lore.set(gemLineIdx,
                            limitReached
                                    ? ChatColor.GRAY + "- " + ChatColor.RED + "✘ Limit reached"
                                    : ChatColor.GRAY + ""
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

    private int getPurchaseCount(Player player, MerchantItem item) {
        if (playerConfig == null || item.getAccountLimit() <= 0) {
            return 0;
        }
        String path = "players." + player.getUniqueId() + ".merchant_limits." + merchantName + "." + item.getSlot();
        return playerConfig.getConfig().getInt(path, 0);
    }

    private boolean hasReachedLimit(Player player, MerchantItem item) {
        int limit = item.getAccountLimit();
        if (limit <= 0) {
            return false;
        }
        return getPurchaseCount(player, item) >= limit;
    }

    private void recordPurchase(Player player, MerchantItem item) {
        if (playerConfig == null || item.getAccountLimit() <= 0) {
            return;
        }
        String path = "players." + player.getUniqueId() + ".merchant_limits." + merchantName + "." + item.getSlot();
        int current = playerConfig.getConfig().getInt(path, 0);
        playerConfig.getConfig().set(path, current + 1);
        playerConfig.saveConfigFile();
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
