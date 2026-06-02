package me.nakilex.levelplugin.player.fishing.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.items.tools.ToolDiscipline;
import me.nakilex.levelplugin.player.attributes.gui.LifeSkillRewardsGUI;
import me.nakilex.levelplugin.player.fishing.config.FishingRewardsConfig;
import me.nakilex.levelplugin.player.fishing.data.FishDefinition;
import me.nakilex.levelplugin.player.fishing.managers.FishingManager;
import me.nakilex.levelplugin.player.fishing.managers.FishingManager;
import me.nakilex.levelplugin.player.fishing.utils.FishingItemUtil;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import me.nakilex.levelplugin.utils.gui.widgets.ActionWidget;
import me.nakilex.levelplugin.utils.gui.widgets.GuiContext;
import me.nakilex.levelplugin.utils.gui.widgets.GuiLayout;
import me.nakilex.levelplugin.utils.gui.widgets.GuiWidget;
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

    private static FishingCatalogGUI instance;

    private final Main plugin;
    private final FishingRewardsConfig rewardsConfig;
    private final FishingManager fishingManager;
    private final Map<UUID, Integer> pageMap = new java.util.HashMap<>();
    private final Map<UUID, List<GuiWidget>> widgetsByPlayer = new java.util.HashMap<>();

    private FishingCatalogGUI(Main plugin, FishingRewardsConfig rewardsConfig) {
        this.plugin = plugin;
        this.rewardsConfig = rewardsConfig;
        this.fishingManager = FishingManager.getInstance();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public static FishingCatalogGUI getInstance() {
        if (instance == null) {
            Main plugin = Main.getInstance();
            instance = new FishingCatalogGUI(plugin, plugin.getFishingRewardsConfig());
        }
        return instance;
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

        List<FishDefinition> fish = rewardsConfig.getFish();
        fish.sort(Comparator
                .comparingInt(FishDefinition::minLevel)
                .thenComparing(FishDefinition::displayName, String.CASE_INSENSITIVE_ORDER));

        int maxPage = Math.max(0, (fish.size() - 1) / GuiUtil.PAGED_SLOTS.length);
        List<GuiWidget> widgets = buildWidgets(player, fish, page, maxPage);
        widgetsByPlayer.put(player.getUniqueId(), widgets);
        renderWidgets(inv, player, widgets);

        player.openInventory(inv);
    }

    private ItemStack createInfoItem(Player player) {
        ItemStack info = GuiUtil.getNexoItem("info", ChatColor.YELLOW + "Fishing Catalog");
        ItemMeta meta = info.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Track every fish you catch on this profile.");
            lore.add(ChatColor.GRAY + "Unknown species reveal after discovery.");
            int discovered = FishingManager.getInstance().getDiscoveredFish(player.getUniqueId()).size();
            int total = rewardsConfig.getFish().size();
            lore.add("");
            lore.add(TooltipUtil.sectionHeader("Collection Progress"));
            lore.add(TooltipUtil.arrowLine(ChatColor.WHITE + "Species: " + ChatColor.YELLOW + discovered
                    + ChatColor.GOLD + "/" + ChatColor.WHITE + total));
            lore.add(TooltipUtil.progressBar(discovered, total, 16));
            lore.add("");
            lore.add(TooltipUtil.sectionHeader("Catalog Milestones"));
            lore.add(TooltipUtil.arrowLine(milestone(discovered, Math.min(5, total), "Angler")));
            lore.add(TooltipUtil.arrowLine(milestone(discovered, Math.min(10, total), "Collector")));
            lore.add(TooltipUtil.arrowLine(milestone(discovered, total, "Master Angler")));
            lore.add("");
            lore.addAll(TooltipUtil.clickInstructions("to return to rewards", null));
            meta.setLore(lore);
            info.setItemMeta(meta);
        }
        return info;
    }


    private String milestone(int discovered, int required, String title) {
        boolean complete = discovered >= required;
        return (complete ? ChatColor.GREEN + "✔ " : ChatColor.DARK_GRAY + "✘ ")
                + ChatColor.YELLOW + title + ChatColor.GRAY + ": " + ChatColor.WHITE
                + Math.min(discovered, required) + ChatColor.GOLD + "/" + ChatColor.WHITE + required;
    }

    private ItemStack createDiscoveredItem(Player player, FishDefinition def) {
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
        FishingManager.FishRecord record = FishingManager.getInstance().getFishRecord(player.getUniqueId(), def.id());
        lore.add(ChatColor.GRAY + "You caught this fish " + ChatColor.WHITE + record.caughtCount()
                + "x" + ChatColor.GRAY + " on this profile.");
        lore.add("");
        lore.add(TooltipUtil.sectionHeader("Trophy Records"));
        lore.add(TooltipUtil.statLine("Largest Catch", String.format("%.1f cm", record.largestSize()), ChatColor.WHITE));
        lore.add(TooltipUtil.statLine("Best Quality", record.bestQuality().getDisplayName(), record.bestQuality().getColor()));
        lore.add("");
        lore.add(TooltipUtil.sectionHeader("Catch Rewards"));
        lore.add(TooltipUtil.statLine("Fishing XP", "+" + def.xpReward(), ChatColor.WHITE));
        lore.add(TooltipUtil.statLine("Base Value", def.sellValue() + " coins", ChatColor.WHITE));
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
            lore.add("");
            lore.add(TooltipUtil.sectionHeader("Requirements"));
            for (String requirement : requirements) lore.add(TooltipUtil.arrowLine(ChatColor.YELLOW + requirement));
        }
        if (def.rarity() != ItemRarity.COMMON) {
            lore.add(TooltipUtil.statLine("Rarity",
                    def.rarity().name().toLowerCase(Locale.ROOT), def.rarity().getColor()));
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
        if (handleWidgetClick(event, player)) {
            return;
        }
        event.setCancelled(true);
    }

    private List<GuiWidget> buildWidgets(Player player, List<FishDefinition> fish, int page, int maxPage) {
        List<GuiWidget> widgets = new ArrayList<>();
        widgets.add(new ActionWidget(INFO_SLOT,
                context -> createInfoItem(player),
                (click, context) -> LifeSkillRewardsGUI.open(context.player(), ToolDiscipline.FISHING)));
        widgets.add(new ActionWidget(BACK_SLOT,
                context -> GuiUtil.getNexoItem("arrow_left", ChatColor.YELLOW + "Back to Rewards"),
                (click, context) -> LifeSkillRewardsGUI.open(context.player(), ToolDiscipline.FISHING)));
        if (page > 0) {
            widgets.add(new ActionWidget(PREV_SLOT,
                    context -> GuiUtil.getNexoItem("arrow_left", ChatColor.GREEN + "Previous"),
                    (click, context) -> open(context.player(), page - 1)));
        }
        if (page < maxPage) {
            widgets.add(new ActionWidget(NEXT_SLOT,
                    context -> GuiUtil.getNexoItem("arrow_right", ChatColor.GREEN + "Next"),
                    (click, context) -> open(context.player(), page + 1)));
        }

        int start = page * GuiUtil.PAGED_SLOTS.length;
        int slotIndex = 0;
        for (int i = start; i < fish.size() && slotIndex < GuiUtil.PAGED_SLOTS.length; i++) {
            FishDefinition def = fish.get(i);
            int slot = GuiUtil.PAGED_SLOTS[slotIndex++];
            widgets.add(new ActionWidget(slot,
                    context -> fishingManager.isFishDiscovered(context.player().getUniqueId(), def.id())
                            ? createDiscoveredItem(player, def)
                            : createUndiscoveredItem(def),
                    null));
        }
        return widgets;
    }

    private void renderWidgets(Inventory inventory, Player player, List<GuiWidget> widgets) {
        GuiLayout layout = new GuiLayout(inventory);
        GuiContext context = new GuiContext(player, inventory);
        for (GuiWidget widget : widgets) {
            widget.contribute(layout, context);
        }
    }

    private boolean handleWidgetClick(InventoryClickEvent event, Player player) {
        List<GuiWidget> widgets = widgetsByPlayer.get(player.getUniqueId());
        if (widgets == null) {
            return false;
        }
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
}
