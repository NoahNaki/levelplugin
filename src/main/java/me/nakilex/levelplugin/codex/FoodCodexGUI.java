package me.nakilex.levelplugin.codex;

import me.nakilex.levelplugin.cooking.model.CookingRecipe;
import me.nakilex.levelplugin.cooking.model.CookingReward;
import me.nakilex.levelplugin.cooking.registry.CookingRecipeRegistry;
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
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** GUI listing discovered cooking outputs. */
public class FoodCodexGUI implements Listener {
    private static final String TITLE = "Codex - Foods";
    private static final int SIZE = 54;
    private static final int PREV_SLOT = 45;
    private static final int NEXT_SLOT = 53;
    private static final int BACK_SLOT = 49;
    private static final int ITEMS_PER_PAGE = GuiUtil.PAGED_SLOTS.length;

    private final CodexManager manager;
    private final CookingRecipeRegistry recipes;
    private CodexMainGUI mainGui;
    private final Map<UUID, Integer> pageMap = new java.util.HashMap<>();
    private final List<GuiWidget> widgets;

    public FoodCodexGUI(CodexManager manager, CookingRecipeRegistry recipes, CodexMainGUI mainGui) {
        this.manager = manager;
        this.recipes = recipes;
        this.mainGui = mainGui;
        this.widgets = buildWidgets();
    }

    public void setMainGui(CodexMainGUI mainGui) {
        this.mainGui = mainGui;
    }

    public void open(Player player) {
        open(player, pageMap.getOrDefault(player.getUniqueId(), 0));
    }

    private void open(Player player, int page) {
        List<FoodEntry> foods = foodEntries();
        int maxPage = Math.max(0, (foods.size() - 1) / ITEMS_PER_PAGE);
        int safePage = Math.max(0, Math.min(page, maxPage));
        pageMap.put(player.getUniqueId(), safePage);

        Inventory inv = GuiBuilder.create(SIZE, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .fillEmptySlots(false)
                .border()
                .build();
        Map<String, String> lines = new LinkedHashMap<>();
        lines.put("Foods", manager.getDiscoveredFoodCount(player.getUniqueId()) + "/" + foods.size());
        inv.setItem(4, CodexGuiUtil.createInfoBook("Discoveries", lines,
                ChatColor.GRAY + "Cook recipes to reveal food entries.",
                ChatColor.GRAY + "Ingredient chains unlock advanced dishes."));

        int start = safePage * ITEMS_PER_PAGE;
        for (int i = start, slot = 0; i < foods.size() && slot < ITEMS_PER_PAGE; i++) {
            inv.setItem(GuiUtil.PAGED_SLOTS[slot++], createFoodIcon(player.getUniqueId(), foods.get(i)));
        }
        renderWidgets(inv, player);
        player.openInventory(inv);
    }

    private List<FoodEntry> foodEntries() {
        Map<String, FoodEntry> entries = new LinkedHashMap<>();
        for (CookingRecipe recipe : recipes.all()) {
            for (CookingReward reward : recipe.rewards()) {
                String key = manager.normalizeFoodKey(reward.discoveryKey());
                entries.putIfAbsent(key, new FoodEntry(key, reward.displayName(), recipe, reward));
            }
        }
        List<FoodEntry> list = new ArrayList<>(entries.values());
        list.sort(Comparator.comparing(FoodEntry::displayName, String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    private ItemStack createFoodIcon(UUID playerId, FoodEntry entry) {
        boolean discovered = manager.hasDiscoveredFood(playerId, entry.key());
        ItemStack item = discovered ? entry.reward().toItemStack() : new ItemStack(Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (discovered) {
                meta.setDisplayName(ChatColor.GOLD + entry.displayName());
                List<String> lore = new ArrayList<>();
                lore.add(TooltipUtil.sectionHeader("Discovered Food"));
                lore.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Recipe: " + ChatColor.WHITE + entry.recipe().displayName()));
                lore.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Output: " + ChatColor.WHITE + entry.reward().amount() + "x"));
                lore.add(" ");
                lore.addAll(TooltipUtil.bulletList("Craft ingredient chains to discover more meals."));
                meta.setLore(lore);
            } else {
                meta.setDisplayName(ChatColor.DARK_GRAY + "???");
                meta.setLore(TooltipUtil.bulletList("Cook this food to reveal it."));
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!GuiUtil.titleMatches(event.getView().getTitle(), TITLE)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (handleWidgetClick(event, player)) return;
        event.setCancelled(true);
    }

    private List<GuiWidget> buildWidgets() {
        List<GuiWidget> widgetList = new ArrayList<>();
        widgetList.add(new ActionWidget(PREV_SLOT,
                context -> createPageItem(context.player(), true),
                (click, context) -> open(context.player(), pageMap.getOrDefault(context.player().getUniqueId(), 0) - 1)));
        widgetList.add(new ActionWidget(NEXT_SLOT,
                context -> createPageItem(context.player(), false),
                (click, context) -> open(context.player(), pageMap.getOrDefault(context.player().getUniqueId(), 0) + 1)));
        widgetList.add(new ActionWidget(BACK_SLOT,
                context -> GuiUtil.getNexoItem("arrow_left", ChatColor.YELLOW + "Back"),
                (click, context) -> {
                    if (mainGui != null) {
                        mainGui.open(context.player());
                    }
                }));
        return widgetList;
    }

    private ItemStack createPageItem(Player player, boolean previous) {
        int page = pageMap.getOrDefault(player.getUniqueId(), 0) + 1;
        String name = previous ? "Previous Page" : "Next Page";
        return GuiUtil.getNexoItem(previous ? "arrow_left" : "arrow_right", ChatColor.YELLOW + name,
                TooltipUtil.bulletList("Current page: " + page));
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
        GuiWidget widget = widgets.stream().filter(w -> w.handlesSlot(slot)).findFirst().orElse(null);
        if (widget == null) {
            return false;
        }
        event.setCancelled(true);
        widget.onClick(slot, event.getClick(), new GuiContext(player, event.getView().getTopInventory()));
        return true;
    }


    private record FoodEntry(String key, String displayName, CookingRecipe recipe, CookingReward reward) {}
}
