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
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class AuctionHouseGUI implements Listener {
    private static final String TITLE = ChatColor.GOLD + "Auction House";
    private static final int SIZE = 54;
    private static final int SELL_SLOT = 49;
    private static final int PREV_PAGE = 45;
    private static final int NEXT_PAGE = 53;
    private static final int SEARCH_SLOT = 47;
    private static final int FILTER_SLOT = 50;

    private final JavaPlugin plugin;
    private final AuctionHouseManager manager;
    private final EconomyManager economy;
    private final NamespacedKey indexKey;

    private static class ListingData {
        ItemStack item;
        int step = 0;
        int start;
        int bin;
        long duration;
    }

    private final Map<UUID, ListingData> pending = new HashMap<>();
    private final Map<UUID, Integer> pageMap = new HashMap<>();
    private final Map<UUID, String> searchTerms = new HashMap<>();
    private final Map<UUID, Integer> levelFilters = new HashMap<>();
    private final Set<UUID> awaitingSearch = new HashSet<>();

    public AuctionHouseGUI(JavaPlugin plugin, AuctionHouseManager manager, EconomyManager economy) {
        this.plugin = plugin;
        this.manager = manager;
        this.economy = economy;
        this.indexKey = new NamespacedKey(plugin, "auction_index");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        open(player, pageMap.getOrDefault(player.getUniqueId(), 0));
    }

    private void open(Player player, int page) {
        pageMap.put(player.getUniqueId(), page);
        levelFilters.putIfAbsent(player.getUniqueId(), 5);
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);
        ItemStack filler = createFiller();
        for (int i = 0; i < SIZE; i++) inv.setItem(i, filler);
        String term = searchTerms.getOrDefault(player.getUniqueId(), "");
        int filter = levelFilters.getOrDefault(player.getUniqueId(), 5);
        List<AuctionItem> list = new ArrayList<>();
        for (AuctionItem ai : manager.getAuctions()) {
            if (!matchesSearch(ai, term)) continue;
            if (!matchesLevelFilter(ai, filter)) continue;
            list.add(ai);
        }
        int startIndex = page * 45;
        int slot = 0;
        for (int i = startIndex; i < list.size() && slot < 45; i++) {
            AuctionItem ai = list.get(i);
            ItemStack stack = ai.getItem().clone();
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add(" ");
                lore.add(ChatColor.YELLOW + "Start: " + ai.getStartingPrice());
                if (ai.getCurrentBid() > 0) {
                    lore.add(ChatColor.AQUA + "Current bid: " + ai.getCurrentBid());
                }
                if (ai.getBinPrice() > 0) {
                    lore.add(ChatColor.GREEN + "BIN: " + ai.getBinPrice());
                }
                long left = (ai.getEndTime() - System.currentTimeMillis()) / 1000;
                long mins = left / 60;
                lore.add(ChatColor.GRAY + "Time left: " + mins + "m");
                lore.add(ChatColor.GRAY + "Category: " + ai.getCategory().name());
                if (ai.getSeller().equals(player.getUniqueId())) {
                    lore.add(ChatColor.RED + "Click to cancel listing");
                } else {
                    lore.add(ChatColor.GRAY + "Click to buy (BIN) or /ah bid " + i + " <amount>");
                }
                meta.setLore(lore);
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                meta.getPersistentDataContainer().set(indexKey, PersistentDataType.INTEGER, i);
                stack.setItemMeta(meta);
            }
            inv.setItem(slot++, stack);
        }

        if (page > 0) inv.setItem(PREV_PAGE, createArrow(ChatColor.RED + "Previous"));
        if (list.size() > (page + 1) * 45) inv.setItem(NEXT_PAGE, createArrow(ChatColor.GREEN + "Next"));
        inv.setItem(SELL_SLOT, createSellButton());
        inv.setItem(SEARCH_SLOT, createSearchButton(term));
        inv.setItem(FILTER_SLOT, createLevelFilterButton(filter));

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals(TITLE)) return;
        e.setCancelled(true);
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        Player player = (Player) e.getWhoClicked();

        int rawSlot = e.getRawSlot();
        if (rawSlot == SELL_SLOT) {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand == null || hand.getType().isAir()) {
                player.sendMessage(ChatColor.RED + "Hold the item you wish to sell in your hand.");
                return;
            }
            ListingData data = new ListingData();
            data.item = hand.clone();
            pending.put(player.getUniqueId(), data);
            player.getInventory().setItemInMainHand(null);
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "Enter starting price or 'cancel'.");
            return;
        }

        if (rawSlot == SEARCH_SLOT) {
            if (e.getClick() == ClickType.RIGHT) {
                searchTerms.remove(player.getUniqueId());
                open(player, pageMap.getOrDefault(player.getUniqueId(), 0));
            } else {
                awaitingSearch.add(player.getUniqueId());
                player.closeInventory();
                player.sendMessage(ChatColor.YELLOW + "Enter search term or 'cancel'.");
            }
            return;
        }

        if (rawSlot == FILTER_SLOT) {
            int filter = levelFilters.getOrDefault(player.getUniqueId(), 5);
            switch (e.getClick()) {
                case RIGHT -> filter = (filter + 5) % 6;
                default -> filter = (filter + 1) % 6;
            }
            levelFilters.put(player.getUniqueId(), filter);
            open(player, pageMap.getOrDefault(player.getUniqueId(), 0));
            return;
        }

        if (rawSlot == NEXT_PAGE) {
            int page = pageMap.getOrDefault(player.getUniqueId(), 0) + 1;
            open(player, page);
            return;
        }

        if (rawSlot == PREV_PAGE) {
            int page = Math.max(0, pageMap.getOrDefault(player.getUniqueId(), 0) - 1);
            open(player, page);
            return;
        }

        Integer idx = clicked.getItemMeta().getPersistentDataContainer().get(indexKey, PersistentDataType.INTEGER);
        if (idx == null) return;
        AuctionItem ai = manager.getAuctions().get(idx);
        if (ai.getSeller().equals(player.getUniqueId())) {
            if (manager.cancelListing(player, idx)) {
                player.sendMessage(ChatColor.RED + "Listing cancelled.");
            }
            Bukkit.getScheduler().runTaskLater(plugin, () -> open(player, pageMap.getOrDefault(player.getUniqueId(), 0)), 1L);
            return;
        }
        if (ai.getBinPrice() > 0) {
            manager.buyNow(player, idx);
            Bukkit.getScheduler().runTaskLater(plugin, () -> open(player, pageMap.getOrDefault(player.getUniqueId(), 0)), 1L);
        } else {
            player.sendMessage(ChatColor.YELLOW + "Use /auctionhouse bid " + idx + " <amount> to bid.");
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        if (awaitingSearch.remove(id)) {
            e.setCancelled(true);
            String msg = e.getMessage();
            if (msg.equalsIgnoreCase("cancel")) {
                searchTerms.remove(id);
            } else {
                searchTerms.put(id, msg.trim());
            }
            Bukkit.getScheduler().runTask(plugin, () -> open(e.getPlayer(), pageMap.getOrDefault(id, 0)));
            return;
        }
        ListingData data = pending.get(id);
        if (data == null) return;
        e.setCancelled(true);
        String msg = e.getMessage();
        if (msg.equalsIgnoreCase("cancel")) {
            ItemStack item = data.item;
            pending.remove(id);
            Bukkit.getScheduler().runTask(plugin, () -> e.getPlayer().getInventory().addItem(item));
            e.getPlayer().sendMessage(ChatColor.RED + "Listing cancelled.");
            return;
        }
        try {
            switch (data.step) {
                case 0 -> {
                    data.start = Integer.parseInt(msg);
                    data.step = 1;
                    e.getPlayer().sendMessage(ChatColor.YELLOW + "Enter BIN price or 0.");
                }
                case 1 -> {
                    data.bin = Integer.parseInt(msg);
                    data.step = 2;
                    e.getPlayer().sendMessage(ChatColor.YELLOW + "Enter duration in hours (e.g. 6)");
                }
                case 2 -> {
                    data.duration = Long.parseLong(msg);
                    ItemStack item = data.item;
                    pending.remove(id);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        manager.listItem(e.getPlayer(), item, data.start, data.bin, data.duration);
                        e.getPlayer().sendMessage(ChatColor.GREEN + "Item listed.");
                        open(e.getPlayer(), pageMap.getOrDefault(id, 0));
                    });
                }
            }
        } catch (NumberFormatException ex) {
            e.getPlayer().sendMessage(ChatColor.RED + "Invalid number. Type again or 'cancel'.");
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

    private ItemStack createArrow(String name) {
        ItemStack it = new ItemStack(Material.ARROW);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack createSearchButton(String term) {
        ItemStack it = new ItemStack(Material.OAK_SIGN);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Search");
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
            lore.add(ChatColor.GRAY + "Filters the content of the page by the item level range");
            lore.add(" ");
            lore.add(rangeLine(0, filter, "Lv. 1-19"));
            lore.add(rangeLine(1, filter, "Lv. 20-39"));
            lore.add(rangeLine(2, filter, "Lv. 40-59"));
            lore.add(rangeLine(3, filter, "Lv. 60-79"));
            lore.add(rangeLine(4, filter, "Lv. 80+"));
            lore.add(rangeLine(5, filter, "Show All"));
            lore.add(" ");
            lore.add(ChatColor.GRAY + "Left click to go forward");
            lore.add(ChatColor.GRAY + "Right click to go backward");
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private String rangeLine(int index, int current, String label) {
        ChatColor color = (index == current) ? ChatColor.WHITE : ChatColor.GRAY;
        ChatColor bullet = (index == current) ? ChatColor.GREEN : ChatColor.DARK_GRAY;
        return bullet + "- " + color + label;
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
}
