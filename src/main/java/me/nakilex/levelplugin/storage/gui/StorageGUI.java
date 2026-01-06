package me.nakilex.levelplugin.storage.gui;

import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.storage.data.FileHandler;
import me.nakilex.levelplugin.storage.events.StorageEvents;
import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.items.ItemBuilder;
import me.nakilex.levelplugin.utils.TooltipUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import static me.nakilex.levelplugin.utils.ChatMessageUtil.send;
import static me.nakilex.levelplugin.utils.ChatMessageUtil.sendPurchaseMessage;

public class StorageGUI {

    private final String ownerKey;
    private final String folder;
    private final String prefix;
    private final String titleBase;
    private final List<Inventory> pages;
    private int currentPage;
    private final StorageEvents storageEvents;
    private final boolean allowSoulbound;
    private int maxPages;

    /** Base price for unlocking storage pages. */
    private static final int BASE_PAGE_COST = 300;
    /** Cost to unlock the next new page. */
    private int currentPageCost = BASE_PAGE_COST;

    private boolean confirmUnlock = false;
    private int sortMode = 0;
    private int filterMode = 5;
    private final Map<Inventory, List<ItemStack>> hiddenItems = new HashMap<>();

    private static final int PAGE_SIZE     = 54;  // double chest size
    private static final int NAV_NEXT_SLOT = 53;
    private static final int NAV_PREV_SLOT = 45;
    private static final int SORT_SLOT     = 50;
    private static final int FILTER_SLOT   = 51;
    private static final int INFO_SLOT     = 8;
    private static final ItemStack FILLER  = createFiller();

    public StorageGUI(String ownerKey, String folder, String prefix, String titleBase, StorageEvents storageEvents, boolean allowSoulbound, int maxPages) {
        this.ownerKey = ownerKey;
        this.folder = folder;
        this.prefix = prefix;
        this.titleBase = titleBase;
        this.storageEvents = storageEvents;
        this.allowSoulbound = allowSoulbound;
        this.maxPages = maxPages;
        this.pages = new ArrayList<>();
        this.currentPage = 0;

        pages.add(createBlankPage(1));
        this.currentPageCost = BASE_PAGE_COST * pages.size();
    }

    /** Convenience constructor for personal storage using defaults. */
    public StorageGUI(String ownerKey, StorageEvents storageEvents) {
        this(ownerKey, "storage", "player_", "Personal Storage", storageEvents, true, Integer.MAX_VALUE);
    }

    public StorageGUI(String ownerKey, String folder, String prefix, String titleBase, StorageEvents storageEvents, boolean allowSoulbound) {
        this(ownerKey, folder, prefix, titleBase, storageEvents, allowSoulbound, Integer.MAX_VALUE);
    }

    /**
     * Creates an empty Inventory page without any nav items.
     * Nav items will be added/updated dynamically in open().
     */
    private Inventory createBlankPage(int pageNumber) {
        String title = titleBase + " (Page " + pageNumber + ")";
        return Bukkit.createInventory(null, PAGE_SIZE, title);
    }

    /**
     * Updates the Prev/Next arrow slots to show either navigation labels
     * or, when on the last locked page, a purchase tooltip with cost.
     */
    private void updateNavigationItems(Inventory inv) {
        // Next arrow: if on last page, show purchase cost; otherwise "Next Page"
        ItemStack nextItem;
        if (currentPage == pages.size() - 1) {
            if (pages.size() >= maxPages) {
                nextItem = FILLER.clone();
            } else if (confirmUnlock) {
                nextItem = getNexoItem("check",
                        ChatColor.GREEN + "Confirm " + currentPageCost + " <glyph:coins_icon>");
            } else {
                nextItem = getNexoItem("arrow_right",
                        ChatColor.GRAY + "Unlock Page: " + ChatColor.YELLOW + currentPageCost + " <glyph:coins_icon>");
            }
        } else {
            nextItem = getNexoItem("arrow_right", ChatColor.YELLOW + "Next Page");
        }
        inv.setItem(NAV_NEXT_SLOT, nextItem);

        // Previous arrow slot behavior
        ItemStack prevItem;
        if (confirmUnlock) {
            prevItem = getNexoItem("cross", ChatColor.RED + "Cancel");
        } else if (currentPage > 0) {
            prevItem = getNexoItem("arrow_left", ChatColor.YELLOW + "Previous Page");
        } else {
            prevItem = FILLER.clone();
        }
        inv.setItem(NAV_PREV_SLOT, prevItem);
    }

    /**
     * Opens the current page for the player, refreshing nav tooltips first.
     */
    public void open(Player player) {
        Inventory inv = pages.get(currentPage);

        // set filler border
        for (int i = 0; i < PAGE_SIZE; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, FILLER);
            }
        }

        updateNavigationItems(inv);
        applySortAndFilter(inv);
        inv.setItem(SORT_SLOT, createSortButton(sortMode));
        inv.setItem(FILTER_SLOT, createFilterButton(filterMode));
        inv.setItem(INFO_SLOT, createInfoItem());

        // Register after opening so InventoryCloseEvent from the previous page
        // does not immediately unregister this one when navigating or
        // refreshing the same inventory.
        player.openInventory(inv);
        storageEvents.registerInventory(this, inv);
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            if (player.isOnline() && player.getOpenInventory().getTopInventory().equals(inv)) {
                player.updateInventory();
            }
        }, 1L);
    }

    /**
     * Handles clicks inside the storage GUI, including nav arrows.
     */
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= PAGE_SIZE) {
            return; // clicked outside main area
        }

        if (slot == NAV_NEXT_SLOT) {
            event.setCancelled(true);
            goToNextPage((Player) event.getWhoClicked());
        }
        else if (slot == NAV_PREV_SLOT) {
            event.setCancelled(true);
            if (confirmUnlock) {
                // Cancel purchase confirmation and restore navigation arrow
                confirmUnlock = false;
                open((Player) event.getWhoClicked());
            } else if (currentPage > 0) {
                goToPreviousPage((Player) event.getWhoClicked());
            }
        }
        else if (slot == SORT_SLOT) {
            event.setCancelled(true);
            if (event.isLeftClick()) sortMode++; else sortMode--;
            if (sortMode > 2) sortMode = 0; if (sortMode < 0) sortMode = 2;
            open((Player) event.getWhoClicked());
        }
        else if (slot == FILTER_SLOT) {
            event.setCancelled(true);
            if (event.isLeftClick()) filterMode++; else filterMode--;
            if (filterMode > 5) filterMode = 0; if (filterMode < 0) filterMode = 5;
            Main.getInstance().getLogger().info(
                    "[StorageGUI] filterMode owner=" + ownerKey + " newMode=" + filterMode);
            open((Player) event.getWhoClicked());
        }
        else if (slot == INFO_SLOT || slot < 9 || slot >= 45 || slot % 9 == 0 || slot % 9 == 8) {
            // Prevent taking filler or info items
            event.setCancelled(true);
        }
        // otherwise allow regular interactions
    }

    /**
     * Handles drag events within the storage GUI. Any drag that targets
     * one of the protected menu slots is cancelled.
     */
    public void handleDrag(InventoryDragEvent event) {
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < 0 || rawSlot >= PAGE_SIZE) continue;

            if (rawSlot == NAV_NEXT_SLOT || rawSlot == NAV_PREV_SLOT ||
                rawSlot == SORT_SLOT || rawSlot == FILTER_SLOT ||
                rawSlot == INFO_SLOT || rawSlot < 9 || rawSlot >= 45 ||
                rawSlot % 9 == 0 || rawSlot % 9 == 8) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private void goToNextPage(Player player) {
        if (player == null) return;

        if (currentPage == pages.size() - 1) {
            if (pages.size() >= maxPages) {
                send(player, MessageType.ERROR, "Storage is at maximum pages.");
                confirmUnlock = false;
                open(player);
                return;
            }
            if (!confirmUnlock) {
                confirmUnlock = true;
                open(player);
                return;
            }

            EconomyManager econ = Main.getInstance().getEconomyManager();
            int balance = econ.getBalance(player);
            if (balance < currentPageCost) {
                send(player, MessageType.ERROR,
                        "You need " + ChatColor.YELLOW + currentPageCost + " <glyph:coins_icon> coins to unlock a new page!");
                confirmUnlock = false;
                open(player);
                return;
            }

            econ.deductCoins(player, currentPageCost);
            sendPurchaseMessage(player, ChatColor.YELLOW + "a new storage page", currentPageCost);

            pages.add(createBlankPage(pages.size() + 1));
            // next page cost scales with current page count
            currentPageCost = BASE_PAGE_COST * pages.size();
        }

        confirmUnlock = false;
        currentPage++;
        open(player);
    }

    private void goToPreviousPage(Player player) {
        if (player == null) return;
        if (currentPage > 0) {
            currentPage--;
            open(player);
        }
    }

    public void setMaxPages(int maxPages) {
        this.maxPages = maxPages;
    }

    /** Persists all pages to disk under this owner's UUID. */
    public void saveToDisk() {
        FileHandler fileHandler = new FileHandler();
        for (Inventory page : pages) {
            restoreHiddenItems(page);
        }
        fileHandler.saveStorage(ownerKey, pages, folder, prefix);
    }

    /** Loads pages from disk, replacing any in-memory pages. */
    public void loadFromDisk() {
        FileHandler fileHandler = new FileHandler();
        List<Inventory> loaded = fileHandler.loadStorage(ownerKey, folder, prefix, titleBase);
        if (!loaded.isEmpty()) {
            pages.clear();
            pages.addAll(loaded);
        }
        currentPage = 0;
        // recalculate unlock cost based on loaded page count
        this.currentPageCost = BASE_PAGE_COST * pages.size();
    }


    // standard getters
    public String getOwnerKey() {
        return ownerKey;
    }

    public boolean allowsSoulbound() {
        return allowSoulbound;
    }
    public List<Inventory> getPages() {
        return pages;
    }
    public int getCurrentPage() {
        return currentPage;
    }

    public boolean isFilterActive() {
        return filterMode != 5;
    }

    public boolean hasHiddenItems(Inventory inventory) {
        List<ItemStack> hidden = hiddenItems.get(inventory);
        return hidden != null && !hidden.isEmpty();
    }

    private static ItemStack createFiller() {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            glass.setItemMeta(meta);
        }
        return glass;
    }

    private static ItemStack getNexoItem(String id, String name) {
        ItemBuilder builder = NexoItems.itemFromId(id);
        if (builder == null) return new ItemStack(Material.BARRIER);
        ItemStack item = builder.build();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String rangeLine(int index, int current, String label) {
        return TooltipUtil.selectionLine(index == current, label);
    }

    private ItemStack createSortButton(int mode) {
        ItemStack it = new ItemStack(Material.COMPARATOR);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Sorting");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "");
            lore.add(ChatColor.DARK_GRAY + "Sort the items");
            lore.add(" ");
            String[] opts = {"None", "Rarity \u2193", "Rarity \u2191"};
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

    private ItemStack createFilterButton(int mode) {
        ItemStack it = new ItemStack(Material.HOPPER);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Filter");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "");
            lore.add(ChatColor.DARK_GRAY + "Filter items");
            lore.add(" ");
            String[] opts = {"Lv. 1-19", "Lv. 20-39", "Lv. 40-59", "Lv. 60-79", "Lv. 80+", "Show All"};
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

    protected ItemStack createInfoItem() {
        ItemStack info = getNexoItem("info", ChatColor.YELLOW + "Information");
        ItemMeta meta = info.getItemMeta();
        if (meta != null) {
            meta.setLore(List.of(
                    ChatColor.GRAY + "Personal bank storage.",
                    ChatColor.GRAY + "Use arrows to change pages."));
            info.setItemMeta(meta);
        }
        return info;
    }

    // -----------------------------------------------------------------------
    // Helper methods for sorting/filtering
    // -----------------------------------------------------------------------

    /** List of slots that store actual items (excluding borders and controls). */
    private static final List<Integer> STORAGE_SLOTS = new ArrayList<>();
    static {
        for (int i = 0; i < PAGE_SIZE; i++) {
            if (i == NAV_NEXT_SLOT || i == NAV_PREV_SLOT || i == SORT_SLOT ||
                i == FILTER_SLOT || i == INFO_SLOT) continue;
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) continue;
            STORAGE_SLOTS.add(i);
        }
    }

    private int getRarityOrdinal(ItemStack item) {
        me.nakilex.levelplugin.items.data.CustomItem ci =
            me.nakilex.levelplugin.items.managers.ItemManager.getInstance()
                .getCustomItemFromItemStack(item);
        return ci != null ? ci.getRarity().ordinal() : 0;
    }

    private Integer getItemLevelRequirement(ItemStack item) {
        return me.nakilex.levelplugin.items.utils.ItemUtil.getLevelRequirement(item);
    }

    private boolean matchesLevelFilter(Integer level, int filter) {
        if (filter == 5) return true;
        if (level == null) {
            return false;
        }
        return switch (filter) {
            case 0 -> level >= 1 && level <= 19;
            case 1 -> level >= 20 && level <= 39;
            case 2 -> level >= 40 && level <= 59;
            case 3 -> level >= 60 && level <= 79;
            case 4 -> level >= 80;
            default -> true;
        };
    }

    /** Apply current sort and filter settings to a page before showing it. */
    private void applySortAndFilter(Inventory inv) {
        List<ItemStack> items = collectStoredItems(inv);
        for (int slot : STORAGE_SLOTS) {
            inv.setItem(slot, null);
        }

        Main.getInstance().getLogger().info(
                "[StorageGUI] applySortAndFilter owner=" + ownerKey
                        + " page=" + (currentPage + 1)
                        + " sort=" + sortMode
                        + " filter=" + filterMode
                        + " items=" + items.size());

        if (sortMode == 1) {
            items.sort(java.util.Comparator.comparingInt(this::getRarityOrdinal).reversed());
        } else if (sortMode == 2) {
            items.sort(java.util.Comparator.comparingInt(this::getRarityOrdinal));
        }

        if (filterMode != 5) {
            List<ItemStack> matches = new ArrayList<>();
            List<ItemStack> nonMatches = new ArrayList<>();
            int unknownLevels = 0;
            for (ItemStack item : items) {
                Integer level = getItemLevelRequirement(item);
                boolean match = matchesLevelFilter(level, filterMode);
                if (level == null) {
                    unknownLevels++;
                }
                if (match) {
                    matches.add(item);
                } else {
                    nonMatches.add(item);
                }
            }
            Main.getInstance().getLogger().info(
                    "[StorageGUI] filterResults owner=" + ownerKey
                            + " matches=" + matches.size()
                            + " hidden=" + nonMatches.size()
                            + " unknownLevels=" + unknownLevels);
            hiddenItems.put(inv, nonMatches);
            items = matches;
        } else {
            hiddenItems.remove(inv);
        }

        int idx = 0;
        for (int slot : STORAGE_SLOTS) {
            if (idx >= items.size()) break;
            inv.setItem(slot, items.get(idx++));
        }
    }

    private List<ItemStack> collectStoredItems(Inventory inv) {
        List<ItemStack> items = new ArrayList<>();
        for (int slot : STORAGE_SLOTS) {
            ItemStack it = inv.getItem(slot);
            if (it != null && it.getType() != Material.AIR) {
                items.add(it);
            }
        }
        List<ItemStack> hidden = hiddenItems.get(inv);
        if (hidden != null) {
            items.addAll(hidden);
        }
        return items;
    }

    private void restoreHiddenItems(Inventory inv) {
        List<ItemStack> hidden = hiddenItems.get(inv);
        if (hidden == null || hidden.isEmpty()) {
            return;
        }
        List<ItemStack> items = collectStoredItems(inv);
        hiddenItems.remove(inv);
        for (int slot : STORAGE_SLOTS) {
            inv.setItem(slot, null);
        }
        int idx = 0;
        for (int slot : STORAGE_SLOTS) {
            if (idx >= items.size()) break;
            inv.setItem(slot, items.get(idx++));
        }
    }
}
