package me.nakilex.levelplugin.spells.gui;

import me.nakilex.levelplugin.spells.deck.SpellCardCategory;
import me.nakilex.levelplugin.spells.deck.SpellCardDefinition;
import me.nakilex.levelplugin.spells.deck.SpellDeckManager;
import me.nakilex.levelplugin.spells.deck.SpellDeckProfile;
import me.nakilex.levelplugin.spells.deck.SpellDeckRarity;
import me.nakilex.levelplugin.spells.input.SpellInputType;
import me.nakilex.levelplugin.utils.ChatUtil;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import me.nakilex.levelplugin.utils.gui.widgets.ActionWidget;
import me.nakilex.levelplugin.utils.gui.widgets.GuiContext;
import me.nakilex.levelplugin.utils.gui.widgets.GuiLayout;
import me.nakilex.levelplugin.utils.gui.widgets.GuiWidget;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SpellUpgradeGUI implements Listener {
    private static final int GUI_SIZE = 54;
    private static final int PAGE_SIZE = GuiUtil.PAGED_SLOTS.length;
    private static final int PREV_SLOT = 45;
    private static final int NEXT_SLOT = 53;
    private static final int BACK_SLOT = 45;
    private static final int INVEST_ALL_SLOT = 49;
    private static final int SORT_SLOT = 51;
    private static final int CATEGORY_FILTER_SLOT = 50;
    private static final int RARITY_FILTER_SLOT = 52;
    private static final int[] SPELL_SLOTS = {20, 21, 23, 24};
    private static final SpellInputType[] EQUIP_INPUTS = {
            SpellInputType.SPELL_1,
            SpellInputType.SPELL_2,
            SpellInputType.SPELL_3,
            SpellInputType.SPELL_4
    };
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final SpellDeckManager deckManager = SpellDeckManager.getInstance();
    private final String titlePrefix = ChatUtil.applyEmojis("§8Spell Deck");
    private final String selectTitlePrefix = ChatUtil.applyEmojis("§8Select Spell");
    private final Map<UUID, List<GuiWidget>> widgetsByPlayer = new HashMap<>();
    private final Map<UUID, Integer> pages = new HashMap<>();
    private final Map<UUID, SpellDeckRarity> rarityFilterByPlayer = new HashMap<>();
    private final Map<UUID, SpellCardCategory> categoryFilterByPlayer = new HashMap<>();
    private final Map<UUID, SortMode> sortByPlayer = new HashMap<>();
    private final Map<UUID, SpellInputType> selectingSlotByPlayer = new HashMap<>();

    public void open(Player player) {
        Inventory gui = GuiBuilder.create(GUI_SIZE, titlePrefix)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .border()
                .build();
        List<GuiWidget> widgets = buildMainWidgets(player);
        widgetsByPlayer.put(player.getUniqueId(), widgets);
        render(gui, player, widgets);
        player.openInventory(gui);
    }

    private void openSelection(Player player, SpellInputType inputType, int page) {
        selectingSlotByPlayer.put(player.getUniqueId(), inputType);
        List<SpellCardDefinition> cards = applySortAndFilter(player, deckManager.getOwnedCards(player.getUniqueId()));
        int maxPage = Math.max(0, (cards.size() - 1) / PAGE_SIZE);
        int current = Math.max(0, Math.min(page, maxPage));
        pages.put(player.getUniqueId(), current);
        Inventory gui = GuiBuilder.create(GUI_SIZE, selectTitlePrefix + " §8(" + ChatColor.WHITE + labelForInput(inputType) + "§8)")
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .border()
                .build();
        List<GuiWidget> widgets = buildSelectionWidgets(player, inputType, cards, current, maxPage);
        widgetsByPlayer.put(player.getUniqueId(), widgets);
        render(gui, player, widgets);
        player.openInventory(gui);
    }

    private List<GuiWidget> buildMainWidgets(Player player) {
        List<GuiWidget> widgets = new ArrayList<>();
        widgets.add(new ActionWidget(4, ctx -> createInfoItem(ctx.player()), (click, context) -> {}));
        for (int i = 0; i < EQUIP_INPUTS.length; i++) {
            SpellInputType input = EQUIP_INPUTS[i];
            int slot = SPELL_SLOTS[i];
            widgets.add(new ActionWidget(slot, ctx -> createSpellSlotItem(ctx.player(), input),
                    (click, context) -> openSelection(context.player(), input, 0)));
        }
        widgets.add(new ActionWidget(INVEST_ALL_SLOT, ctx -> createInvestAllItem(ctx.player()),
                (click, context) -> {
                    deckManager.investAllDuplicateCopies(context.player());
                    open(context.player());
                }));
        return widgets;
    }

    private List<GuiWidget> buildSelectionWidgets(Player player,
                                                  SpellInputType inputType,
                                                  List<SpellCardDefinition> cards,
                                                  int page,
                                                  int maxPage) {
        List<GuiWidget> widgets = new ArrayList<>();
        if (cards.isEmpty()) {
            widgets.add(new ActionWidget(22, ctx -> createEmptySelectionItem(), (click, context) -> {}));
        }
        int start = page * PAGE_SIZE;
        int end = Math.min(cards.size(), start + PAGE_SIZE);
        for (int i = start; i < end; i++) {
            SpellCardDefinition card = cards.get(i);
            int slot = GuiUtil.PAGED_SLOTS[i - start];
            widgets.add(new ActionWidget(slot, ctx -> createBrowserCardItem(ctx.player(), card, inputType),
                    (click, context) -> {
                        deckManager.equip(context.player(), inputType, card.cardId());
                        open(context.player());
                    }));
        }
        if (page > 0) {
            widgets.add(new ActionWidget(PREV_SLOT, ctx -> createNavItem(false),
                    (click, context) -> openSelection(context.player(), inputType, page - 1)));
        }
        if (page < maxPage) {
            widgets.add(new ActionWidget(NEXT_SLOT, ctx -> createNavItem(true),
                    (click, context) -> openSelection(context.player(), inputType, page + 1)));
        }
        widgets.add(new ActionWidget(BACK_SLOT, ctx -> GuiUtil.getNexoItem("arrow_left", "§eBack",
                TooltipUtil.clickInstructions("to return to spell slots", null)),
                (click, context) -> open(context.player())));
        widgets.add(new ActionWidget(SORT_SLOT, ctx -> sortButton(ctx.player()), (click, context) -> {
            UUID id = context.player().getUniqueId();
            SortMode next = SortMode.next(sortByPlayer.getOrDefault(id, SortMode.RARITY), click.isLeftClick());
            sortByPlayer.put(id, next);
            openSelection(context.player(), inputType, 0);
        }));
        widgets.add(new ActionWidget(CATEGORY_FILTER_SLOT, ctx -> categoryFilterButton(ctx.player()), (click, context) -> {
            UUID id = context.player().getUniqueId();
            categoryFilterByPlayer.put(id, nextCategoryFilter(categoryFilterByPlayer.get(id), click.isLeftClick()));
            openSelection(context.player(), inputType, 0);
        }));
        widgets.add(new ActionWidget(RARITY_FILTER_SLOT, ctx -> rarityFilterButton(ctx.player()), (click, context) -> {
            UUID id = context.player().getUniqueId();
            rarityFilterByPlayer.put(id, nextRarityFilter(rarityFilterByPlayer.get(id), click.isLeftClick()));
            openSelection(context.player(), inputType, 0);
        }));
        return widgets;
    }

    private ItemStack createInfoItem(Player player) {
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.addAll(TooltipUtil.bulletList(
                "Equip up to four spell cards at once.",
                "Click a gray slot to choose an unlocked spell.",
                "Use /debug spellpull <amount> to test pulls."));
        lore.add(" ");
        lore.add(TooltipUtil.iconLabelValueLine("✦", ChatColor.AQUA, ChatColor.AQUA,
                "Pity", ChatColor.WHITE, deckManager.getPityPullsSinceLegendary(player.getUniqueId())
                        + "/" + deckManager.getPityThreshold() + " toward Legendary+"));
        return GuiUtil.createGuiItem(Material.ENCHANTED_BOOK, ChatColor.AQUA + "Spell Deck", lore);
    }

    private ItemStack createSpellSlotItem(Player player, SpellInputType inputType) {
        SpellCardDefinition equipped = deckManager.getEquippedCard(player.getUniqueId(), inputType);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "Slot: " + ChatColor.WHITE + labelForInput(inputType));
        lore.add(" ");
        if (equipped == null) {
            lore.add(TooltipUtil.iconLabelValueLine("✖", ChatColor.RED, ChatColor.RED,
                    "Equipped", ChatColor.GRAY, "Empty"));
            lore.addAll(TooltipUtil.bulletList("Choose one of your unlocked spell cards."));
            lore.add(" ");
            lore.addAll(TooltipUtil.clickInstructions("to choose a spell", null));
            return GuiUtil.createGuiItem(Material.GRAY_DYE, ChatColor.GRAY + labelForInput(inputType) + " Slot", lore);
        }
        addCardSummaryLore(player, lore, equipped, true);
        lore.add(" ");
        lore.addAll(TooltipUtil.clickInstructions("to change this spell", null));
        return GuiUtil.createGuiItem(equipped.rarity().displayMaterial(),
                equipped.rarity().color() + labelForInput(inputType) + ": " + equipped.displayName(), lore);
    }

    private ItemStack createInvestAllItem(Player player) {
        int duplicates = 0;
        int owned = 0;
        for (SpellCardDefinition card : deckManager.getDefinitions()) {
            int copies = deckManager.getCopies(player.getUniqueId(), card.cardId());
            if (copies > 0) {
                owned++;
            }
            duplicates += Math.max(0, copies - 1);
        }
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add(TooltipUtil.iconLabelValueLine("✦", ChatColor.AQUA, ChatColor.AQUA,
                "Owned Cards", ChatColor.WHITE, String.valueOf(owned)));
        lore.add(TooltipUtil.iconLabelValueLine("◆", ChatColor.GOLD, ChatColor.GOLD,
                "Duplicate Copies", ChatColor.WHITE, String.valueOf(duplicates)));
        lore.add(" ");
        lore.addAll(TooltipUtil.bulletList("Consumes every copy above the first.",
                "Invested copies are saved for future spell upgrades."));
        lore.add(" ");
        lore.addAll(TooltipUtil.clickInstructions("to invest all duplicates", null));
        return GuiUtil.createGuiItem(Material.AMETHYST_SHARD, ChatColor.LIGHT_PURPLE + "Invest Duplicate Spells", lore);
    }

    private ItemStack createBrowserCardItem(Player player, SpellCardDefinition card, SpellInputType targetSlot) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "Target Slot: " + ChatColor.WHITE + labelForInput(targetSlot));
        addCardSummaryLore(player, lore, card, false);
        lore.add(" ");
        lore.add(ChatColor.WHITE + "Effects");
        for (String line : card.effectLines()) {
            lore.add(TooltipUtil.bulletLine(ChatColor.GRAY + line));
        }
        if (!card.tradeoffLines().isEmpty()) {
            lore.add(" ");
            lore.add(ChatColor.WHITE + "Tradeoff");
            for (String line : card.tradeoffLines()) {
                lore.add(TooltipUtil.bulletLine(ChatColor.GRAY + line));
            }
        }
        lore.add(" ");
        lore.addAll(TooltipUtil.clickInstructions("to equip to " + labelForInput(targetSlot), null));
        return GuiUtil.createGuiItem(card.rarity().displayMaterial(), card.rarity().color() + card.displayName(), lore);
    }

    private void addCardSummaryLore(Player player, List<String> lore, SpellCardDefinition card, boolean includeEquippedLine) {
        SpellDeckProfile profile = deckManager.getProfile(player.getUniqueId());
        int copies = profile == null ? 0 : profile.getCopies(card.cardId());
        int invested = profile == null ? 0 : profile.getInvestedCopies(card.cardId());
        lore.add(TooltipUtil.iconLabelValueLine("◆", card.rarity().color(), card.rarity().color(),
                "Rarity", ChatColor.WHITE, card.rarity().displayName()));
        lore.add(TooltipUtil.iconLabelValueLine("✦", card.category().color(), card.category().color(),
                "Category", ChatColor.WHITE, card.category().displayName()));
        lore.add(TooltipUtil.iconLabelValueLine("✚", ChatColor.GREEN, ChatColor.GREEN,
                "Copies", ChatColor.WHITE, String.valueOf(copies)));
        lore.add(TooltipUtil.iconLabelValueLine("✧", ChatColor.LIGHT_PURPLE, ChatColor.LIGHT_PURPLE,
                "Invested", ChatColor.WHITE, String.valueOf(invested)));
        if (includeEquippedLine) {
            lore.add(TooltipUtil.selectionLine(true, "Equipped"));
        }
    }

    private ItemStack createEmptySelectionItem() {
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.addAll(TooltipUtil.bulletList("No unlocked spell cards match the current filters.",
                "Pull cards or change your filters."));
        return GuiUtil.createGuiItem(Material.BARRIER, ChatColor.RED + "No Spell Cards", lore);
    }

    private ItemStack createNavItem(boolean next) {
        return GuiUtil.getNexoItem(next ? "arrow_right" : "arrow_left",
                next ? "§aNext Page" : "§aPrevious Page",
                TooltipUtil.clickInstructions("to change page", null));
    }

    private ItemStack sortButton(Player player) {
        SortMode mode = sortByPlayer.getOrDefault(player.getUniqueId(), SortMode.RARITY);
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        for (SortMode value : SortMode.values()) {
            lore.add(TooltipUtil.selectionLine(value == mode, value.label));
        }
        lore.add(" ");
        lore.addAll(TooltipUtil.clickInstructions("to cycle forward", "to cycle backward"));
        return GuiUtil.createGuiItem(Material.COMPARATOR, "§bSort", lore);
    }

    private ItemStack rarityFilterButton(Player player) {
        SpellDeckRarity filter = rarityFilterByPlayer.get(player.getUniqueId());
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add(TooltipUtil.selectionLine(filter == null, "All Rarities"));
        for (SpellDeckRarity rarity : SpellDeckRarity.values()) {
            lore.add(TooltipUtil.selectionLine(filter == rarity, rarity.color() + rarity.displayName()));
        }
        lore.add(" ");
        lore.addAll(TooltipUtil.clickInstructions("to cycle forward", "to cycle backward"));
        return GuiUtil.createGuiItem(Material.HOPPER, "§bFilter Rarity", lore);
    }

    private ItemStack categoryFilterButton(Player player) {
        SpellCardCategory filter = categoryFilterByPlayer.get(player.getUniqueId());
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add(TooltipUtil.selectionLine(filter == null, "All Categories"));
        for (SpellCardCategory category : SpellCardCategory.values()) {
            lore.add(TooltipUtil.selectionLine(filter == category, category.color() + category.displayName()));
        }
        lore.add(" ");
        lore.addAll(TooltipUtil.clickInstructions("to cycle forward", "to cycle backward"));
        return GuiUtil.createGuiItem(Material.BOOK, "§bFilter Category", lore);
    }

    private List<SpellCardDefinition> applySortAndFilter(Player player, List<SpellCardDefinition> cards) {
        UUID id = player.getUniqueId();
        SpellDeckRarity rarityFilter = rarityFilterByPlayer.get(id);
        SpellCardCategory categoryFilter = categoryFilterByPlayer.get(id);
        SortMode sortMode = sortByPlayer.getOrDefault(id, SortMode.RARITY);
        SpellDeckProfile profile = deckManager.getProfile(id);
        List<SpellCardDefinition> filtered = new ArrayList<>();
        for (SpellCardDefinition card : cards) {
            if (rarityFilter != null && card.rarity() != rarityFilter) {
                continue;
            }
            if (categoryFilter != null && card.category() != categoryFilter) {
                continue;
            }
            filtered.add(card);
        }
        filtered.sort(comparatorFor(sortMode, profile));
        return filtered;
    }

    private Comparator<SpellCardDefinition> comparatorFor(SortMode mode, SpellDeckProfile profile) {
        return switch (mode) {
            case NAME -> Comparator.comparing(SpellCardDefinition::displayName, String.CASE_INSENSITIVE_ORDER);
            case RARITY -> Comparator.comparingInt((SpellCardDefinition card) -> card.rarity().ordinal()).reversed()
                    .thenComparing(SpellCardDefinition::displayName, String.CASE_INSENSITIVE_ORDER);
            case CATEGORY -> Comparator.comparing((SpellCardDefinition card) -> card.category().displayName())
                    .thenComparing(SpellCardDefinition::displayName, String.CASE_INSENSITIVE_ORDER);
            case COPIES -> Comparator.comparingInt((SpellCardDefinition card) -> profile == null ? 0 : profile.getCopies(card.cardId())).reversed()
                    .thenComparing(SpellCardDefinition::displayName, String.CASE_INSENSITIVE_ORDER);
            case INVESTED -> Comparator.comparingInt((SpellCardDefinition card) -> profile == null ? 0 : profile.getInvestedCopies(card.cardId())).reversed()
                    .thenComparing(SpellCardDefinition::displayName, String.CASE_INSENSITIVE_ORDER);
        };
    }

    private SpellDeckRarity nextRarityFilter(SpellDeckRarity current, boolean forward) {
        List<SpellDeckRarity> order = new ArrayList<>();
        order.add(null);
        order.addAll(List.of(SpellDeckRarity.values()));
        return cycle(order, current, forward);
    }

    private SpellCardCategory nextCategoryFilter(SpellCardCategory current, boolean forward) {
        List<SpellCardCategory> order = new ArrayList<>();
        order.add(null);
        order.addAll(List.of(SpellCardCategory.values()));
        return cycle(order, current, forward);
    }

    private <T> T cycle(List<T> order, T current, boolean forward) {
        int idx = order.indexOf(current);
        idx = forward ? idx + 1 : idx - 1;
        if (idx >= order.size()) idx = 0;
        if (idx < 0) idx = order.size() - 1;
        return order.get(idx);
    }

    private void render(Inventory gui, Player player, List<GuiWidget> widgets) {
        GuiLayout layout = new GuiLayout(gui);
        GuiContext context = new GuiContext(player, gui);
        for (GuiWidget widget : widgets) {
            widget.contribute(layout, context);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        String viewTitle = LEGACY.serialize(event.getView().title());
        if (!GuiUtil.titleStartsWith(viewTitle, titlePrefix) && !GuiUtil.titleStartsWith(viewTitle, selectTitlePrefix)) {
            return;
        }
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) {
            return;
        }
        List<GuiWidget> widgets = widgetsByPlayer.get(player.getUniqueId());
        if (widgets == null) {
            return;
        }
        GuiWidget widget = widgets.stream()
                .filter(candidate -> candidate.handlesSlot(slot))
                .findFirst()
                .orElse(null);
        if (widget != null) {
            widget.onClick(slot, event.getClick(), new GuiContext(player, event.getView().getTopInventory()));
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        String viewTitle = LEGACY.serialize(event.getView().title());
        if (GuiUtil.titleStartsWith(viewTitle, titlePrefix) || GuiUtil.titleStartsWith(viewTitle, selectTitlePrefix)) {
            UUID id = event.getPlayer().getUniqueId();
            widgetsByPlayer.remove(id);
        }
    }

    private String labelForInput(SpellInputType inputType) {
        return switch (inputType) {
            case SPELL_1 -> "Spell 1";
            case SPELL_2 -> "Spell 2";
            case SPELL_3 -> "Spell 3";
            case SPELL_4 -> "Spell 4";
            case BASIC_ATTACK -> "Basic Attack";
        };
    }

    private enum SortMode {
        RARITY("Rarity"),
        NAME("Name"),
        CATEGORY("Category"),
        COPIES("Copies"),
        INVESTED("Invested");

        private final String label;

        SortMode(String label) {
            this.label = label;
        }

        private static SortMode next(SortMode current, boolean forward) {
            SortMode[] values = values();
            int index = current.ordinal() + (forward ? 1 : -1);
            if (index >= values.length) index = 0;
            if (index < 0) index = values.length - 1;
            return values[index];
        }
    }
}
