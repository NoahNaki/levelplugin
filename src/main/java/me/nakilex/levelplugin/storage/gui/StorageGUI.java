package me.nakilex.levelplugin.storage.gui;

import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.storage.data.FileHandler;
import me.nakilex.levelplugin.storage.events.StorageEvents;
import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.items.ItemBuilder;
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
import java.util.List;
import java.util.UUID;

public class StorageGUI {

    private final UUID ownerId;
    private final List<Inventory> pages;
    private int currentPage;
    private final StorageEvents storageEvents;

    /** Cost to unlock the next new page; starts at 300 and doubles each purchase */
    private int currentPageCost = 300;

    private boolean confirmUnlock = false;
    private int sortMode = 0;
    private int filterMode = 0;

    private static final int PAGE_SIZE     = 54;  // double chest size
    private static final int NAV_NEXT_SLOT = 53;
    private static final int NAV_PREV_SLOT = 45;
    private static final int SORT_SLOT     = 50;
    private static final int FILTER_SLOT   = 51;
    private static final int INFO_SLOT     = 8;
    private static final ItemStack FILLER  = createFiller();

    public StorageGUI(UUID ownerId, StorageEvents storageEvents) {
        this.ownerId = ownerId;
        this.storageEvents = storageEvents;
        this.pages = new ArrayList<>();
        this.currentPage = 0;

        // initialize with one blank page
        pages.add(createBlankPage(1));
    }

    /**
     * Creates an empty Inventory page without any nav items.
     * Nav items will be added/updated dynamically in open().
     */
    private Inventory createBlankPage(int pageNumber) {
        String title = ChatColor.DARK_GREEN + "Personal Storage (Page " + pageNumber + ")";
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
            if (confirmUnlock) {
                nextItem = getNexoItem("check",
                        ChatColor.GREEN + "Confirm " + currentPageCost + " ⛃");
            } else {
                nextItem = getNexoItem("arrow_right",
                        ChatColor.GRAY + "Unlock Page: " + ChatColor.YELLOW + currentPageCost + " ⛃");
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
        inv.setItem(SORT_SLOT, createSortButton(sortMode));
        inv.setItem(FILTER_SLOT, createFilterButton(filterMode));
        inv.setItem(INFO_SLOT, createInfoItem());

        // Register after opening so InventoryCloseEvent from the previous page
        // does not immediately unregister this one when navigating or
        // refreshing the same inventory.
        player.openInventory(inv);
        storageEvents.registerInventory(this, inv);
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
        else if (slot == NAV_PREV_SLOT && currentPage > 0) {
            event.setCancelled(true);
            goToPreviousPage((Player) event.getWhoClicked());
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
            if (filterMode > 1) filterMode = 0; if (filterMode < 0) filterMode = 1;
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
            if (!confirmUnlock) {
                confirmUnlock = true;
                open(player);
                return;
            }

            EconomyManager econ = new EconomyManager(
                Bukkit.getPluginManager().getPlugin("LevelPlugin")
            );
            int balance = econ.getBalance(player);
            if (balance < currentPageCost) {
                player.sendMessage(
                    ChatColor.RED + "You need " + currentPageCost + " coins to unlock a new page!"
                );
                confirmUnlock = false;
                open(player);
                return;
            }

            econ.deductCoins(player, currentPageCost);
            player.sendMessage(
                ChatColor.GREEN + "Purchased new storage page for " + currentPageCost + " coins!"
            );

            pages.add(createBlankPage(pages.size() + 1));
            currentPageCost *= 2;
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

    /** Persists all pages to disk under this owner's UUID. */
    public void saveToDisk() {
        FileHandler fileHandler = new FileHandler();
        fileHandler.saveStorage(ownerId, pages);
    }

    /** Loads pages from disk, replacing any in-memory pages. */
    public void loadFromDisk() {
        FileHandler fileHandler = new FileHandler();
        List<Inventory> loaded = fileHandler.loadStorage(ownerId);
        if (!loaded.isEmpty()) {
            pages.clear();
            pages.addAll(loaded);
        }
        currentPage = 0;
    }


    // standard getters
    public UUID getOwnerId() {
        return ownerId;
    }
    public List<Inventory> getPages() {
        return pages;
    }
    public int getCurrentPage() {
        return currentPage;
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
        ChatColor color = (index == current) ? ChatColor.WHITE : ChatColor.GRAY;
        ChatColor bullet = (index == current) ? ChatColor.GREEN : ChatColor.DARK_GRAY;
        return bullet + "- " + color + label;
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
            String[] opts = {"None", "A-Z", "Z-A"};
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
            String[] opts = {"Show All", "Blocks Only"};
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
            meta.setLore(List.of(
                    ChatColor.GRAY + "Personal bank storage.",
                    ChatColor.GRAY + "Use arrows to change pages."));
            info.setItemMeta(meta);
        }
        return info;
    }
}
