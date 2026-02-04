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
import me.nakilex.levelplugin.utils.gui.widgets.ActionWidget;
import me.nakilex.levelplugin.utils.gui.widgets.GuiContext;
import me.nakilex.levelplugin.utils.gui.widgets.GuiLayout;
import me.nakilex.levelplugin.utils.gui.widgets.GuiWidget;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.text.DecimalFormat;

/** GUI listing playable dungeons. */
public class DungeonListGUI implements Listener {
    private static final String TITLE = "Dungeons";
    private static final int SIZE = 54;
    private static final int COMMUNITY_FILTER_SLOT = 3;
    private static final int VERIFIED_FILTER_SLOT = 5;

    private final DungeonManager manager;
    private final Map<java.util.UUID, DungeonManager.LayoutFilter> filters = new HashMap<>();
    private final List<GuiWidget> widgets;

    public DungeonListGUI(DungeonManager manager) {
        this.manager = manager;
        this.widgets = buildWidgets();
    }

    public void open(Player player) {
        Inventory inv = GuiBuilder.create(SIZE, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .fillEmptySlots(false)
                .border()
                .build();
        renderWidgets(inv, player);
        int slot = 10;
        DecimalFormat df = new DecimalFormat("#.#");
        for (var entry : manager.getLayoutEntries(getFilter(player))) {
            if (slot >= 44) break;
            String key = entry.getKey();
            String display = entry.getValue();
            boolean verified = manager.isVerified(key);
            Material icon = verified ? Material.WITHER_SKELETON_SKULL : Material.SKELETON_SKULL;
            ItemStack item = new ItemStack(icon);
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
                lore.add(ChatColor.GRAY + "Category: " + (verified
                        ? ChatColor.GREEN + "Verified"
                        : ChatColor.AQUA + "Community"));
                lore.addAll(TooltipUtil.clickInstructions("to play", null));
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
        if (!GuiUtil.titleMatches(event.getView().getTitle(), TITLE)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (handleWidgetClick(event, player)) {
            return;
        }
        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;
        var meta = item.getItemMeta();
        if (!event.getClick().isLeftClick()) return;
        String key = meta != null && meta.getLocalizedName() != null ? meta.getLocalizedName() : ChatColor.stripColor(meta.getDisplayName());
        player.closeInventory();
        manager.startInstance(player, key);
    }

    private List<GuiWidget> buildWidgets() {
        List<GuiWidget> widgetList = new ArrayList<>();
        widgetList.add(new ActionWidget(COMMUNITY_FILTER_SLOT,
                context -> createFilterItem(DungeonManager.LayoutFilter.COMMUNITY, getFilter(context.player())),
                (click, context) -> handleFilterClick(context.player(), DungeonManager.LayoutFilter.COMMUNITY)));
        widgetList.add(new ActionWidget(VERIFIED_FILTER_SLOT,
                context -> createFilterItem(DungeonManager.LayoutFilter.VERIFIED, getFilter(context.player())),
                (click, context) -> handleFilterClick(context.player(), DungeonManager.LayoutFilter.VERIFIED)));
        return widgetList;
    }

    private void renderWidgets(Inventory inventory, Player player) {
        GuiLayout layout = new GuiLayout(inventory);
        GuiContext context = new GuiContext(player, inventory);
        for (GuiWidget widget : widgets) {
            widget.contribute(layout, context);
        }
    }

    private boolean handleWidgetClick(InventoryClickEvent event, Player player) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) {
            return false;
        }
        GuiWidget widget = widgets.stream()
                .filter(w -> w.handlesSlot(slot))
                .findFirst()
                .orElse(null);
        if (widget == null) {
            return false;
        }
        event.setCancelled(true);
        widget.onClick(slot, event.getClick(), new GuiContext(player, event.getView().getTopInventory()));
        return true;
    }

    private ItemStack createFilterItem(DungeonManager.LayoutFilter type, DungeonManager.LayoutFilter active) {
        boolean selected = active == type;
        ItemStack filter = new ItemStack(selected ? Material.LIME_STAINED_GLASS_PANE : Material.WHITE_STAINED_GLASS_PANE);
        var meta = filter.getItemMeta();
        if (meta != null) {
            meta.setDisplayName((selected ? ChatColor.GREEN : ChatColor.WHITE) + (type == DungeonManager.LayoutFilter.COMMUNITY
                    ? "Community Dungeons"
                    : "Verified Dungeons"));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Toggle to filter the dungeon list.");
            lore.add(ChatColor.GRAY + "Current filter: " + ChatColor.WHITE + active.name().toLowerCase());
            lore.addAll(TooltipUtil.clickInstructions("to apply", null));
            meta.setLore(lore);
            filter.setItemMeta(meta);
        }
        return filter;
    }

    private DungeonManager.LayoutFilter getFilter(Player player) {
        return filters.getOrDefault(player.getUniqueId(), DungeonManager.LayoutFilter.ALL);
    }

    private void handleFilterClick(Player player, DungeonManager.LayoutFilter type) {
        DungeonManager.LayoutFilter current = getFilter(player);
        DungeonManager.LayoutFilter next = current == type ? DungeonManager.LayoutFilter.ALL : type;
        filters.put(player.getUniqueId(), next);
        open(player);
    }
}
