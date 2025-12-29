package me.nakilex.levelplugin.player.fishing.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.player.fishing.config.FishingRewardsConfig;
import me.nakilex.levelplugin.player.fishing.data.FishDefinition;
import me.nakilex.levelplugin.player.fishing.managers.FishingManager;
import me.nakilex.levelplugin.player.fishing.utils.FishingItemUtil;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class FishingCatalogGUI implements Listener {

    private static final String TITLE = "Fishing Catalog";
    private static final int SIZE = 54;
    private static final int INFO_SLOT = 8;
    private static final int PREV_SLOT = 45;
    private static final int BACK_SLOT = 49;
    private static final int NEXT_SLOT = 53;

    private final Main plugin;
    private final FishingRewardsConfig rewardsConfig;
    private final FishingManager fishingManager;
    private final FishingRewardsGUI rewardsGUI;
    private final Map<UUID, Integer> pageMap = new java.util.HashMap<>();

    public FishingCatalogGUI(Main plugin, FishingRewardsConfig rewardsConfig, FishingRewardsGUI rewardsGUI) {
        this.plugin = plugin;
        this.rewardsConfig = rewardsConfig;
        this.rewardsGUI = rewardsGUI;
        this.fishingManager = FishingManager.getInstance();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        open(player, pageMap.getOrDefault(player.getUniqueId(), 0));
    }

    private void open(Player player, int page) {
        pageMap.put(player.getUniqueId(), page);
        Inventory inv = GuiBuilder.create(SIZE, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .fillEmptySlots(false)
                .border()
                .build();

        inv.setItem(INFO_SLOT, createInfoItem());
        inv.setItem(BACK_SLOT, GuiUtil.getNexoItem("arrow_left", ChatColor.YELLOW + "Back to Rewards"));

        List<FishDefinition> fish = rewardsConfig.getFish();
        fish.sort(Comparator
                .comparingInt(FishDefinition::minLevel)
                .thenComparing(FishDefinition::displayName, String.CASE_INSENSITIVE_ORDER));

        int start = page * GuiUtil.PAGED_SLOTS.length;
        int slotIndex = 0;
        for (int i = start; i < fish.size() && slotIndex < GuiUtil.PAGED_SLOTS.length; i++) {
            FishDefinition def = fish.get(i);
            ItemStack item = fishingManager.isFishDiscovered(player.getUniqueId(), def.id())
                    ? createDiscoveredItem(def)
                    : createUndiscoveredItem(def);
            inv.setItem(GuiUtil.PAGED_SLOTS[slotIndex++], item);
        }

        if (page > 0) {
            inv.setItem(PREV_SLOT, GuiUtil.getNexoItem("arrow_left", ChatColor.GREEN + "Previous"));
        }
        if (fish.size() > (page + 1) * GuiUtil.PAGED_SLOTS.length) {
            inv.setItem(NEXT_SLOT, GuiUtil.getNexoItem("arrow_right", ChatColor.GREEN + "Next"));
        }

        player.openInventory(inv);
    }

    private ItemStack createInfoItem() {
        ItemStack info = GuiUtil.getNexoItem("info", ChatColor.YELLOW + "Fishing Catalog");
        ItemMeta meta = info.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Track every fish you can catch.");
            lore.add(ChatColor.GRAY + "Unknown fish reveal after discovery.");
            lore.add("");
            lore.addAll(TooltipUtil.clickInstructions("to return to rewards", null));
            meta.setLore(lore);
            info.setItemMeta(meta);
        }
        return info;
    }

    private ItemStack createDiscoveredItem(FishDefinition def) {
        double size = (def.minSize() + def.maxSize()) / 2.0;
        ItemStack item = FishingItemUtil.createFishItem(def, size);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        List<String> lore = meta.getLore();
        if (lore == null) {
            lore = new ArrayList<>();
        } else {
            lore = new ArrayList<>(lore);
        }
        lore.add("");
        lore.add(ChatColor.GOLD + "Rewards:");
        lore.addAll(TooltipUtil.bulletList(
                "XP: +" + def.xpReward(),
                "Value: " + def.sellValue() + " coins"));
        addRequirements(lore, def);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createUndiscoveredItem(FishDefinition def) {
        ItemStack item = GuiUtil.getNexoItem("info", ChatColor.DARK_GRAY + "Unknown Fish");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Catch this fish to reveal it.");
            lore.add("");
            addRequirements(lore, def);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void addRequirements(List<String> lore, FishDefinition def) {
        List<String> requirements = new ArrayList<>();
        requirements.add("Fishing Lv. " + def.minLevel());
        if (def.requiresLava()) {
            requirements.add("Requires lava fishing");
        }
        if (def.requiresHighestTier()) {
            requirements.add("Requires highest tier rod");
        }
        if (!requirements.isEmpty()) {
            lore.add(ChatColor.GRAY + "Requirements:");
            lore.addAll(TooltipUtil.bulletList(requirements.toArray(new String[0])));
        }
        if (def.rarity() != ItemRarity.COMMON) {
            lore.add(ChatColor.GRAY + "Rarity: " + def.rarity().getColor()
                    + def.rarity().name().toLowerCase(Locale.ROOT));
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!GuiUtil.titleMatches(event.getView().getTitle(), TITLE)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        event.setCancelled(true);
        int rawSlot = event.getRawSlot();
        if (rawSlot == BACK_SLOT || rawSlot == INFO_SLOT) {
            rewardsGUI.open(player);
            return;
        }
        if (rawSlot == PREV_SLOT) {
            int page = pageMap.getOrDefault(player.getUniqueId(), 0);
            if (page > 0) {
                open(player, page - 1);
            }
            return;
        }
        if (rawSlot == NEXT_SLOT) {
            int page = pageMap.getOrDefault(player.getUniqueId(), 0);
            open(player, page + 1);
        }
    }
}
