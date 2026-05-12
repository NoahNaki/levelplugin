package me.nakilex.levelplugin.spells.gui;

import me.nakilex.levelplugin.spells.deck.SpellCardCategory;
import me.nakilex.levelplugin.spells.deck.SpellCardDefinition;
import me.nakilex.levelplugin.spells.deck.SpellDeckManager;
import me.nakilex.levelplugin.spells.deck.SpellDeckProfile;
import me.nakilex.levelplugin.spells.SpellCastManager;
import me.nakilex.levelplugin.spells.SpellDefinition;
import me.nakilex.levelplugin.spells.SpellRegistry;
import me.nakilex.levelplugin.spells.deck.SpellDeckRarity;
import me.nakilex.levelplugin.spells.input.SpellInputType;
import me.nakilex.levelplugin.spells.progression.SpellProgressionManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpellUpgradeGUI implements Listener {
    private static final int MAIN_GUI_SIZE = 27;
    private static final int SELECT_GUI_SIZE = 45;
    private static final int[] SELECT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };
    private static final int PAGE_SIZE = SELECT_SLOTS.length;
    private static final int BACK_SLOT = 36;
    private static final int PREV_SLOT = 37;
    private static final int NEXT_SLOT = 43;
    private static final int INVEST_ALL_SLOT = 22;
    private static final int SORT_SLOT = 39;
    private static final int CATEGORY_FILTER_SLOT = 40;
    private static final int RARITY_FILTER_SLOT = 41;
    private static final int[] SPELL_SLOTS = {10, 12, 14, 16};
    private static final SpellInputType[] EQUIP_INPUTS = {
            SpellInputType.SPELL_1,
            SpellInputType.SPELL_2,
            SpellInputType.SPELL_3,
            SpellInputType.SPELL_4
    };
    private static final ChatColor SPELL_ACCENT = ChatColor.LIGHT_PURPLE;
    private static final Pattern IMPORTANT_TOKEN_PATTERN = Pattern.compile(
            "(?i)(\\b(?:damage|radius|stuns?|poisons?|burns?|burning|ignite[sd]?|inferno|chains?|charge|resistance|hp|movement|spread|explosions?|lose|projectile|speed)\\b|\\d+(?:\\.\\d+)?%?(?:/sec|s|x)?)");
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
        Inventory gui = GuiBuilder.create(MAIN_GUI_SIZE, titlePrefix)
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
        Inventory gui = GuiBuilder.create(SELECT_GUI_SIZE, selectTitlePrefix + " §8(" + ChatColor.WHITE + labelForInput(inputType) + "§8)")
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
            int slot = SELECT_SLOTS[i - start];
            widgets.add(new ActionWidget(slot, ctx -> createBrowserCardItem(ctx.player(), card, inputType),
                    (click, context) -> {
                        if (deckManager.equip(context.player(), inputType, card.cardId())) {
                            open(context.player());
                        } else {
                            openSelection(context.player(), inputType, pages.getOrDefault(context.player().getUniqueId(), 0));
                        }
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
        lore.add(TooltipUtil.labelValueLine("Pity", ChatColor.WHITE,
                deckManager.getPityPullsSinceLegendary(player.getUniqueId())
                        + "/" + deckManager.getPityThreshold() + " toward Legendary+"));
        return GuiUtil.createGuiItem(Material.ENCHANTED_BOOK, ChatColor.AQUA + "Spell Deck", lore);
    }

    private ItemStack createSpellSlotItem(Player player, SpellInputType inputType) {
        SpellCardDefinition equipped = deckManager.getEquippedCard(player.getUniqueId(), inputType);
        List<String> lore = new ArrayList<>();
        if (equipped == null) {
            lore.add(ChatColor.DARK_GRAY + "Slot: " + ChatColor.WHITE + labelForInput(inputType));
            lore.add(" ");
            lore.add(TooltipUtil.iconLabelValueLine("✖", ChatColor.RED, ChatColor.RED,
                    "Equipped", ChatColor.GRAY, "Empty"));
            lore.addAll(TooltipUtil.bulletList("Choose one of your unlocked spell cards."));
            lore.add(" ");
            lore.addAll(TooltipUtil.clickInstructions("to choose a spell", null));
            return GuiUtil.createGuiItem(Material.GRAY_DYE, ChatColor.GRAY + labelForInput(inputType) + " Slot", lore);
        }
        addCardSummaryLore(player, lore, equipped, inputType, true);
        lore.add(" ");
        lore.add(clickLine("to change", "this spell"));
        return createSpellCardGuiItem(equipped, equipped.rarity().color() + equipped.displayName().toUpperCase(Locale.ROOT), lore);
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
        lore.add(TooltipUtil.labelValueLine("Owned Cards", ChatColor.WHITE, String.valueOf(owned)));
        lore.add(TooltipUtil.labelValueLine("Duplicate Copies", ChatColor.WHITE, String.valueOf(duplicates)));
        lore.add(" ");
        lore.addAll(TooltipUtil.bulletList("Consumes every copy above the first.",
                "Invested copies are saved for future spell upgrades."));
        lore.add(" ");
        lore.addAll(TooltipUtil.clickInstructions("to invest all duplicates", null));
        return GuiUtil.createGuiItem(Material.AMETHYST_SHARD, ChatColor.LIGHT_PURPLE + "Invest Duplicate Spells", lore);
    }

    private ItemStack createBrowserCardItem(Player player, SpellCardDefinition card, SpellInputType targetSlot) {
        List<String> lore = new ArrayList<>();
        addCardSummaryLore(player, lore, card, targetSlot, false);
        SpellDeckProfile profile = deckManager.getProfile(player.getUniqueId());
        SpellInputType equippedSlot = profile == null ? null : deckManager.getEquippedSlotForFamily(profile, card.familyId());
        if (equippedSlot != null && equippedSlot != targetSlot) {
            lore.add(" ");
            lore.add(unavailableLine(equippedSlot));
        }
        lore.add(" ");
        lore.add(clickLine("to equip to", labelForInput(targetSlot)));
        return createSpellCardGuiItem(card, card.rarity().color() + card.displayName().toUpperCase(Locale.ROOT), lore);
    }

    private ItemStack createSpellCardGuiItem(SpellCardDefinition card, String name, List<String> lore) {
        Material material = card.displayMaterial() == null ? card.rarity().displayMaterial() : card.displayMaterial();
        ItemStack item = GuiUtil.createGuiItem(material, name, lore);
        ItemUtil.applyRarityTooltipStyle(item, card.rarity().itemRarity());
        TooltipUtil.centerItemName(item);
        return item;
    }

    private void addCardSummaryLore(Player player, List<String> lore, SpellCardDefinition card, SpellInputType inputType, boolean includeEquippedLine) {
        String effectiveSpellId = deckManager.getEffectiveSpellId(player.getUniqueId(), card);
        SpellRegistry.SpellEntry spellEntry = SpellRegistry.getInstance().getSpell(effectiveSpellId);
        SpellDefinition definition = spellEntry == null ? null : spellEntry.definition();
        int manaCost = definition == null
                ? readStatFromLore(card, "mana cost", 0)
                : SpellCastManager.getInstance().getManaCost(player, definition);
        long cooldownMs = definition == null
                ? readStatFromLore(card, "cooldown", 0) * 1000L
                : SpellCastManager.getInstance().getCooldownMs(player, definition);

        lore.add(TooltipUtil.rarityGlyphLine(card.rarity().itemRarity(), "spell"));
        lore.add(" ");
        lore.add(spellTypeLine(inputType));
        addActiveTierLore(lore, card, effectiveSpellId);
        for (String description : descriptionLines(card)) {
            for (String wrapped : TooltipUtil.wrapLoreLine(ChatColor.GRAY + description, 168)) {
                lore.add(wrapped);
            }
        }
        lore.add(" ");
        lore.add(statLine("Mana Cost", ChatColor.AQUA, manaCost + " mana"));
        lore.add(statLine("Cooldown", ChatColor.GREEN, formatCooldown(cooldownMs)));
        lore.add(" ");
        addMasteryLore(player, lore, card);
        lore.add(" ");
        lore.add(SPELL_ACCENT + "EFFECTS");
        List<String> effectLines = visibleEffectLines(card);
        if (effectLines.isEmpty()) {
            lore.add(TooltipUtil.bulletLine(ChatColor.GRAY + "No extra effect details."));
        } else {
            for (String line : effectLines) {
                lore.add(formatEffectLine(line));
            }
        }
        if (includeEquippedLine) {
            lore.add(" ");
            lore.add(TooltipUtil.selectionLine(true, "Equipped"));
        }
    }

    private void addMasteryLore(Player player, List<String> lore, SpellCardDefinition card) {
        SpellDeckProfile profile = deckManager.getProfile(player.getUniqueId());
        int progress = deckManager.getMasteryProgress(profile, card);
        int maxProgress = deckManager.getMaxMasteryProgress();
        int percent = maxProgress <= 0 ? 0 : (int) Math.round(progress * 100.0 / maxProgress);
        lore.add(SPELL_ACCENT + "MASTERY");
        if (progress >= maxProgress) {
            lore.add(ChatColor.GRAY + TooltipUtil.expProgressBarByPixels(1, 1, 168)
                    + ChatColor.GRAY + " " + ChatColor.WHITE + "100%" + ChatColor.GRAY + " Max");
            lore.add(ChatColor.GRAY + "Duplicate pulls auto-salvage: "
                    + ChatColor.LIGHT_PURPLE + deckManager.maxedDuplicateGemValue(card.rarity())
                    + " <glyph:purple_orb_icon>");
            return;
        }
        lore.add(TooltipUtil.expProgressBarByPixels(progress, maxProgress, 168) + " "
                + ChatColor.WHITE + percent + "%" + ChatColor.GRAY + " mastery");
        lore.add(ChatColor.GRAY + "Progress: " + ChatColor.WHITE + progress + ChatColor.GOLD + "/"
                + ChatColor.WHITE + maxProgress + ChatColor.GRAY + " duplicate value");
    }

    private String spellTypeLine(SpellInputType inputType) {
        return SPELL_ACCENT + labelForInput(inputType).toUpperCase(Locale.ROOT);
    }

    private void addActiveTierLore(List<String> lore, SpellCardDefinition card, String effectiveSpellId) {
        if (effectiveSpellId == null || effectiveSpellId.equalsIgnoreCase(card.spellId())) {
            return;
        }
        SpellRegistry.SpellEntry active = SpellRegistry.getInstance().getSpell(effectiveSpellId);
        String activeName = active == null || active.definition() == null
                ? effectiveSpellId
                : active.definition().displayName();
        int tier = activeRarityTier(card, effectiveSpellId);
        lore.add(ChatColor.GRAY + "Active Upgrade: " + ChatColor.AQUA + activeName
                + (tier > 0 ? ChatColor.DARK_GRAY + " (Rarity Tier " + tier + ")" : ""));
    }

    private int activeRarityTier(SpellCardDefinition card, String effectiveSpellId) {
        SpellProgressionManager progression = SpellProgressionManager.getInstance();
        int max = progression.getMaxLevel(card.spellId());
        for (int level = 1; level <= max; level++) {
            String candidate = progression.getSpellIdAtLevel(card.spellId(), level);
            if (candidate != null && candidate.equalsIgnoreCase(effectiveSpellId)) {
                return level;
            }
        }
        return 0;
    }

    private String statLine(String label, ChatColor valueColor, String value) {
        ChatColor resolvedValue = valueColor == null ? ChatColor.WHITE : valueColor;
        return ChatColor.GRAY + label.toUpperCase(Locale.ROOT) + ": " + resolvedValue + value;
    }

    private String unavailableLine(SpellInputType equippedSlot) {
        return ChatColor.RED + "UNAVAILABLE: "
                + ChatColor.GRAY + "ALREADY IN "
                + SPELL_ACCENT + labelForInput(equippedSlot).toUpperCase(Locale.ROOT);
    }

    private String clickLine(String action, String target) {
        ChatColor targetColor = "this spell".equalsIgnoreCase(target) ? ChatColor.GRAY : SPELL_ACCENT;
        return ChatColor.WHITE + "Left-click " + ChatColor.GRAY + action + " " + targetColor + target;
    }

    private String formatEffectLine(String line) {
        if (line == null || line.isBlank()) {
            return ChatColor.DARK_GRAY + "- " + ChatColor.GRAY;
        }
        String trimmed = line.trim();
        int separator = trimmed.indexOf(':');
        if (separator > 0) {
            String label = trimmed.substring(0, separator).trim();
            String value = trimmed.substring(separator + 1).trim();
            return ChatColor.DARK_GRAY + "- "
                    + highlightEffectLabel(label) + ChatColor.GRAY + ": "
                    + highlightEffectValue(label, value);
        }
        return ChatColor.DARK_GRAY + "- " + highlightImportant(trimmed);
    }

    private String highlightEffectLabel(String label) {
        if (label == null || label.isBlank()) {
            return ChatColor.GRAY.toString();
        }
        Matcher matcher = Pattern.compile("\\d+(?:\\.\\d+)?%").matcher(label);
        StringBuilder highlighted = new StringBuilder(ChatColor.GRAY.toString());
        int last = 0;
        while (matcher.find()) {
            highlighted.append(label, last, matcher.start());
            highlighted.append(ChatColor.WHITE).append(matcher.group()).append(ChatColor.GRAY);
            last = matcher.end();
        }
        highlighted.append(label.substring(last));
        return highlighted.toString();
    }

    private String highlightEffectValue(String label, String value) {
        if (value == null || value.isBlank()) {
            return ChatColor.GRAY.toString();
        }
        String normalizedLabel = label == null ? "" : label.toLowerCase(Locale.ROOT);
        ChatColor color = normalizedLabel.contains("damage") || normalizedLabel.contains("secondary") || normalizedLabel.contains("hit")
                ? ChatColor.RED
                : normalizedLabel.contains("radius") || normalizedLabel.contains("block") || normalizedLabel.contains("chain")
                ? ChatColor.YELLOW
                : normalizedLabel.contains("burn") || normalizedLabel.contains("cooldown") || normalizedLabel.contains("mana")
                ? ChatColor.GOLD
                : ChatColor.WHITE;
        Matcher matcher = Pattern.compile("(\\d+(?:\\.\\d+)?%?(?:/sec|s|x)?|\\bblocks?\\b|\\bmana\\b)", Pattern.CASE_INSENSITIVE).matcher(value);
        StringBuilder highlighted = new StringBuilder(ChatColor.GRAY.toString());
        int last = 0;
        while (matcher.find()) {
            highlighted.append(value, last, matcher.start());
            highlighted.append(color).append(matcher.group()).append(ChatColor.GRAY);
            last = matcher.end();
        }
        highlighted.append(value.substring(last));
        return highlighted.toString();
    }

    private List<String> descriptionLines(SpellCardDefinition card) {
        List<String> lines = new ArrayList<>();
        for (String line : card.effectLines()) {
            if (line == null || line.isBlank() || line.contains(":")) {
                continue;
            }
            lines.add(line.trim());
        }
        return lines;
    }

    private List<String> visibleEffectLines(SpellCardDefinition card) {
        List<String> lines = new ArrayList<>();
        for (String line : card.effectLines()) {
            if (line == null || line.isBlank() || isManaOrCooldownLine(line) || !line.contains(":")) {
                continue;
            }
            lines.add(line);
        }
        for (String line : card.tradeoffLines()) {
            if (isManaOrCooldownLine(line)) {
                continue;
            }
            lines.add(line);
        }
        return lines;
    }

    private boolean isManaOrCooldownLine(String line) {
        if (line == null) {
            return false;
        }
        String normalized = line.toLowerCase(Locale.ROOT);
        return normalized.contains("mana cost") || normalized.contains("cooldown");
    }

    private int readStatFromLore(SpellCardDefinition card, String label, int fallback) {
        for (String line : card.effectLines()) {
            Integer value = readFirstNumberAfterLabel(line, label);
            if (value != null) {
                return value;
            }
        }
        for (String line : card.tradeoffLines()) {
            Integer value = readFirstNumberAfterLabel(line, label);
            if (value != null) {
                return value;
            }
        }
        return fallback;
    }

    private Integer readFirstNumberAfterLabel(String line, String label) {
        if (line == null || label == null) {
            return null;
        }
        String normalized = line.toLowerCase(Locale.ROOT);
        int index = normalized.indexOf(label);
        if (index < 0) {
            return null;
        }
        Matcher matcher = Pattern.compile("\\d+").matcher(line.substring(index));
        return matcher.find() ? Integer.parseInt(matcher.group()) : null;
    }

    private String formatCooldown(long cooldownMs) {
        double seconds = Math.max(0L, cooldownMs) / 1000.0;
        if (Math.abs(seconds - Math.rint(seconds)) < 0.001) {
            return (int) Math.rint(seconds) + "s";
        }
        return String.format(Locale.ROOT, "%.1fs", seconds);
    }

    private String highlightImportant(String line) {
        if (line == null || line.isBlank()) {
            return ChatColor.GRAY.toString();
        }
        Matcher matcher = IMPORTANT_TOKEN_PATTERN.matcher(line);
        StringBuilder highlighted = new StringBuilder(ChatColor.GRAY.toString());
        int last = 0;
        while (matcher.find()) {
            highlighted.append(line, last, matcher.start());
            highlighted.append(ChatColor.WHITE).append(matcher.group()).append(ChatColor.GRAY);
            last = matcher.end();
        }
        highlighted.append(line.substring(last));
        return highlighted.toString();
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
        if (!isDeckTitle(viewTitle)) {
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


    private boolean isDeckTitle(String viewTitle) {
        String normalized = GuiUtil.normalizeTitle(viewTitle);
        return normalized.equalsIgnoreCase("Spell Deck")
                || normalized.startsWith("Select Spell");
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
