package me.nakilex.levelplugin.xprison;

import dev.drawethree.xprison.XPrison;
import dev.drawethree.xprison.api.XPrisonAPI;
import dev.drawethree.xprison.api.currency.model.XPrisonCurrency;
import dev.drawethree.xprison.api.enchants.model.ChanceBasedEnchant;
import dev.drawethree.xprison.api.enchants.model.PrestigeableEnchant;
import dev.drawethree.xprison.api.enchants.model.RequiresPickaxeLevel;
import dev.drawethree.xprison.api.enchants.model.XPrisonEnchantment;
import dev.drawethree.xprison.enchants.XPrisonEnchants;
import dev.drawethree.xprison.enchants.gui.ChoosePickaxeSlotGui;
import dev.drawethree.xprison.enchants.gui.DisenchantGUI;
import dev.drawethree.xprison.enchants.gui.EnchantGUI;
import dev.drawethree.xprison.enchants.gui.EnchantUpgradeGui;
import dev.drawethree.xprison.enchants.gui.PickaxeSettingsGui;
import dev.drawethree.xprison.enchants.utils.EnchantUtils;
import dev.drawethree.xprison.enchants.utils.GuiUtils;
import dev.drawethree.xprison.pickaxequality.XPrisonPickaxeQuality;
import dev.drawethree.xprison.pickaxequality.gui.PickaxeQualityGui;
import dev.drawethree.xprison.pickaxeskins.XPrisonPickaxeSkins;
import dev.drawethree.xprison.pickaxeskins.gui.PickaxeSkinsGui;
import dev.drawethree.xprison.utils.gui.XPrisonGui;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.NumberUtil;
import me.nakilex.levelplugin.utils.TextUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.widgets.ActionWidget;
import me.nakilex.levelplugin.utils.gui.widgets.GuiContext;
import me.nakilex.levelplugin.utils.gui.widgets.GuiLayout;
import me.nakilex.levelplugin.utils.gui.widgets.GuiWidget;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Our own Pickaxe Menu, replacing X-Prison's {@code EnchantGUI} so the enchant list, tabs and
 * buttons follow the same layout and tooltip conventions as the rest of the plugin.
 *
 * <p>X-Prison still owns everything behind the buttons: this menu only reads enchant data through
 * its API and opens X-Prison's own sub-menus (upgrade, refund, skins, quality, settings, slot).
 * Those sub-menus have no hook for where "back" goes - they construct a fresh {@code EnchantGUI}
 * - so every attempt to open X-Prison's menu is cancelled here and this one opened in its place,
 * picking up the pickaxe slot and currency tab X-Prison was about to show.</p>
 */
public final class PickaxeMenuGUI implements Listener {

    private static final String TITLE = TextUtil.centerInventoryTitle("Pickaxe Menu");
    private static final int SIZE = 54;
    /** Three centred rows starting on row 3, leaving an empty row under the header buttons. */
    private static final int[] ENCHANT_SLOTS = {
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };
    private static final int PAGE_SIZE = ENCHANT_SLOTS.length;

    private static final int SKINS_SLOT = 1;
    private static final int QUALITY_SLOT = 2;
    private static final int REFUND_SLOT = 3;
    private static final int PICKAXE_SLOT = 4;
    private static final int SETTINGS_SLOT = 5;
    private static final int HOTBAR_SLOT_SLOT = 6;
    private static final int INFO_SLOT = 8;
    private static final int EMPTY_SLOT = 31;
    private static final int PREV_SLOT = 45;
    private static final int TAB_CENTER_SLOT = 49;
    private static final int SORT_SLOT = 51;
    private static final int FILTER_SLOT = 52;
    private static final int NEXT_SLOT = 53;
    /** X-Prison's refund menu has no back button of its own; ours goes bottom-centre, between its tabs. */
    private static final int REFUND_BACK_SLOT = 49;

    private static final String SETTINGS_PERMISSION = "xprison.enchants.settings";
    private static final String HOTBAR_SLOT_PERMISSION = "xprison.enchants.pickaxeslot";
    private static final int LORE_WRAP_PX = 170;

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final Main plugin;
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final Map<UUID, String> currencyByPlayer = new HashMap<>();
    private final Map<UUID, SortMode> sortByPlayer = new HashMap<>();
    private final Map<UUID, FilterMode> filterByPlayer = new HashMap<>();
    private final Map<UUID, Integer> pageByPlayer = new HashMap<>();

    private String xprisonMenuTitle = "Pickaxe Menu";
    private String refundMenuTitle = "Enchant Refund";
    private boolean skinsEnabled = true;
    private boolean qualityEnabled = true;
    private boolean refundEnabled = true;
    private boolean settingsEnabled = true;
    private boolean hotbarSlotEnabled = true;

    public PickaxeMenuGUI(Main plugin) {
        this.plugin = plugin;
        loadXPrisonMenuConfig();
    }

    /** Whether an inventory is one of ours, so X-Prison menu listeners can leave it alone. */
    public static boolean isPickaxeMenu(Inventory inventory) {
        return inventory != null && inventory.getHolder() instanceof Holder;
    }

    /**
     * Mirrors the enable toggles admins set for X-Prison's own menu buttons, so switching a feature
     * off in {@code enchants.yml} still hides it here.
     */
    private void loadXPrisonMenuConfig() {
        try {
            File file = new File(plugin.getServer().getPluginsFolder(), "X-Prison/enchants.yml");
            if (!file.isFile()) {
                return;
            }
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            String refundTitle = yaml.getString("disenchant_menu.title");
            if (refundTitle != null && !refundTitle.isBlank()) {
                refundMenuTitle = ChatColor.stripColor(toLegacy(refundTitle)).trim();
            }
            ConfigurationSection menu = yaml.getConfigurationSection("enchant_menu");
            if (menu == null) {
                return;
            }
            String title = menu.getString("title");
            if (title != null && !title.isBlank()) {
                xprisonMenuTitle = ChatColor.stripColor(toLegacy(title));
            }
            skinsEnabled = menu.getBoolean("skins_item.enabled", true);
            qualityEnabled = menu.getBoolean("quality_item.enabled", true);
            refundEnabled = menu.getBoolean("disenchant_item.enabled", true);
            settingsEnabled = menu.getBoolean("settings_item.enabled", true);
            hotbarSlotEnabled = menu.getBoolean("pickaxe_slot_item.enabled", true);
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("[PickaxeMenu] Could not read X-Prison enchants.yml: " + ex.getMessage());
        }
    }

    // ----------------------------------------------------------------------------------------
    // Opening
    // ----------------------------------------------------------------------------------------

    /** Open the menu for the pickaxe in the given player inventory slot. */
    public void open(Player player, int pickaxeSlot) {
        ItemStack pickaxe = pickaxeAt(player, pickaxeSlot);
        if (pickaxe == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Hold your prison pickaxe to open the Pickaxe Menu.");
            return;
        }
        Inventory inventory = Bukkit.createInventory(new Holder(), SIZE, TITLE);
        Session session = new Session(pickaxeSlot, inventory);
        sessions.put(player.getUniqueId(), session);
        render(player, session);
        player.openInventory(inventory);
    }

    /**
     * X-Prison tried to open its own Pickaxe Menu - from a pickaxe right-click, /enchantmenu or a
     * sub-menu's back button. Its open is cancelled and ours opened a tick later, once X-Prison has
     * recorded which pickaxe and tab it meant; our open then invalidates X-Prison's menu for it.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onXPrisonMenuOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)
                || isPickaxeMenu(event.getInventory())
                || !GuiUtil.titleMatches(event.getView().getTitle(), xprisonMenuTitle)) {
            return;
        }
        event.setCancelled(true);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            XPrisonGui attempted = me.lucko.helper.metadata.Metadata.provideForPlayer(player)
                    .get(XPrisonGui.OPEN_GUI_KEY).orElse(null);
            int slot = -1;
            if (attempted instanceof EnchantGUI enchantGui) {
                slot = enchantGui.getPickaxePlayerInventorySlot();
                if (enchantGui.getCurrency() != null) {
                    currencyByPlayer.put(player.getUniqueId(), enchantGui.getCurrency().getName());
                }
            }
            if (pickaxeAt(player, slot) == null) {
                slot = findPickaxeSlot(player);
            }
            open(player, slot);
        });
    }

    // ----------------------------------------------------------------------------------------
    // Refund menu back button
    // ----------------------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRefundMenuOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player && isRefundMenu(event.getView().getTitle())) {
            placeRefundBackNextTick(player);
        }
    }

    /**
     * Slot 49 is filler to X-Prison, so a click there is ours. Any other click can make X-Prison
     * redraw the menu (paging, tabs, a refund), which wipes the button - so it is put back after.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRefundMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !isRefundMenu(event.getView().getTitle())) {
            return;
        }
        if (event.getRawSlot() != REFUND_BACK_SLOT) {
            placeRefundBackNextTick(player);
            return;
        }
        event.setCancelled(true);
        XPrisonGui refund = me.lucko.helper.metadata.Metadata.provideForPlayer(player)
                .get(XPrisonGui.OPEN_GUI_KEY).orElse(null);
        int slot = refund instanceof DisenchantGUI disenchant ? disenchant.getPickaxePlayerInventorySlot() : -1;
        int pickaxeSlot = pickaxeAt(player, slot) != null ? slot : findPickaxeSlot(player);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                open(player, pickaxeSlot);
            }
        });
    }

    private void placeRefundBackNextTick(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline() || !isRefundMenu(player.getOpenInventory().getTitle())) {
                return;
            }
            Inventory top = player.getOpenInventory().getTopInventory();
            if (top.getSize() > REFUND_BACK_SLOT) {
                top.setItem(REFUND_BACK_SLOT, GuiUtil.getNexoItem("arrow_left", ChatColor.YELLOW + "Back",
                        List.of(TooltipUtil.leftClickLine("to return to the Pickaxe Menu"))));
            }
        });
    }

    private boolean isRefundMenu(String title) {
        return GuiUtil.titleMatches(title, refundMenuTitle);
    }

    // ----------------------------------------------------------------------------------------
    // Rendering
    // ----------------------------------------------------------------------------------------

    private void render(Player player, Session session) {
        Inventory inventory = session.inventory;
        inventory.clear();
        ItemStack filler = GuiUtil.createFiller(Material.GRAY_STAINED_GLASS_PANE);
        GuiUtil.fillBorder(inventory, filler);

        List<GuiWidget> widgets = buildWidgets(player, session);
        session.widgets = widgets;
        GuiLayout layout = new GuiLayout(inventory);
        GuiContext context = new GuiContext(player, inventory);
        for (GuiWidget widget : widgets) {
            widget.contribute(layout, context);
        }
    }

    private List<GuiWidget> buildWidgets(Player player, Session session) {
        UUID id = player.getUniqueId();
        ItemStack pickaxe = pickaxeAt(player, session.pickaxeSlot);
        List<GuiWidget> widgets = new ArrayList<>();
        if (pickaxe == null) {
            return widgets;
        }

        List<XPrisonEnchantment> purchasable = purchasableEnchants();
        List<String> currencies = currencyTabs(purchasable);
        String currency = currencyByPlayer.get(id);
        if (currency == null || currencies.stream().noneMatch(currency::equalsIgnoreCase)) {
            currency = currencies.isEmpty() ? null : currencies.getFirst();
            currencyByPlayer.put(id, currency);
        }

        List<XPrisonEnchantment> shown = sortedAndFiltered(player, pickaxe, purchasable, currency);
        int maxPage = Math.max(0, (shown.size() - 1) / PAGE_SIZE);
        int page = Math.max(0, Math.min(pageByPlayer.getOrDefault(id, 0), maxPage));
        pageByPlayer.put(id, page);

        int start = page * PAGE_SIZE;
        int end = Math.min(shown.size(), start + PAGE_SIZE);
        for (int i = start; i < end; i++) {
            XPrisonEnchantment enchant = shown.get(i);
            widgets.add(new ActionWidget(ENCHANT_SLOTS[i - start],
                    context -> enchantIcon(player, pickaxe, enchant),
                    (click, context) -> openUpgradeMenu(player, session, enchant)));
        }
        if (shown.isEmpty()) {
            widgets.add(new ActionWidget(EMPTY_SLOT, context -> emptyItem(), null));
        }

        widgets.add(new ActionWidget(PICKAXE_SLOT, context -> pickaxeSummary(pickaxe, purchasable), null));
        widgets.add(new ActionWidget(INFO_SLOT, context -> infoItem(), null));
        if (skinsEnabled && skinsModule() != null) {
            widgets.add(new ActionWidget(SKINS_SLOT, context -> skinsItem(),
                    (click, context) -> openSkins(player, session)));
        }
        if (qualityEnabled && qualityModule() != null) {
            widgets.add(new ActionWidget(QUALITY_SLOT, context -> qualityItem(pickaxe),
                    (click, context) -> openQuality(player, session)));
        }
        if (refundEnabled) {
            widgets.add(new ActionWidget(REFUND_SLOT, context -> refundItem(),
                    (click, context) -> openRefund(player, session)));
        }
        if (settingsEnabled && GuiUtils.hasFeaturePermission(player, SETTINGS_PERMISSION)) {
            widgets.add(new ActionWidget(SETTINGS_SLOT, context -> settingsItem(),
                    (click, context) -> openSettings(player, session)));
        }
        if (hotbarSlotEnabled && GuiUtils.hasFeaturePermission(player, HOTBAR_SLOT_PERMISSION)) {
            widgets.add(new ActionWidget(HOTBAR_SLOT_SLOT, context -> hotbarSlotItem(session),
                    (click, context) -> openHotbarSlot(player, session)));
        }

        int tabStart = TAB_CENTER_SLOT - currencies.size() / 2;
        for (int i = 0; i < currencies.size(); i++) {
            String tab = currencies.get(i);
            boolean selected = tab.equalsIgnoreCase(currency);
            long count = purchasable.stream().filter(e -> tab.equalsIgnoreCase(e.getCurrencyName())).count();
            widgets.add(new ActionWidget(tabStart + i, context -> currencyTab(player, tab, selected, count),
                    (click, context) -> {
                        if (!selected) {
                            currencyByPlayer.put(id, tab);
                            pageByPlayer.put(id, 0);
                            render(player, session);
                        }
                    }));
        }

        if (page > 0) {
            widgets.add(new ActionWidget(PREV_SLOT, context -> navItem(false, page, maxPage),
                    (click, context) -> changePage(player, session, page - 1)));
        }
        if (page < maxPage) {
            widgets.add(new ActionWidget(NEXT_SLOT, context -> navItem(true, page, maxPage),
                    (click, context) -> changePage(player, session, page + 1)));
        }
        widgets.add(new ActionWidget(SORT_SLOT, context -> sortButton(id), (click, context) -> {
            sortByPlayer.put(id, cycle(SortMode.values(), sortByPlayer.getOrDefault(id, SortMode.MENU), click.isLeftClick()));
            pageByPlayer.put(id, 0);
            render(player, session);
        }));
        widgets.add(new ActionWidget(FILTER_SLOT, context -> filterButton(id), (click, context) -> {
            filterByPlayer.put(id, cycle(FilterMode.values(), filterByPlayer.getOrDefault(id, FilterMode.ALL), click.isLeftClick()));
            pageByPlayer.put(id, 0);
            render(player, session);
        }));
        return widgets;
    }

    private void changePage(Player player, Session session, int page) {
        pageByPlayer.put(player.getUniqueId(), page);
        render(player, session);
    }

    // ----------------------------------------------------------------------------------------
    // Enchant data
    // ----------------------------------------------------------------------------------------

    /** Every enchant a player can buy: enabled, and not one of the free baked-in defaults. */
    private List<XPrisonEnchantment> purchasableEnchants() {
        List<XPrisonEnchantment> result = new ArrayList<>();
        for (XPrisonEnchantment enchant : XPrisonAPI.getInstance().getEnchantsApi().getAllEnchantments()) {
            if (enchant.isEnabled() && !XPrisonPickaxeDefaultsListener.HIDDEN_DEFAULT_ENCHANTS
                    .contains(enchant.getRawName().toLowerCase(Locale.ROOT))) {
                result.add(enchant);
            }
        }
        return result;
    }

    /** One tab per currency that has enchants, tokens and gems first. */
    private List<String> currencyTabs(List<XPrisonEnchantment> enchants) {
        List<String> tabs = new ArrayList<>();
        for (XPrisonEnchantment enchant : enchants) {
            String name = enchant.getCurrencyName().toLowerCase(Locale.ROOT);
            if (!tabs.contains(name)) {
                tabs.add(name);
            }
        }
        tabs.sort(Comparator.comparingInt(PickaxeMenuGUI::currencyOrder).thenComparing(Comparator.naturalOrder()));
        return tabs;
    }

    private static int currencyOrder(String currency) {
        return switch (currency) {
            case "tokens" -> 0;
            case "gems" -> 1;
            case "money" -> 2;
            default -> 3;
        };
    }

    private List<XPrisonEnchantment> sortedAndFiltered(Player player, ItemStack pickaxe,
                                                       List<XPrisonEnchantment> enchants, String currency) {
        FilterMode filter = filterByPlayer.getOrDefault(player.getUniqueId(), FilterMode.ALL);
        SortMode sort = sortByPlayer.getOrDefault(player.getUniqueId(), SortMode.MENU);
        List<XPrisonEnchantment> result = new ArrayList<>();
        for (XPrisonEnchantment enchant : enchants) {
            if (currency != null && !currency.equalsIgnoreCase(enchant.getCurrencyName())) {
                continue;
            }
            EnchantState state = EnchantState.of(player, pickaxe, enchant);
            boolean keep = switch (filter) {
                case ALL -> true;
                case AFFORDABLE -> state.unlocked && !state.maxed && state.affordable;
                case NOT_MAXED -> !state.maxed;
                case OWNED -> state.level > 0;
            };
            if (keep) {
                result.add(enchant);
            }
        }
        Comparator<XPrisonEnchantment> menuOrder = Comparator
                .comparingInt((XPrisonEnchantment e) -> e.getGuiProperties().getGuiPage())
                .thenComparingInt(e -> e.getGuiProperties().getGuiSlot());
        Comparator<XPrisonEnchantment> comparator = switch (sort) {
            case MENU -> menuOrder;
            case UNLOCK -> Comparator.comparingInt(PickaxeMenuGUI::requiredPickaxeLevel).thenComparing(menuOrder);
            case LEVEL -> Comparator.comparingInt((XPrisonEnchantment e) -> -enchantLevel(pickaxe, e)).thenComparing(menuOrder);
            case COST -> Comparator.comparing((XPrisonEnchantment e) -> e.getCostAtLevelExact(enchantLevel(pickaxe, e)))
                    .thenComparing(menuOrder);
        };
        result.sort(comparator);
        return result;
    }

    private static int requiredPickaxeLevel(XPrisonEnchantment enchant) {
        return enchant instanceof RequiresPickaxeLevel gated ? gated.getRequiredPickaxeLevel() : 0;
    }

    private static int enchantLevel(ItemStack pickaxe, XPrisonEnchantment enchant) {
        return XPrisonAPI.getInstance().getEnchantsApi().getEnchantLevel(pickaxe, enchant);
    }

    /** Snapshot of one enchant on one pickaxe, computed the same way X-Prison's own lore does. */
    private record EnchantState(int level, boolean unlocked, boolean maxed, boolean affordable,
                                BigDecimal cost, BigDecimal balance) {
        static EnchantState of(Player player, ItemStack pickaxe, XPrisonEnchantment enchant) {
            int level = enchantLevel(pickaxe, enchant);
            boolean unlocked = EnchantUtils.canBeBought(enchant, pickaxe);
            boolean maxed = level >= enchant.getMaxLevel();
            BigDecimal cost = enchant.getCostAtLevelExact(level);
            BigDecimal balance = XPrisonAPI.getInstance().getCurrencyApi()
                    .getBalanceExact(player, enchant.getCurrencyName().toLowerCase(Locale.ROOT));
            return new EnchantState(level, unlocked, maxed, balance.compareTo(cost) >= 0, cost, balance);
        }
    }

    // ----------------------------------------------------------------------------------------
    // Items
    // ----------------------------------------------------------------------------------------

    private ItemStack enchantIcon(Player player, ItemStack pickaxe, XPrisonEnchantment enchant) {
        EnchantState state = EnchantState.of(player, pickaxe, enchant);
        if (!state.unlocked) {
            return lockedEnchantIcon(enchant);
        }
        ChatColor accent = currencyColor(enchant.getCurrencyName());
        XPrisonCurrency currency = enchant.getCurrency();

        List<String> lore = new ArrayList<>();
        for (String line : enchant.getGuiProperties().getGuiDescription()) {
            lore.addAll(TooltipUtil.wrapLoreLine(ChatColor.GRAY + ChatColor.stripColor(toLegacy(line)), LORE_WRAP_PX));
        }
        lore.add(" ");
        lore.add(TooltipUtil.labelValueLine("Level", accent,
                NumberUtil.formatCommas(state.level) + ChatColor.GRAY + "/" + accent + NumberUtil.formatCommas(enchant.getMaxLevel())));
        if (enchant instanceof ChanceBasedEnchant chanced) {
            String now = formatChance(chanced.getChanceToTrigger(state.level));
            lore.add(state.maxed
                    ? TooltipUtil.labelValueLine("Chance", accent, now)
                    : TooltipUtil.labelValueLine("Chance", accent, now + ChatColor.DARK_GRAY + " → "
                    + ChatColor.GREEN + formatChance(chanced.getChanceToTrigger(state.level + 1))));
        }
        if (!state.maxed) {
            lore.add(TooltipUtil.labelValueLine("Upgrade Cost", accent,
                    currency.format(state.cost) + " " + currencyName(currency)));
        }
        appendPrestige(lore, pickaxe, enchant, state, accent);

        lore.add(" ");
        if (state.maxed) {
            lore.add(ChatColor.GREEN + "Max level reached");
        } else if (!state.affordable) {
            lore.add(ChatColor.RED + "Not enough " + currencyName(currency));
        }
        lore.add(TooltipUtil.leftClickLine("to open upgrades"));

        ItemStack icon = GuiUtils.iconBaseItem(enchant.getGuiProperties());
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.WHITE + toLegacy(enchant.getGuiProperties().getGuiName()));
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            meta.setEnchantmentGlintOverride(isFullyMaxed(pickaxe, enchant, state) ? Boolean.TRUE : null);
            icon.setItemMeta(meta);
        }
        return icon;
    }

    private void appendPrestige(List<String> lore, ItemStack pickaxe, XPrisonEnchantment enchant,
                                EnchantState state, ChatColor accent) {
        if (!(enchant instanceof PrestigeableEnchant prestigeable) || !prestigeable.isPrestigeEnabled()) {
            return;
        }
        var enchantsApi = XPrisonAPI.getInstance().getEnchantsApi();
        int prestige = enchantsApi.getEnchantPrestige(pickaxe, enchant);
        int maxPrestige = prestigeable.getMaxPrestige();
        lore.add(" ");
        lore.add(TooltipUtil.sectionHeader("Prestige"));
        lore.add(TooltipUtil.labelValueLine("Prestige", accent, prestige + ChatColor.GRAY.toString() + "/" + accent + maxPrestige));
        lore.add(TooltipUtil.labelValueLine("Bonus", ChatColor.GREEN,
                "+" + formatPercent(prestige * prestigeable.getMultiplierPerPrestige() * 100.0)));
        if (prestige >= maxPrestige) {
            lore.add(ChatColor.GREEN + "Max prestige reached");
            return;
        }
        long activations = enchantsApi.getAmountOfActivations(pickaxe, enchant);
        long required = Math.max(1L, prestigeable.getRequiredActivations(prestige));
        lore.add(TooltipUtil.expProgressBarByPixels(Math.min(activations, required), required, 156) + " "
                + ChatColor.GRAY + NumberUtil.formatCommas(Math.min(activations, required))
                + ChatColor.GOLD + "/" + ChatColor.GRAY + NumberUtil.formatCommas(required) + " activations");
        if (EnchantUtils.isPrestigeReady(enchant, state.level, prestige, activations)) {
            lore.add(ChatColor.GREEN + "Prestige ready! Claim it in the upgrade menu.");
        } else if (!state.maxed) {
            lore.add(ChatColor.DARK_GRAY + "Max this enchant to prestige it.");
        }
    }

    /** Matches X-Prison's glint rule: at max level, and at max prestige for prestigeable enchants. */
    private static boolean isFullyMaxed(ItemStack pickaxe, XPrisonEnchantment enchant, EnchantState state) {
        if (!state.maxed) {
            return false;
        }
        if (enchant instanceof PrestigeableEnchant prestigeable && prestigeable.isPrestigeEnabled()) {
            return XPrisonAPI.getInstance().getEnchantsApi().getEnchantPrestige(pickaxe, enchant) >= prestigeable.getMaxPrestige();
        }
        return true;
    }

    /** X-Prison hides a locked enchant's identity until the pickaxe is strong enough; so do we. */
    private ItemStack lockedEnchantIcon(XPrisonEnchantment enchant) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "A mysterious enchant, hidden");
        lore.add(ChatColor.GRAY + "until your pickaxe is strong enough.");
        lore.add(" ");
        lore.add(TooltipUtil.labelValueLine("Unlocks at Pickaxe Level", ChatColor.YELLOW,
                NumberUtil.formatCommas(requiredPickaxeLevel(enchant))));
        return GuiUtil.getNexoItem("lock", ChatColor.RED + "Locked Enchant", lore);
    }

    private ItemStack pickaxeSummary(ItemStack pickaxe, List<XPrisonEnchantment> purchasable) {
        ItemStack display = pickaxe.clone();
        ItemMeta meta = display.getItemMeta();
        if (meta == null) {
            return display;
        }
        var levelsApi = XPrisonAPI.getInstance().getPickaxeLevelsApi();
        List<String> lore = new ArrayList<>();
        levelsApi.getPickaxeLevel(pickaxe).ifPresent(level -> {
            lore.add(TooltipUtil.labelValueLine("Pickaxe Level", ChatColor.YELLOW, NumberUtil.formatCommas(level.getLevel())));
            long exp = levelsApi.getPickaxeExp(pickaxe);
            var next = levelsApi.getPickaxeLevel(level.getLevel() + 1);
            if (next.isPresent()) {
                long from = level.getExpRequired();
                long to = next.get().getExpRequired();
                lore.add(TooltipUtil.expProgressBarByPixels(Math.max(0, exp - from), Math.max(1, to - from), 156)
                        + " " + ChatColor.GRAY + NumberUtil.formatCommas(exp)
                        + ChatColor.GOLD + "/" + ChatColor.GRAY + NumberUtil.formatCommas(to) + " XP");
            } else {
                lore.add(TooltipUtil.expProgressBarByPixels(1, 1, 156) + " " + ChatColor.GRAY + "Max");
            }
        });
        String quality = qualityLabel(pickaxe);
        if (quality != null) {
            lore.add(TooltipUtil.labelValueLine("Quality", ChatColor.WHITE, quality));
        }
        long owned = purchasable.stream().filter(e -> enchantLevel(pickaxe, e) > 0).count();
        lore.add(TooltipUtil.labelValueLine("Enchants", ChatColor.WHITE,
                owned + ChatColor.GRAY.toString() + "/" + ChatColor.WHITE + purchasable.size()));
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        display.setItemMeta(meta);
        return display;
    }

    private ItemStack infoItem() {
        List<String> lore = new ArrayList<>();
        lore.addAll(TooltipUtil.bulletList(
                "Click an enchant to buy levels",
                "Buy 1, 5, 25, 50 or MAX levels at once",
                "Toggle an enchant on or off in its upgrade menu",
                "Maxed enchants can prestige for a lasting bonus",
                "Level your pickaxe to reveal locked enchants"));
        return GuiUtil.getNexoItem("info", ChatColor.AQUA + "How It Works", lore);
    }

    private ItemStack skinsItem() {
        List<String> lore = new ArrayList<>(TooltipUtil.bulletList("Skins give various boosts to", "help with your progression"));
        lore.add(" ");
        lore.add(TooltipUtil.leftClickLine("to browse skins"));
        return GuiUtil.createGuiItem(Material.AMETHYST_SHARD, ChatColor.LIGHT_PURPLE + "Pickaxe Skins", lore);
    }

    private ItemStack qualityItem(ItemStack pickaxe) {
        List<String> lore = new ArrayList<>(TooltipUtil.bulletList("Upgrade your quality tier for", "permanent mining boosts"));
        String quality = qualityLabel(pickaxe);
        if (quality != null) {
            lore.add(" ");
            lore.add(TooltipUtil.labelValueLine("Current", ChatColor.WHITE, quality));
        }
        lore.add(" ");
        lore.add(TooltipUtil.leftClickLine("to open Pickaxe Quality"));
        return GuiUtil.createGuiItem(Material.NETHER_STAR, ChatColor.AQUA + "Pickaxe Quality", lore);
    }

    private ItemStack refundItem() {
        List<String> lore = new ArrayList<>(TooltipUtil.bulletList("Remove enchant levels for a", "partial refund of their cost"));
        lore.add(" ");
        lore.add(TooltipUtil.leftClickLine("to open refunds"));
        return GuiUtil.createGuiItem(Material.ANVIL, ChatColor.RED + "Enchant Refund", lore);
    }

    private ItemStack settingsItem() {
        List<String> lore = new ArrayList<>(TooltipUtil.bulletList("Adjust how your pickaxe behaves"));
        lore.add(" ");
        lore.add(TooltipUtil.leftClickLine("to open settings"));
        return GuiUtil.getNexoItem("settings", ChatColor.AQUA + "Pickaxe Settings", lore);
    }

    private ItemStack hotbarSlotItem(Session session) {
        List<String> lore = new ArrayList<>(TooltipUtil.bulletList("Choose which hotbar slot", "your pickaxe sits in"));
        if (session.pickaxeSlot >= 0 && session.pickaxeSlot < 9) {
            lore.add(" ");
            lore.add(TooltipUtil.labelValueLine("Current Slot", ChatColor.WHITE, String.valueOf(session.pickaxeSlot + 1)));
        }
        lore.add(" ");
        lore.add(TooltipUtil.leftClickLine("to choose a slot"));
        return GuiUtil.createGuiItem(Material.ITEM_FRAME, ChatColor.AQUA + "Pickaxe Slot", lore);
    }

    private ItemStack currencyTab(Player player, String currencyId, boolean selected, long enchantCount) {
        XPrisonCurrency currency = XPrisonAPI.getInstance().getCurrencyApi().getCurrency(currencyId);
        String label = currency == null ? capitalize(currencyId) : currencyName(currency);
        ChatColor color = currencyColor(currencyId);
        List<String> lore = new ArrayList<>();
        lore.add(TooltipUtil.labelValueLine("Enchants", ChatColor.WHITE, String.valueOf(enchantCount)));
        if (currency != null) {
            lore.add(TooltipUtil.labelValueLine("Balance", color,
                    currency.format(XPrisonAPI.getInstance().getCurrencyApi().getBalanceExact(player, currencyId))));
        }
        lore.add(" ");
        lore.add(selected ? ChatColor.GREEN + "Currently viewing" : TooltipUtil.leftClickLine("to view " + label.toLowerCase(Locale.ROOT) + " enchants"));

        String name = color + singular(label) + " Enchants";
        ItemStack tab = GuiUtil.getNexoItemIfPresent("levelplugin_" + currencyId, name, lore);
        if (tab == null) {
            tab = GuiUtil.createGuiItem(switch (currencyId) {
                case "tokens" -> Material.SUNFLOWER;
                case "gems" -> Material.EMERALD;
                default -> Material.GOLD_INGOT;
            }, name, lore);
        }
        ItemMeta meta = tab.getItemMeta();
        if (meta != null) {
            meta.setEnchantmentGlintOverride(selected ? Boolean.TRUE : null);
            tab.setItemMeta(meta);
        }
        return tab;
    }

    private ItemStack navItem(boolean next, int page, int maxPage) {
        List<String> lore = new ArrayList<>();
        lore.add(TooltipUtil.labelValueLine("Page", ChatColor.WHITE, (page + 1) + ChatColor.GRAY.toString() + "/" + ChatColor.WHITE + (maxPage + 1)));
        lore.add(" ");
        lore.addAll(TooltipUtil.clickInstructions("to change page", null));
        return GuiUtil.getNexoItem(next ? "arrow_right" : "arrow_left",
                ChatColor.GREEN + (next ? "Next Page" : "Previous Page"), lore);
    }

    private ItemStack sortButton(UUID id) {
        SortMode mode = sortByPlayer.getOrDefault(id, SortMode.MENU);
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        for (SortMode value : SortMode.values()) {
            lore.add(TooltipUtil.selectionLine(value == mode, value.label));
        }
        lore.add(" ");
        lore.addAll(TooltipUtil.clickInstructions("to cycle forward", "to cycle backward"));
        return GuiUtil.createGuiItem(Material.COMPARATOR, ChatColor.AQUA + "Sort", lore);
    }

    private ItemStack filterButton(UUID id) {
        FilterMode mode = filterByPlayer.getOrDefault(id, FilterMode.ALL);
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        for (FilterMode value : FilterMode.values()) {
            lore.add(TooltipUtil.selectionLine(value == mode, value.label));
        }
        lore.add(" ");
        lore.addAll(TooltipUtil.clickInstructions("to cycle forward", "to cycle backward"));
        return GuiUtil.createGuiItem(Material.HOPPER, ChatColor.AQUA + "Filter", lore);
    }

    private ItemStack emptyItem() {
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.addAll(TooltipUtil.bulletList("No enchants match this filter.", "Try another filter or currency."));
        return GuiUtil.createGuiItem(Material.BARRIER, ChatColor.RED + "No Enchants Found", lore);
    }

    // ----------------------------------------------------------------------------------------
    // X-Prison sub-menus
    // ----------------------------------------------------------------------------------------

    private void openUpgradeMenu(Player player, Session session, XPrisonEnchantment enchant) {
        ItemStack pickaxe = pickaxeAt(player, session.pickaxeSlot);
        if (pickaxe == null) {
            player.closeInventory();
            return;
        }
        if (!EnchantUtils.canBeBought(enchant, pickaxe)) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Reach Pickaxe Level "
                    + NumberUtil.formatCommas(requiredPickaxeLevel(enchant)) + " to unlock this enchant.");
            return;
        }
        openXPrison(player, "upgrade", () -> new EnchantUpgradeGui(XPrisonEnchants.getInstance(), player, pickaxe,
                session.pickaxeSlot, enchant, enchant.getCurrency(), 1));
    }

    private void openRefund(Player player, Session session) {
        openXPrison(player, "refund", () -> new DisenchantGUI(XPrisonEnchants.getInstance(), player,
                pickaxeAt(player, session.pickaxeSlot), session.pickaxeSlot, selectedCurrency(player), 1));
    }

    private void openSettings(Player player, Session session) {
        openXPrison(player, "settings", () -> new PickaxeSettingsGui(XPrisonEnchants.getInstance(), player,
                pickaxeAt(player, session.pickaxeSlot), session.pickaxeSlot, selectedCurrency(player), 1));
    }

    private void openHotbarSlot(Player player, Session session) {
        openXPrison(player, "pickaxe slot", () -> new ChoosePickaxeSlotGui(XPrisonEnchants.getInstance(), player,
                pickaxeAt(player, session.pickaxeSlot), session.pickaxeSlot, selectedCurrency(player), 1));
    }

    private void openSkins(Player player, Session session) {
        openXPrison(player, "skins", () -> {
            PickaxeSkinsGui gui = new PickaxeSkinsGui(skinsModule(), player, pickaxeAt(player, session.pickaxeSlot));
            gui.setFallbackGui(backToMenu(session.pickaxeSlot));
            return gui;
        });
    }

    private void openQuality(Player player, Session session) {
        openXPrison(player, "quality", () -> new PickaxeQualityGui(qualityModule(), player,
                pickaxeAt(player, session.pickaxeSlot), session.pickaxeSlot, backToMenu(session.pickaxeSlot)));
    }

    /**
     * X-Prison's "back" only knows how to build its own menu; handing it that menu is enough,
     * because {@link #onXPrisonMenuOpen} swaps it for ours the moment it opens.
     */
    private Function<Player, XPrisonGui> backToMenu(int pickaxeSlot) {
        return viewer -> new EnchantGUI(XPrisonEnchants.getInstance(), viewer, pickaxeAt(viewer, pickaxeSlot),
                pickaxeSlot, selectedCurrency(viewer), 1);
    }

    private void openXPrison(Player player, String menu, java.util.function.Supplier<XPrisonGui> factory) {
        try {
            factory.get().open();
        } catch (RuntimeException | LinkageError ex) {
            plugin.getLogger().warning("[PickaxeMenu] Could not open X-Prison " + menu + " menu: " + ex);
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "That menu is unavailable right now.");
        }
    }

    private XPrisonCurrency selectedCurrency(Player player) {
        String id = currencyByPlayer.get(player.getUniqueId());
        return id == null ? null : XPrisonAPI.getInstance().getCurrencyApi().getCurrency(id);
    }

    private static XPrisonPickaxeSkins skinsModule() {
        return XPrison.getInstance().getEnabledModule(XPrisonPickaxeSkins.class);
    }

    private static XPrisonPickaxeQuality qualityModule() {
        return XPrison.getInstance().getEnabledModule(XPrisonPickaxeQuality.class);
    }

    private String qualityLabel(ItemStack pickaxe) {
        XPrisonPickaxeQuality module = qualityEnabled ? qualityModule() : null;
        if (module == null) {
            return null;
        }
        try {
            var manager = module.getPickaxeQualityManager();
            var tier = manager.getTier(pickaxe);
            return toLegacy(tier.getDisplayName()) + ChatColor.DARK_GRAY + " (" + tier.getTier() + "/" + manager.getMaxTierNumber() + ")";
        } catch (RuntimeException ex) {
            return null;
        }
    }

    // ----------------------------------------------------------------------------------------
    // Pickaxe lookup
    // ----------------------------------------------------------------------------------------

    /** The X-Prison pickaxe in that inventory slot, or {@code null} if it is not one any more. */
    private static ItemStack pickaxeAt(Player player, int slot) {
        if (slot < 0 || slot >= player.getInventory().getSize()) {
            return null;
        }
        ItemStack item = player.getInventory().getItem(slot);
        return isPrisonPickaxe(item) ? item : null;
    }

    /** Held pickaxe first, then the first one found in the inventory. */
    private static int findPickaxeSlot(Player player) {
        int held = player.getInventory().getHeldItemSlot();
        if (pickaxeAt(player, held) != null) {
            return held;
        }
        ItemStack[] storage = player.getInventory().getStorageContents();
        for (int slot = 0; slot < storage.length; slot++) {
            if (isPrisonPickaxe(storage[slot])) {
                return slot;
            }
        }
        return -1;
    }

    private static boolean isPrisonPickaxe(ItemStack item) {
        return item != null && !item.getType().isAir()
                && XPrison.getInstance().isPickaxeSupported(item)
                && XPrisonAPI.getInstance().getPickaxeLevelsApi().getPickaxeLevel(item).isPresent();
    }

    // ----------------------------------------------------------------------------------------
    // Events
    // ----------------------------------------------------------------------------------------

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!isPickaxeMenu(event.getView().getTopInventory()) || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        event.setCancelled(true);
        Session session = sessions.get(player.getUniqueId());
        int slot = event.getRawSlot();
        if (session == null || session.widgets == null || slot < 0 || slot >= SIZE) {
            return;
        }
        GuiContext context = new GuiContext(player, event.getView().getTopInventory());
        for (GuiWidget widget : session.widgets) {
            if (widget.handlesSlot(slot)) {
                widget.onClick(slot, event.getClick(), context);
                return;
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (isPickaxeMenu(event.getView().getTopInventory())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (isPickaxeMenu(event.getInventory())) {
            Session session = sessions.get(event.getPlayer().getUniqueId());
            if (session != null && session.inventory.equals(event.getInventory())) {
                sessions.remove(event.getPlayer().getUniqueId());
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        sessions.remove(id);
        currencyByPlayer.remove(id);
        sortByPlayer.remove(id);
        filterByPlayer.remove(id);
        pageByPlayer.remove(id);
    }

    // ----------------------------------------------------------------------------------------
    // Formatting
    // ----------------------------------------------------------------------------------------

    /** X-Prison text is MiniMessage; our tooltips are legacy section-sign strings. */
    private static String toLegacy(String text) {
        if (text == null) {
            return "";
        }
        if (text.indexOf('&') >= 0 || text.indexOf(ChatColor.COLOR_CHAR) >= 0) {
            return ChatColor.translateAlternateColorCodes('&', text);
        }
        try {
            return LEGACY.serialize(MiniMessage.miniMessage().deserialize(text));
        } catch (RuntimeException ex) {
            return text;
        }
    }

    /** Same per-currency colours X-Prison's recoloured tooltips use: tokens lime, gems magenta. */
    private static ChatColor currencyColor(String currency) {
        return switch (currency == null ? "" : currency.toLowerCase(Locale.ROOT)) {
            case "tokens" -> ChatColor.GREEN;
            case "gems" -> ChatColor.LIGHT_PURPLE;
            default -> ChatColor.GOLD;
        };
    }

    private static String currencyName(XPrisonCurrency currency) {
        return capitalize(ChatColor.stripColor(toLegacy(currency.getDisplayName())));
    }

    private static String singular(String label) {
        return label.endsWith("s") && label.length() > 1 ? label.substring(0, label.length() - 1) : label;
    }

    private static String capitalize(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private static String formatChance(double percent) {
        double clamped = Math.min(100.0, Math.max(0.0, percent));
        return clamped > 0 && clamped < 0.01 ? String.format(Locale.ROOT, "%.4f%%", clamped)
                : String.format(Locale.ROOT, "%,.2f%%", clamped);
    }

    private static String formatPercent(double percent) {
        return percent == Math.rint(percent)
                ? String.format(Locale.ROOT, "%,.0f%%", percent)
                : String.format(Locale.ROOT, "%,.1f%%", percent);
    }

    private static <T extends Enum<T>> T cycle(T[] values, T current, boolean forward) {
        int next = Math.floorMod(current.ordinal() + (forward ? 1 : -1), values.length);
        return values[next];
    }

    // ----------------------------------------------------------------------------------------
    // State
    // ----------------------------------------------------------------------------------------

    private static final class Holder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private static final class Session {
        private final int pickaxeSlot;
        private final Inventory inventory;
        private List<GuiWidget> widgets;

        private Session(int pickaxeSlot, Inventory inventory) {
            this.pickaxeSlot = pickaxeSlot;
            this.inventory = inventory;
            if (inventory.getHolder() instanceof Holder holder) {
                holder.inventory = inventory;
            }
        }
    }

    private enum SortMode {
        MENU("Menu Order"),
        UNLOCK("Unlock Level"),
        LEVEL("Your Level"),
        COST("Upgrade Cost");

        private final String label;

        SortMode(String label) {
            this.label = label;
        }
    }

    private enum FilterMode {
        ALL("All Enchants"),
        AFFORDABLE("Affordable"),
        NOT_MAXED("Not Maxed"),
        OWNED("Owned");

        private final String label;

        FilterMode(String label) {
            this.label = label;
        }
    }
}
