package me.nakilex.levelplugin.auctionhouse;

import me.nakilex.levelplugin.economy.managers.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.widgets.ActionWidget;
import me.nakilex.levelplugin.utils.gui.widgets.GuiContext;
import me.nakilex.levelplugin.utils.gui.widgets.GuiLayout;
import me.nakilex.levelplugin.utils.gui.widgets.GuiWidget;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;

import java.util.*;

public class AuctionHouseGUI implements Listener {
    private static final String TITLE = "<glyph:judge_gavel_icon> Auction House";
    private static final int SIZE = 54;
    private static final int SELL_SLOT = 49;
    private static final int PREV_PAGE = 45;
    private static final int NEXT_PAGE = 53;
    private static final int SEARCH_SLOT = 47;
    private static final int FILTER_SLOT = 50;
    private static final int RARITY_FILTER_SLOT = 51;
    private static final int SORT_SLOT = 52;
    private static final int MY_LISTINGS_SLOT = 48;
    private static final int BACK_SLOT = 46;
    private static final String MY_LISTINGS_TITLE = "Your Listings";
    private static final int INFO_SLOT = 8;
    private static final int REFRESH_SLOT = 0;
    private static final int[] LISTING_SLOTS = {
            10,11,12,13,14,15,16,
            19,20,21,22,23,24,25,
            28,29,30,31,32,33,34,
            37,38,39,40,41,42,43
    };
    private static final int ITEMS_PER_PAGE = LISTING_SLOTS.length;

    private final JavaPlugin plugin;
    private final AuctionHouseManager manager;
    private final EconomyManager economy;
    private final NamespacedKey listingKey;
    private final AuctionDialogService dialogs;

    private final Map<UUID, Integer> pageMap = new HashMap<>();
    private final Map<UUID, String> searchTerms = new HashMap<>();
    private final Map<UUID, Integer> levelFilters = new HashMap<>();
    private final Map<UUID, Integer> rarityFilters = new HashMap<>();
    private final Map<UUID, Integer> sortModes = new HashMap<>();
    private final Map<UUID, Integer> myPageMap = new HashMap<>();
    private final List<GuiWidget> widgets;
    private boolean canPrevPage;
    private boolean canNextPage;

    public AuctionHouseGUI(JavaPlugin plugin, AuctionHouseManager manager, EconomyManager economy) {
        this.plugin = plugin;
        this.manager = manager;
        this.economy = economy;
        this.listingKey = new NamespacedKey(plugin, "auction_listing_id");
        this.dialogs = new AuctionDialogService(plugin, manager, economy);
        this.widgets = buildWidgets();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        open(player, pageMap.getOrDefault(player.getUniqueId(), 0));
    }

    private void open(Player player, int page) {
        pageMap.put(player.getUniqueId(), page);
        levelFilters.putIfAbsent(player.getUniqueId(), 5);
        rarityFilters.putIfAbsent(player.getUniqueId(), ItemRarity.values().length);
        Inventory inv = GuiBuilder.create(SIZE, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .fillEmptySlots(false)
                .border()
                .build();
        String term = searchTerms.getOrDefault(player.getUniqueId(), "");
        int filter = levelFilters.getOrDefault(player.getUniqueId(), 5);
        int rarityFilter = rarityFilters.getOrDefault(player.getUniqueId(), ItemRarity.values().length);
        int sort = sortModes.getOrDefault(player.getUniqueId(), 0);
        List<AuctionItem> list = new ArrayList<>();
        for (AuctionItem ai : manager.getAuctions()) {
            if (!matchesSearch(ai, term)) continue;
            if (!matchesLevelFilter(ai, filter)) continue;
            if (!matchesRarityFilter(ai, rarityFilter)) continue;
            list.add(ai);
        }
        sortAuctions(list, sort);
        int startIndex = page * ITEMS_PER_PAGE;
        int slot = 0;
        for (int i = startIndex; i < list.size() && slot < ITEMS_PER_PAGE; i++) {
            AuctionItem ai = list.get(i);
            ItemStack stack = ai.getItem().clone();
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add(" ");
                int bid = ai.getCurrentBid() > 0 ? ai.getCurrentBid() : ai.getStartingPrice();
                lore.add(ChatColor.GOLD + "" + ChatColor.BOLD + "Bid" + ChatColor.WHITE + ": " + bid + ChatColor.YELLOW + " <glyph:coins_icon>");
                if (ai.getBinPrice() > 0) {
                    lore.add(ChatColor.GOLD + "" + ChatColor.BOLD + "BIN" + ChatColor.WHITE + ": " + ai.getBinPrice() + ChatColor.YELLOW + " <glyph:coins_icon>");
                }
                long left = (ai.getEndTime() - System.currentTimeMillis()) / 1000;
                long mins = left / 60;
                lore.add(ChatColor.GOLD + "" + ChatColor.BOLD + "Duration" + ChatColor.WHITE + ": " + mins + "m");
                lore.add("");
                if (ai.getSeller().equals(player.getUniqueId())) {
                    lore.add(ChatColor.RED + "Click to cancel listing");
                } else {
                    lore.add("");
                    lore.addAll(TooltipUtil.clickInstructions("to buy", "to bid"));
                }
                meta.setLore(lore);
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                meta.getPersistentDataContainer().set(
                        listingKey,
                        PersistentDataType.STRING,
                        ai.getListingId().toString()
                );
                stack.setItemMeta(meta);
            }
            inv.setItem(LISTING_SLOTS[slot++], stack);
        }
        canPrevPage = page > 0;
        canNextPage = list.size() > (page + 1) * ITEMS_PER_PAGE;
        renderWidgets(inv, player);

        player.openInventory(inv);
    }

    private void renderWidgets(Inventory inventory, Player player) {
        GuiLayout layout = new GuiLayout(inventory);
        GuiContext context = new GuiContext(player, inventory);
        for (GuiWidget widget : widgets) {
            widget.contribute(layout, context);
        }
    }

    private boolean handleWidgetClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        GuiWidget widget = widgets.stream()
                .filter(w -> w.handlesSlot(slot))
                .findFirst()
                .orElse(null);
        if (widget == null) {
            return false;
        }
        event.setCancelled(true);
        if (event.getWhoClicked() instanceof Player player) {
            widget.onClick(slot, event.getClick(), new GuiContext(player, event.getView().getTopInventory()));
        }
        return true;
    }

    private List<GuiWidget> buildWidgets() {
        List<GuiWidget> widgetList = new ArrayList<>();
        widgetList.add(new ActionWidget(REFRESH_SLOT, context -> createRefreshButton(),
                (click, context) -> handleRefreshClick(context.player())));
        widgetList.add(new ActionWidget(SELL_SLOT, context -> createSellButton(),
                (click, context) -> handleSellClick(context.player())));
        widgetList.add(new ActionWidget(MY_LISTINGS_SLOT, context -> createMyListingsButton(),
                (click, context) -> openMyListings(context.player())));
        widgetList.add(new ActionWidget(SEARCH_SLOT,
                context -> createSearchButton(searchTerms.getOrDefault(context.player().getUniqueId(), "")),
                (click, context) -> handleSearchClick(context.player(), click)));
        widgetList.add(new ActionWidget(FILTER_SLOT,
                context -> createLevelFilterButton(levelFilters.getOrDefault(context.player().getUniqueId(), 5)),
                (click, context) -> handleLevelFilterClick(context.player(), click)));
        widgetList.add(new ActionWidget(RARITY_FILTER_SLOT,
                context -> createRarityFilterButton(rarityFilters.getOrDefault(context.player().getUniqueId(), ItemRarity.values().length)),
                (click, context) -> handleRarityFilterClick(context.player(), click)));
        widgetList.add(new ActionWidget(SORT_SLOT,
                context -> createSortButton(sortModes.getOrDefault(context.player().getUniqueId(), 0)),
                (click, context) -> handleSortClick(context.player(), click)));
        widgetList.add(new ActionWidget(INFO_SLOT, context -> createInfoItem(), null));
        widgetList.add(new ActionWidget(PREV_PAGE,
                context -> canPrevPage ? createArrow(ChatColor.RED + "Previous", false) : null,
                (click, context) -> handlePrevPageClick(context.player())));
        widgetList.add(new ActionWidget(NEXT_PAGE,
                context -> canNextPage ? createArrow(ChatColor.GREEN + "Next", true) : null,
                (click, context) -> handleNextPageClick(context.player())));
        return widgetList;
    }

    private void handleRefreshClick(Player player) {
        open(player, pageMap.getOrDefault(player.getUniqueId(), 0));
    }

    private void handleSellClick(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) {
            player.sendMessage(ChatColor.RED + "Hold the item you wish to sell in your hand.");
            return;
        }
        if (me.nakilex.levelplugin.items.listeners.StaticItemListener.isStaticItem(hand)
                || me.nakilex.levelplugin.items.utils.ItemUtil.isSoulbound(hand)) {
            player.sendMessage(ChatColor.RED + "You cannot list that item.");
            return;
        }

        ItemStack expectedHandItem = hand.clone();
        dialogs.showSellDialog(
                player,
                expectedHandItem,
                () -> open(player, pageMap.getOrDefault(player.getUniqueId(), 0))
        );
    }

    private void handleSearchClick(Player player, ClickType click) {
        if (click == ClickType.RIGHT) {
            searchTerms.remove(player.getUniqueId());
            open(player, pageMap.getOrDefault(player.getUniqueId(), 0));
            return;
        }

        String current = searchTerms.getOrDefault(player.getUniqueId(), "");
        dialogs.showSearchDialog(
                player,
                current,
                term -> {
                    if (term == null || term.isBlank()) {
                        searchTerms.remove(player.getUniqueId());
                    } else {
                        searchTerms.put(player.getUniqueId(), term);
                    }
                    pageMap.put(player.getUniqueId(), 0);
                    open(player, 0);
                },
                () -> open(player, pageMap.getOrDefault(player.getUniqueId(), 0))
        );
    }

    private void handleLevelFilterClick(Player player, ClickType click) {
        int filter = levelFilters.getOrDefault(player.getUniqueId(), 5);
        switch (click) {
            case RIGHT -> filter = (filter + 5) % 6;
            default -> filter = (filter + 1) % 6;
        }
        levelFilters.put(player.getUniqueId(), filter);
        open(player, pageMap.getOrDefault(player.getUniqueId(), 0));
    }

    private void handleRarityFilterClick(Player player, ClickType click) {
        int filter = rarityFilters.getOrDefault(player.getUniqueId(), ItemRarity.values().length);
        int total = ItemRarity.values().length + 1;
        switch (click) {
            case RIGHT -> filter = (filter + total - 1) % total;
            default -> filter = (filter + 1) % total;
        }
        rarityFilters.put(player.getUniqueId(), filter);
        open(player, pageMap.getOrDefault(player.getUniqueId(), 0));
    }

    private void handleSortClick(Player player, ClickType click) {
        int mode = sortModes.getOrDefault(player.getUniqueId(), 0);
        int total = 7;
        switch (click) {
            case RIGHT -> mode = (mode + total - 1) % total;
            default -> mode = (mode + 1) % total;
        }
        sortModes.put(player.getUniqueId(), mode);
        open(player, pageMap.getOrDefault(player.getUniqueId(), 0));
    }

    private void handlePrevPageClick(Player player) {
        if (!canPrevPage) {
            return;
        }
        int page = Math.max(0, pageMap.getOrDefault(player.getUniqueId(), 0) - 1);
        open(player, page);
    }

    private void handleNextPageClick(Player player) {
        if (!canNextPage) {
            return;
        }
        int page = pageMap.getOrDefault(player.getUniqueId(), 0) + 1;
        open(player, page);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getView().getTitle().equals(MY_LISTINGS_TITLE)) {
            handleMyListingsClick(e);
            return;
        }

        if (!e.getView().getTitle().equals(TITLE)) return;
        e.setCancelled(true);
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        Player player = (Player) e.getWhoClicked();

        if (handleWidgetClick(e)) {
            return;
        }

        AuctionItem ai = resolveClickedAuction(clicked);
        if (ai == null) {
            player.sendMessage(ChatColor.RED + "That auction is no longer available.");
            open(player, pageMap.getOrDefault(player.getUniqueId(), 0));
            return;
        }

        UUID listingId = ai.getListingId();
        Runnable returnToBrowser = () -> open(player, pageMap.getOrDefault(player.getUniqueId(), 0));

        if (ai.getSeller().equals(player.getUniqueId())) {
            dialogs.showCancelListingDialog(player, listingId, returnToBrowser);
            return;
        }

        if (e.getClick() == ClickType.RIGHT) {
            dialogs.showBidDialog(player, listingId, returnToBrowser);
        } else if (ai.getBinPrice() > 0) {
            dialogs.showPurchaseDialog(player, listingId, returnToBrowser);
        } else {
            player.sendMessage(ChatColor.RED + "This item has no BIN price. Right-click it to place a bid.");
        }
    }

    private ItemStack createFiller() {
        ItemStack it = new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack createSellButton() {
        ItemStack it = new ItemStack(Material.EMERALD);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "List Item (hand)");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Hold an item in your hand and click.");
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack createMyListingsButton() {
        ItemStack it = new ItemStack(Material.BOOK);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "My Listings");
            meta.setLore(Collections.singletonList(ChatColor.GRAY + "View and cancel your listings."));
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack createRefreshButton() {
        return getNexoItem("refresh", ChatColor.GREEN + "Refresh");
    }

    private ItemStack createArrow(String name, boolean right) {
        return getNexoItem(right ? "arrow_right" : "arrow_left", name);
    }

    private ItemStack createSearchButton(String term) {
        ItemStack it = getNexoItem("search", ChatColor.GOLD + "Search");
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            if (term != null && !term.isEmpty()) {
                lore.add(ChatColor.GRAY + "Current: " + ChatColor.WHITE + term);
            } else {
                lore.add(ChatColor.GRAY + "Click to enter a term");
            }
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack createLevelFilterButton(int filter) {
        ItemStack it = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Level Filter");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "");
            lore.add(ChatColor.DARK_GRAY + "Filters the content of the page by the item level range");
            lore.add(" ");
            lore.add(rangeLine(0, filter, "Lv. 1-19"));
            lore.add(rangeLine(1, filter, "Lv. 20-39"));
            lore.add(rangeLine(2, filter, "Lv. 40-59"));
            lore.add(rangeLine(3, filter, "Lv. 60-79"));
            lore.add(rangeLine(4, filter, "Lv. 80+"));
            lore.add(rangeLine(5, filter, "Show All"));
            lore.add(" ");
            lore.addAll(TooltipUtil.clickInstructions("to go forward", "to go backward"));
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack createRarityFilterButton(int filter) {
        ItemStack it = new ItemStack(Material.HOPPER);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Rarity Filter");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "");
            lore.add(ChatColor.DARK_GRAY + "Filters the content of the page by item rarity");
            lore.add(" ");
            ItemRarity[] arr = ItemRarity.values();
            for (int i = 0; i < arr.length; i++) {
                String name = arr[i].name().charAt(0) + arr[i].name().substring(1).toLowerCase();
                lore.add(rangeLine(i, filter, name));
            }
            lore.add(rangeLine(arr.length, filter, "Show All"));
            lore.add(" ");
            lore.add(ChatColor.WHITE + "Left-Click " + ChatColor.GRAY + "to go forward");
            lore.add(ChatColor.WHITE + "Right-Click " + ChatColor.GRAY + "to go backward");
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack createSortButton(int mode) {
        ItemStack it = new ItemStack(Material.COMPARATOR);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Sorting");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "");
            lore.add(ChatColor.DARK_GRAY + "Sort the content of the page");
            lore.add(" ");
            String[] opts = {"Highest Price", "Lowest Price", "Highest Bid", "Lowest Bid", "Ending Soon", "Ending Later", "Alphabetical"};
            for (int i = 0; i < opts.length; i++) {
                lore.add(rangeLine(i, mode, opts[i]));
            }
            lore.add(" ");
            lore.add(ChatColor.WHITE + "Left-Click " + ChatColor.GRAY + "to go forward");
            lore.add(ChatColor.WHITE + "Right-Click " + ChatColor.GRAY + "to go backward");
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack createInfoItem() {
        ItemStack info = getNexoItem("info", ChatColor.YELLOW + "Information");
        ItemMeta meta = info.getItemMeta();
        if (meta != null) {
            List<String> lines = new ArrayList<>();
            lines.add(ChatColor.GRAY + "");
            lines.addAll(TooltipUtil.clickInstructions("items to buy (if BIN set).", "items to place a bid."));
            lines.add(ChatColor.GRAY + "");
            lines.add(ChatColor.GRAY + "Use search and filters below.");
            meta.setLore(lines);
            info.setItemMeta(meta);
        }
        return info;
    }

    private ItemStack getNexoItem(String id, String name) {
        com.nexomc.nexo.items.ItemBuilder builder = com.nexomc.nexo.api.NexoItems.itemFromId(id);
        if (builder == null) return new ItemStack(Material.BARRIER);
        ItemStack item = builder.build();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void openMyListings(Player player) {
        openMyListings(player, myPageMap.getOrDefault(player.getUniqueId(), 0));
    }

    private void openMyListings(Player player, int page) {
        myPageMap.put(player.getUniqueId(), page);
        Inventory inv = GuiBuilder.create(SIZE, MY_LISTINGS_TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .fillEmptySlots(false)
                .border()
                .build();
        List<AuctionItem> list = new ArrayList<>();
        for (AuctionItem ai : manager.getAuctions()) {
            if (ai.getSeller().equals(player.getUniqueId())) {
                list.add(ai);
            }
        }
        int startIndex = page * ITEMS_PER_PAGE;
        int slot = 0;
        for (int i = startIndex; i < list.size() && slot < ITEMS_PER_PAGE; i++) {
            AuctionItem ai = list.get(i);
            ItemStack stack = ai.getItem().clone();
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add(" ");
                int bid = ai.getCurrentBid() > 0 ? ai.getCurrentBid() : ai.getStartingPrice();
                lore.add(ChatColor.GOLD + "" + ChatColor.BOLD + "Bid" + ChatColor.WHITE + ": " + bid + ChatColor.YELLOW + " <glyph:coins_icon>");
                if (ai.getBinPrice() > 0) {
                    lore.add(ChatColor.GOLD + "" + ChatColor.BOLD + "BIN" + ChatColor.WHITE + ": " + ai.getBinPrice() + ChatColor.YELLOW + " <glyph:coins_icon>");
                }
                long left = (ai.getEndTime() - System.currentTimeMillis()) / 1000;
                long mins = left / 60;
                lore.add(ChatColor.GOLD + "" + ChatColor.BOLD + "Duration" + ChatColor.WHITE + ": " + mins + "m");
                lore.add("");
                lore.add(ChatColor.RED + "Click to cancel listing");
                meta.setLore(lore);
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                meta.getPersistentDataContainer().set(
                        listingKey,
                        PersistentDataType.STRING,
                        ai.getListingId().toString()
                );
                stack.setItemMeta(meta);
            }
            inv.setItem(LISTING_SLOTS[slot++], stack);
        }
        if (page > 0) inv.setItem(PREV_PAGE, createArrow(ChatColor.RED + "Previous", false));
        if (list.size() > (page + 1) * ITEMS_PER_PAGE) inv.setItem(NEXT_PAGE, createArrow(ChatColor.GREEN + "Next", true));
        inv.setItem(BACK_SLOT, createArrow(ChatColor.RED + "Back", false));
        inv.setItem(SELL_SLOT, createSellButton());
        player.openInventory(inv);
    }

    private void handleMyListingsClick(InventoryClickEvent e) {
        e.setCancelled(true);
        Player player = (Player) e.getWhoClicked();
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        int raw = e.getRawSlot();

        if (raw == BACK_SLOT && isArrow(clicked, "Back")) {
            open(player, pageMap.getOrDefault(player.getUniqueId(), 0));
            return;
        }

        if (raw == SELL_SLOT) {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand == null || hand.getType().isAir()) {
                player.sendMessage(ChatColor.RED + "Hold the item you wish to sell in your hand.");
                return;
            }
            if (me.nakilex.levelplugin.items.listeners.StaticItemListener.isStaticItem(hand)
                    || me.nakilex.levelplugin.items.utils.ItemUtil.isSoulbound(hand)) {
                player.sendMessage(ChatColor.RED + "You cannot list that item.");
                return;
            }

            ItemStack expectedHandItem = hand.clone();
            dialogs.showSellDialog(
                    player,
                    expectedHandItem,
                    () -> openMyListings(player, myPageMap.getOrDefault(player.getUniqueId(), 0))
            );
            return;
        }

        if (raw == NEXT_PAGE && isArrow(clicked, "Next")) {
            int page = myPageMap.getOrDefault(player.getUniqueId(), 0) + 1;
            openMyListings(player, page);
            return;
        }

        if (raw == PREV_PAGE && isArrow(clicked, "Previous")) {
            int page = Math.max(0, myPageMap.getOrDefault(player.getUniqueId(), 0) - 1);
            openMyListings(player, page);
            return;
        }

        AuctionItem auction = resolveClickedAuction(clicked);
        if (auction == null) {
            player.sendMessage(ChatColor.RED + "That auction is no longer available.");
            openMyListings(player, myPageMap.getOrDefault(player.getUniqueId(), 0));
            return;
        }

        dialogs.showCancelListingDialog(
                player,
                auction.getListingId(),
                () -> openMyListings(player, myPageMap.getOrDefault(player.getUniqueId(), 0))
        );
    }

    private AuctionItem resolveClickedAuction(ItemStack clicked) {
        if (clicked == null || !clicked.hasItemMeta()) return null;
        String rawId = clicked.getItemMeta().getPersistentDataContainer()
                .get(listingKey, PersistentDataType.STRING);
        if (rawId == null) return null;
        try {
            return manager.getAuction(UUID.fromString(rawId));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private String rangeLine(int index, int current, String label) {
        ChatColor color = (index == current) ? ChatColor.WHITE : ChatColor.GRAY;
        ChatColor bullet = (index == current) ? ChatColor.GREEN : ChatColor.DARK_GRAY;
        return bullet + "- " + color + label;
    }

    private boolean isArrow(ItemStack item, String name) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasDisplayName()) return false;
        String disp = ChatColor.stripColor(meta.getDisplayName());
        return disp.equalsIgnoreCase(name);
    }

    private boolean matchesSearch(AuctionItem ai, String term) {
        if (term == null || term.isEmpty()) return true;
        ItemStack stack = ai.getItem();
        String name = stack.hasItemMeta() && stack.getItemMeta().hasDisplayName() ?
                ChatColor.stripColor(stack.getItemMeta().getDisplayName()) : stack.getType().name();
        return name.toLowerCase().contains(term.toLowerCase());
    }

    private boolean matchesLevelFilter(AuctionItem ai, int filter) {
        if (filter == 5) return true;
        int level = 0;
        try {
            me.nakilex.levelplugin.items.data.CustomItem ci = me.nakilex.levelplugin.items.managers.ItemManager.getInstance()
                    .getCustomItemFromItemStack(ai.getItem());
            if (ci != null) level = ci.getLevelRequirement();
        } catch (Exception ignored) {}
        return switch (filter) {
            case 0 -> level >= 1 && level <= 19;
            case 1 -> level >= 20 && level <= 39;
            case 2 -> level >= 40 && level <= 59;
            case 3 -> level >= 60 && level <= 79;
            case 4 -> level >= 80;
            default -> true;
        };
    }

    private boolean matchesRarityFilter(AuctionItem ai, int filter) {
        if (filter == ItemRarity.values().length) return true;
        try {
            me.nakilex.levelplugin.items.data.CustomItem ci = me.nakilex.levelplugin.items.managers.ItemManager.getInstance()
                    .getCustomItemFromItemStack(ai.getItem());
            if (ci != null) {
                return ci.getRarity() == ItemRarity.values()[filter];
            }
        } catch (Exception ignored) {}
        return false;
    }

    private void sortAuctions(List<AuctionItem> list, int mode) {
        Comparator<AuctionItem> comp = switch (mode) {
            case 0 -> Comparator.comparingInt(this::getPrice).reversed();
            case 1 -> Comparator.comparingInt(this::getPrice);
            case 2 -> Comparator.comparingInt(AuctionItem::getCurrentBid).reversed();
            case 3 -> Comparator.comparingInt(AuctionItem::getCurrentBid);
            case 4 -> Comparator.comparingLong(AuctionItem::getEndTime);
            case 5 -> Comparator.comparingLong(AuctionItem::getEndTime).reversed();
            case 6 -> Comparator.comparing(ai -> getItemName(ai.getItem()), String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparingLong(AuctionItem::getEndTime);
        };
        list.sort(comp);
    }

    private int getPrice(AuctionItem ai) {
        return ai.getBinPrice() > 0 ? ai.getBinPrice() : ai.getStartingPrice();
    }

    private String getItemName(ItemStack stack) {
        if (stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()) {
            return ChatColor.stripColor(stack.getItemMeta().getDisplayName());
        }
        return stack.getType().name();
    }
}
