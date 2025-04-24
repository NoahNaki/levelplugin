package me.nakilex.levelplugin.storage.gui;

import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.storage.data.FileHandler;
import me.nakilex.levelplugin.storage.events.StorageEvents;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
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

    private static final int PAGE_SIZE     = 54;  // double chest size
    private static final int NAV_NEXT_SLOT = 53;
    private static final int NAV_PREV_SLOT = 45;

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
        String nextLabel;
        if (currentPage == pages.size() - 1) {
            nextLabel = ChatColor.YELLOW + "Purchase Page: " + currentPageCost + " coins";
        } else {
            nextLabel = ChatColor.YELLOW + "Next Page";
        }
        inv.setItem(NAV_NEXT_SLOT, createNavigationItem(Material.ARROW, nextLabel));

        // Previous arrow: only if not on first page
        if (currentPage > 0) {
            inv.setItem(NAV_PREV_SLOT,
                createNavigationItem(Material.ARROW, ChatColor.YELLOW + "Previous Page"));
        } else {
            inv.setItem(NAV_PREV_SLOT, null);
        }
    }

    private ItemStack createNavigationItem(Material material, String displayName) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Opens the current page for the player, refreshing nav tooltips first.
     */
    public void open(Player player) {
        Inventory inv = pages.get(currentPage);

        // refresh nav arrows to reflect unlock cost or page availability
        updateNavigationItems(inv);

        // register so clicks get forwarded to handleClick(...)
        storageEvents.registerInventory(this, inv);
        player.openInventory(inv);
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
        // otherwise allow regular interactions
    }

    private void goToNextPage(Player player) {
        if (player == null) return;

        // if on the last page, offer purchase
        if (currentPage == pages.size() - 1) {
            EconomyManager econ = new EconomyManager(
                Bukkit.getPluginManager().getPlugin("LevelPlugin")
            );
            int balance = econ.getBalance(player);
            if (balance < currentPageCost) {
                player.sendMessage(
                    ChatColor.RED + "You need " + currentPageCost + " coins to unlock a new page!"
                );
                return;
            }

            econ.deductCoins(player, currentPageCost);
            player.sendMessage(
                ChatColor.GREEN + "Purchased new storage page for " + currentPageCost + " coins!"
            );

            // add blank page and double the next cost
            pages.add(createBlankPage(pages.size() + 1));
            currentPageCost *= 2;
        }

        // advance page index and re-open
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

    // This method left unimplemented; replace with your actual lookup if needed.
    @SuppressWarnings("unused")
    private StorageEvents getStorageEvents() {
        return null;
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
}
