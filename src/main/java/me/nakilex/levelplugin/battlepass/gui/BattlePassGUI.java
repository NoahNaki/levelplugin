package me.nakilex.levelplugin.battlepass.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.battlepass.BattlePassManager;
import me.nakilex.levelplugin.battlepass.BattlePassProgress;
import me.nakilex.levelplugin.battlepass.BattlePassReward;
import me.nakilex.levelplugin.battlepass.BattlePassTier;
import me.nakilex.levelplugin.utils.CurrencyMessageUtil;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Renders the season pass inventory view.
 */
public final class BattlePassGUI {
    private BattlePassGUI() {}

    public static final String TITLE = ChatColor.BLACK + "Season Pass";
    private static final int SIZE = 54;
    private static final int PAGE_SIZE = 8;
    private static final int INFO_SLOT = 4;
    private static final int PREMIUM_SLOT = 49;
    private static final int PREV_SLOT = 45;
    private static final int NEXT_SLOT = 53;

    private static final Map<UUID, Integer> PAGES = new HashMap<>();

    public static final NamespacedKey ACTION_KEY = new NamespacedKey(Main.getInstance(), "bp_action");
    public static final NamespacedKey TIER_KEY = new NamespacedKey(Main.getInstance(), "bp_tier");
    public static final NamespacedKey TRACK_KEY = new NamespacedKey(Main.getInstance(), "bp_track");

    public static void open(Player player, BattlePassManager manager) {
        int page = PAGES.getOrDefault(player.getUniqueId(), 0);
        open(player, manager, page);
    }

    public static void changePage(Player player, BattlePassManager manager, int delta) {
        int current = PAGES.getOrDefault(player.getUniqueId(), 0);
        open(player, manager, current + delta);
    }

    public static void open(Player player, BattlePassManager manager, int page) {
        List<BattlePassTier> tiers = manager.getTiers();
        int maxPage = Math.max(0, (tiers.size() - 1) / PAGE_SIZE);
        if (page < 0) page = 0;
        if (page > maxPage) page = maxPage;
        PAGES.put(player.getUniqueId(), page);

        GuiBuilder builder = GuiBuilder.create(SIZE, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .border()
                .fillEmptySlots(false);

        BattlePassProgress progress = manager.getProgress(player.getUniqueId());
        builder.setItem(INFO_SLOT, createProgressItem(manager, progress, tiers.size()));
        builder.setItem(PREMIUM_SLOT, createPremiumItem(manager, progress));

        if (page > 0) {
            builder.setItem(PREV_SLOT, navigationItem(ChatColor.GREEN + "Previous Page", "prev"));
        }
        if (page < maxPage) {
            builder.setItem(NEXT_SLOT, navigationItem(ChatColor.GREEN + "Next Page", "next"));
        }

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, tiers.size());
        for (int index = start; index < end; index++) {
            BattlePassTier tier = tiers.get(index);
            int local = index - start;
            placeTier(builder, manager, progress, tier, index + 1, local, player);
        }

        Inventory inv = builder.fillEmptySlots(true).build();
        player.openInventory(inv);
    }

    private static ItemStack createProgressItem(BattlePassManager manager, BattlePassProgress progress, int totalTiers) {
        ItemStack item = GuiUtil.getNexoItem("info", ChatColor.YELLOW + "Season Progress");
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        List<String> lore = new ArrayList<>();
        lore.add(manager.getSeasonName());
        int unlocked = progress.unlockedTiers(manager.getXpPerTier(), totalTiers);
        int currentTier = Math.min(totalTiers, unlocked + 1);
        lore.add(ChatColor.GRAY + "Tier: " + ChatColor.WHITE + Math.max(0, unlocked) + "/" + totalTiers);
        lore.add(ChatColor.GRAY + "Current Goal: " + ChatColor.WHITE + currentTier);
        lore.add(" ");
        lore.add(ChatColor.GRAY + "Battle Pass XP: " + ChatColor.WHITE + progress.getXp() + "/" + (manager.getXpPerTier() * totalTiers));
        int xpPerTier = manager.getXpPerTier();
        int base = unlocked * xpPerTier;
        int within = Math.max(0, Math.min(xpPerTier, progress.getXp() - base));
        double ratio = xpPerTier == 0 ? 0.0 : (double) within / xpPerTier;
        lore.add(ChatColor.DARK_GRAY + GuiUtil.createProgressBar(ratio, 20, ChatColor.GREEN, ChatColor.DARK_GRAY, "|"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createPremiumItem(BattlePassManager manager, BattlePassProgress progress) {
        if (progress.hasPremium()) {
            ItemStack unlocked = GuiUtil.getNexoItem("check", ChatColor.LIGHT_PURPLE + "Premium Track Active");
            ItemMeta meta = unlocked.getItemMeta();
            if (meta != null) {
                List<String> lore = List.of(
                        ChatColor.GRAY + "Enjoy premium rewards for the season."
                );
                meta.setLore(lore);
                unlocked.setItemMeta(meta);
            }
            return unlocked;
        }
        ItemStack lock = GuiUtil.getNexoItem("lock", ChatColor.LIGHT_PURPLE + "Unlock Premium Track");
        ItemMeta meta = lock.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Cost: " + CurrencyMessageUtil.formatAmount(
                    CurrencyMessageUtil.Currency.GEMS, manager.getPremiumCostGems()));
            lore.add(" ");
            lore.addAll(TooltipUtil.clickInstructions("to unlock premium", null));
            meta.setLore(lore);
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(ACTION_KEY, PersistentDataType.STRING, "unlock");
            lock.setItemMeta(meta);
        }
        return lock;
    }

    private static ItemStack navigationItem(String title, String action) {
        ItemStack stack = GuiUtil.getNexoItem("arrow_right", title);
        if (title.contains("Previous")) {
            stack = GuiUtil.getNexoItem("arrow_left", title);
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(ACTION_KEY, PersistentDataType.STRING, action);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static void placeTier(GuiBuilder builder,
                                  BattlePassManager manager,
                                  BattlePassProgress progress,
                                  BattlePassTier tier,
                                  int tierNumber,
                                  int localIndex,
                                  Player player) {
        int row = localIndex / 2;
        int group = localIndex % 2;
        int base = row * 7 + group * 3;
        if (base + 2 >= GuiUtil.PAGED_SLOTS.length) return;

        int freeSlot = GuiUtil.PAGED_SLOTS[base];
        int infoSlot = GuiUtil.PAGED_SLOTS[base + 1];
        int premiumSlot = GuiUtil.PAGED_SLOTS[base + 2];

        builder.setItem(infoSlot, tierInfo(manager, progress, tierNumber));

        boolean tierUnlocked = progress.getXp() >= tierNumber * manager.getXpPerTier();
        ItemStack free = rewardItem(player, tier.freeReward(), manager, progress, tierNumber, false, tierUnlocked,
                progress.hasPremium(), progress.isClaimed(tierNumber, false));
        if (free != null) {
            builder.setItem(freeSlot, free);
        }
        ItemStack premium = rewardItem(player, tier.premiumReward(), manager, progress, tierNumber, true, tierUnlocked,
                progress.hasPremium(), progress.isClaimed(tierNumber, true));
        if (premium != null) {
            builder.setItem(premiumSlot, premium);
        }
    }

    private static ItemStack tierInfo(BattlePassManager manager, BattlePassProgress progress, int tierNumber) {
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();
        if (meta == null) return paper;
        meta.setDisplayName(ChatColor.AQUA + "Tier " + tierNumber);
        List<String> lore = new ArrayList<>();
        int xpPerTier = manager.getXpPerTier();
        int required = tierNumber * xpPerTier;
        int base = (tierNumber - 1) * xpPerTier;
        int within = Math.max(0, Math.min(xpPerTier, progress.getXp() - base));
        double ratio = xpPerTier == 0 ? 0.0 : (double) within / xpPerTier;
        lore.add(ChatColor.GRAY + "Required XP: " + ChatColor.WHITE + required);
        lore.add(ChatColor.GRAY + "Progress: " + ChatColor.WHITE + within + "/" + xpPerTier);
        lore.add(ChatColor.DARK_GRAY + GuiUtil.createProgressBar(ratio, 16, ChatColor.GREEN, ChatColor.DARK_GRAY, "|"));
        meta.setLore(lore);
        paper.setItemMeta(meta);
        return paper;
    }

    private static ItemStack rewardItem(Player player,
                                        BattlePassReward reward,
                                        BattlePassManager manager,
                                        BattlePassProgress progress,
                                        int tierNumber,
                                        boolean premium,
                                        boolean tierUnlocked,
                                        boolean premiumUnlocked,
                                        boolean claimed) {
        if (reward == null) {
            ItemStack placeholder = new ItemStack(Material.BARRIER);
            ItemMeta meta = placeholder.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.RED + "No Reward");
                placeholder.setItemMeta(meta);
            }
            return placeholder;
        }
        ItemStack item = reward.createDisplay(player, manager, progress, tierNumber, premium, tierUnlocked, premiumUnlocked, claimed);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(ACTION_KEY, PersistentDataType.STRING, "reward");
            pdc.set(TIER_KEY, PersistentDataType.INTEGER, tierNumber);
            pdc.set(TRACK_KEY, PersistentDataType.STRING, premium ? "PREMIUM" : "FREE");
            item.setItemMeta(meta);
        }
        return item;
    }
}
