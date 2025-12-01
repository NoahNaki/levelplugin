package me.nakilex.levelplugin.dungeon.gui;

import me.nakilex.levelplugin.dungeon.DungeonManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.text.DecimalFormat;

/** GUI listing playable dungeons. */
public class DungeonListGUI implements Listener {
    private static final String TITLE = ChatColor.BLACK + "Dungeons";
    private static final int SIZE = 54;
    private static final int COMMUNITY_FILTER_SLOT = 3;
    private static final int VERIFIED_FILTER_SLOT = 5;

    private final DungeonManager manager;
    private final Map<java.util.UUID, DungeonManager.LayoutFilter> filters = new HashMap<>();

    public DungeonListGUI(DungeonManager manager) {
        this.manager = manager;
    }

    public void open(Player player) {
        Inventory inv = GuiBuilder.create(SIZE, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .fillEmptySlots(false)
                .border()
                .build();
        DungeonManager.LayoutFilter filter = filters.getOrDefault(player.getUniqueId(), DungeonManager.LayoutFilter.ALL);
        inv.setItem(COMMUNITY_FILTER_SLOT, createFilterItem(DungeonManager.LayoutFilter.COMMUNITY, filter));
        inv.setItem(VERIFIED_FILTER_SLOT, createFilterItem(DungeonManager.LayoutFilter.VERIFIED, filter));
        int slot = 10;
        DecimalFormat df = new DecimalFormat("#.#");
        for (var entry : manager.getLayoutEntries(filter)) {
            if (slot >= 44) break;
            String key = entry.getKey();
            String display = entry.getValue();
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.AQUA + display);
                int threat = manager.getThreatLevel(key);
                double rating = me.nakilex.levelplugin.Main.getInstance().getDungeonRatingManager().getAverage(key);
                String stars = GuiUtil.glyphStars((int) Math.floor(rating));
                String ratingLine = rating > 0
                        ? ChatColor.GRAY + "Rating " + ChatColor.WHITE + df.format(rating) + " " + ChatColor.GOLD + stars
                        : ChatColor.GRAY + "Rating " + ChatColor.WHITE + "N/A";
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + " ");
                lore.add(ChatColor.GRAY + "Threat Level " + ChatColor.WHITE + threat);
                lore.add(ratingLine);
                lore.add(ChatColor.GRAY + " ");
                lore.add(ChatColor.GRAY + "Category: " + (manager.isVerified(key)
                        ? ChatColor.GREEN + "Verified"
                        : ChatColor.AQUA + "Community"));
                lore.add(ChatColor.WHITE + "Left-click " + ChatColor.GRAY + "to play");
                meta.setLore(lore);
                meta.setLocalizedName(key);
                item.setItemMeta(meta);
                me.nakilex.levelplugin.utils.TextUtil.centerItemTooltip(item, true, false);
            }
            inv.setItem(slot, item);
            slot++;
            if (slot % 9 == 8) slot += 2;
        }
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(TITLE)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.getLocalizedName() != null && meta.getLocalizedName().startsWith("filter:")) {
            String token = meta.getLocalizedName().substring("filter:".length());
            DungeonManager.LayoutFilter next = DungeonManager.LayoutFilter.valueOf(token);
            DungeonManager.LayoutFilter current = filters.getOrDefault(player.getUniqueId(), DungeonManager.LayoutFilter.ALL);
            if (current == next) {
                next = DungeonManager.LayoutFilter.ALL;
            }
            filters.put(player.getUniqueId(), next);
            open(player);
            return;
        }
        if (!event.getClick().isLeftClick()) return;
        String key = meta != null && meta.getLocalizedName() != null ? meta.getLocalizedName() : ChatColor.stripColor(meta.getDisplayName());
        player.closeInventory();
        manager.startInstance(player, key);
    }

    private ItemStack createFilterItem(DungeonManager.LayoutFilter type, DungeonManager.LayoutFilter active) {
        boolean selected = active == type;
        ItemStack filter = new ItemStack(selected ? Material.LIME_STAINED_GLASS_PANE : Material.WHITE_STAINED_GLASS_PANE);
        ItemMeta meta = filter.getItemMeta();
        if (meta != null) {
            meta.setDisplayName((selected ? ChatColor.GREEN : ChatColor.WHITE) + (type == DungeonManager.LayoutFilter.COMMUNITY
                    ? "Community Dungeons"
                    : "Verified Dungeons"));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Toggle to filter the dungeon list.");
            lore.add(ChatColor.GRAY + "Current filter: " + ChatColor.WHITE + active.name().toLowerCase());
            lore.addAll(TooltipUtil.clickInstructions("to apply", null));
            meta.setLore(lore);
            meta.setLocalizedName("filter:" + type.name());
            filter.setItemMeta(meta);
        }
        return filter;
    }
}
