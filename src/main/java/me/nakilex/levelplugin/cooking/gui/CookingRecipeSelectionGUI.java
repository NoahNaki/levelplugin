package me.nakilex.levelplugin.cooking.gui;

import me.nakilex.levelplugin.cooking.model.CookingIngredientRequirement;
import me.nakilex.levelplugin.cooking.model.CookingRecipe;
import me.nakilex.levelplugin.cooking.model.CookingWorkstationType;
import me.nakilex.levelplugin.cooking.registry.CookingRecipeRegistry;
import me.nakilex.levelplugin.cooking.runtime.ActiveCookingSessionRegistry;
import me.nakilex.levelplugin.cooking.service.CookingSessionService;
import me.nakilex.levelplugin.cooking.util.CookingIngredientMatcher;
import me.nakilex.levelplugin.cooking.runtime.PlacedCookingWorkstation;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import me.nakilex.levelplugin.utils.gui.widgets.ActionWidget;
import me.nakilex.levelplugin.utils.gui.widgets.GuiContext;
import me.nakilex.levelplugin.utils.gui.widgets.GuiLayout;
import me.nakilex.levelplugin.utils.gui.widgets.GuiWidget;
import me.nakilex.levelplugin.utils.gui.widgets.ToggleFilterWidget;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Recipe picker for a placed cooking workstation, including ingredient previews and craftable filtering. */
public class CookingRecipeSelectionGUI implements Listener {
    private static final int SIZE = 54;
    private static final int PAGE_SIZE = GuiUtil.PAGED_SLOTS.length;
    private static final int PREV_SLOT = 45;
    private static final int NEXT_SLOT = 53;
    private static final int FILTER_SLOT = 49;
    private static final String TITLE_PREFIX = ChatColor.DARK_GRAY + "Cooking Recipes";
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final CookingRecipeRegistry recipeRegistry;
    private final CookingSessionService sessionService;
    private final Map<UUID, Integer> pages = new HashMap<>();
    private final Map<UUID, List<GuiWidget>> widgetsByPlayer = new HashMap<>();
    private final Map<UUID, Boolean> craftableOnlyByPlayer = new HashMap<>();

    public CookingRecipeSelectionGUI(CookingRecipeRegistry recipeRegistry, CookingSessionService sessionService) {
        this.recipeRegistry = recipeRegistry;
        this.sessionService = sessionService;
    }

    public void open(Player player, PlacedCookingWorkstation workstation) {
        open(player, workstation, pages.getOrDefault(player.getUniqueId(), 0));
    }

    private void open(Player player, PlacedCookingWorkstation workstation, int page) {
        boolean craftableOnly = craftableOnlyByPlayer.getOrDefault(player.getUniqueId(), false);
        List<CookingRecipe> recipes = recipesFor(player, workstation.type(), craftableOnly);
        int maxPage = Math.max(0, (recipes.size() - 1) / PAGE_SIZE);
        int current = Math.max(0, Math.min(page, maxPage));
        UUID playerId = player.getUniqueId();
        pages.put(playerId, current);

        Inventory inventory = GuiBuilder.create(SIZE, TITLE_PREFIX)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .border()
                .build();
        List<GuiWidget> widgets = buildWidgets(player, workstation, recipes, current, maxPage, craftableOnly);
        widgetsByPlayer.put(playerId, widgets);
        renderWidgets(inventory, player, widgets);
        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        String title = LEGACY.serialize(event.getView().title());
        if (!GuiUtil.titleStartsWith(title, TITLE_PREFIX)) {
            return;
        }
        if (!handleWidgetClick(event, player)) {
            event.setCancelled(true);
        }
    }

    private List<GuiWidget> buildWidgets(Player player, PlacedCookingWorkstation workstation,
                                         List<CookingRecipe> recipes, int page, int maxPage, boolean craftableOnly) {
        List<GuiWidget> widgets = new ArrayList<>();
        widgets.add(new ToggleFilterWidget(FILTER_SLOT, () -> craftableOnlyByPlayer.getOrDefault(player.getUniqueId(), false),
                ChatColor.GREEN + "Craftable Filter",
                (click, context) -> toggleCraftableFilter(player, workstation),
                ChatColor.GRAY + "Show only recipes you can make",
                ChatColor.GRAY + "with your current inventory.",
                " ",
                ChatColor.YELLOW + "Click to toggle."));
        if (recipes.isEmpty()) {
            widgets.add(new ActionWidget(22, ctx -> emptyItem(craftableOnly), (click, context) -> {}));
            return widgets;
        }
        int start = page * PAGE_SIZE;
        int end = Math.min(recipes.size(), start + PAGE_SIZE);
        for (int i = start; i < end; i++) {
            CookingRecipe recipe = recipes.get(i);
            int slot = GuiUtil.PAGED_SLOTS[i - start];
            widgets.add(new ActionWidget(slot, ctx -> recipeItem(player, recipe),
                    (click, context) -> selectRecipe(player, workstation, recipe)));
        }
        if (page > 0) {
            widgets.add(new ActionWidget(PREV_SLOT, ctx -> navItem(false),
                    (click, context) -> open(player, workstation, page - 1)));
        }
        if (page < maxPage) {
            widgets.add(new ActionWidget(NEXT_SLOT, ctx -> navItem(true),
                    (click, context) -> open(player, workstation, page + 1)));
        }
        return widgets;
    }

    private void selectRecipe(Player player, PlacedCookingWorkstation workstation, CookingRecipe recipe) {
        ActiveCookingSessionRegistry.CreateResult result = sessionService.startSession(player, workstation, recipe);
        switch (result) {
            case CREATED -> {
                player.closeInventory();
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                        "Selected cooking recipe " + ChatColor.YELLOW + recipe.displayName()
                                + ChatColor.GREEN + ". Add ingredients by right-clicking the workstation.");
            }
            case PLAYER_BUSY -> ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "You already have an active cooking session.");
            case WORKSTATION_BUSY -> ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "This cooking workstation is busy.");
            case INVALID -> ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Could not start this cooking session.");
        }
    }

    private boolean handleWidgetClick(InventoryClickEvent event, Player player) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) {
            return false;
        }
        List<GuiWidget> widgets = widgetsByPlayer.getOrDefault(player.getUniqueId(), List.of());
        GuiWidget widget = widgets.stream()
                .filter(candidate -> candidate.handlesSlot(slot))
                .findFirst()
                .orElse(null);
        if (widget == null) {
            return false;
        }
        event.setCancelled(true);
        widget.onClick(slot, event.getClick(), new GuiContext(player, event.getView().getTopInventory()));
        return true;
    }

    private void renderWidgets(Inventory inventory, Player player, List<GuiWidget> widgets) {
        GuiLayout layout = new GuiLayout(inventory);
        GuiContext context = new GuiContext(player, inventory);
        for (GuiWidget widget : widgets) {
            widget.contribute(layout, context);
        }
    }

    private void toggleCraftableFilter(Player player, PlacedCookingWorkstation workstation) {
        UUID playerId = player.getUniqueId();
        boolean next = !craftableOnlyByPlayer.getOrDefault(playerId, false);
        craftableOnlyByPlayer.put(playerId, next);
        open(player, workstation, 0);
    }

    private List<CookingRecipe> recipesFor(Player player, CookingWorkstationType type, boolean craftableOnly) {
        List<CookingRecipe> recipes = new ArrayList<>();
        for (String recipeId : type.recipeIds()) {
            recipeRegistry.get(recipeId)
                    .filter(recipe -> !craftableOnly || CookingIngredientMatcher.hasIngredients(player.getInventory(), recipe))
                    .ifPresent(recipes::add);
        }
        return recipes;
    }

    private ItemStack recipeItem(Player player, CookingRecipe recipe) {
        List<String> lore = new ArrayList<>();
        if (!recipe.lore().isEmpty()) {
            lore.add(" ");
            for (String line : recipe.lore()) {
                lore.addAll(TooltipUtil.wrapLoreLine(ChatColor.GRAY + line, 210));
            }
        }
        addRequiredIngredientsLore(player, recipe, lore);
        lore.add(" ");
        lore.addAll(TooltipUtil.clickInstructions("to select this recipe", null));
        ItemStack item = recipe.displayItem();
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + recipe.displayName());
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void addRequiredIngredientsLore(Player player, CookingRecipe recipe, List<String> lore) {
        List<CookingIngredientRequirement> requirements = CookingIngredientMatcher.aggregateRequirements(recipe);
        if (requirements.isEmpty()) {
            return;
        }
        lore.add(" ");
        lore.add(ChatColor.GOLD + "Required Ingredients:");
        for (CookingIngredientRequirement requirement : requirements) {
            int available = CookingIngredientMatcher.countMatching(player.getInventory(), requirement);
            ChatColor color = available >= requirement.amount() ? ChatColor.GREEN : ChatColor.RED;
            lore.add(color + CookingIngredientMatcher.formatRequirement(requirement)
                    + ChatColor.DARK_GRAY + " (" + Math.min(available, requirement.amount()) + "/" + requirement.amount() + ")");
        }
    }

    private ItemStack emptyItem(boolean craftableOnly) {
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        String message = craftableOnly
                ? "No recipes match your current inventory."
                : "No configured recipes are available for this workstation.";
        lore.addAll(TooltipUtil.bulletList(message));
        return GuiUtil.createGuiItem(Material.BARRIER, ChatColor.RED + "No Recipes", lore);
    }

    private ItemStack navItem(boolean next) {
        return GuiUtil.getNexoItem(next ? "arrow_right" : "arrow_left",
                next ? ChatColor.GREEN + "Next Page" : ChatColor.GREEN + "Previous Page",
                TooltipUtil.clickInstructions("to change page", null));
    }
}
