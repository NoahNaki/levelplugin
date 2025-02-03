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


    // Hard-coded page purchase cost
    private int currentPageCost = 300;

    private static final int PAGE_SIZE      = 54;  // e.g., double chest size
    private static final int NAV_NEXT_SLOT  = 53;
    private static final int NAV_PREV_SLOT  = 45;

    public StorageGUI(UUID ownerId, StorageEvents storageEvents) {
        this.ownerId = ownerId;
        this.storageEvents = storageEvents;
        this.pages = new ArrayList<>();
        this.currentPage = 0;

        // Create first page:
        pages.add(createBlankPage(1));
    }

    private Inventory createBlankPage(int pageNumber) {
        String title = ChatColor.DARK_GREEN + "Personal Storage (Page " + pageNumber + ")";
        Inventory inv = Bukkit.createInventory(null, PAGE_SIZE, title);

        // Next page arrow
        inv.setItem(NAV_NEXT_SLOT, createNavigationItem(
            Material.ARROW, ChatColor.YELLOW + "Next Page"));

        // Previous page arrow (if pageNumber > 1)
        if (pageNumber > 1) {
            inv.setItem(NAV_PREV_SLOT, createNavigationItem(
                Material.ARROW, ChatColor.YELLOW + "Previous Page"));
        }
        return inv;
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

    // STEP 3-B (in StorageGUI.java, inside open() method)
    public void open(Player player) {
        Inventory currentInv = pages.get(currentPage);

        // Register the inventory so arrow clicks are handled!
        storageEvents.registerInventory(this, currentInv);

        player.openInventory(currentInv);
    }


    /**
     * Handles inventory clicks, including page navigation.
     */
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= PAGE_SIZE) {
            return; // Click outside the main inventory area
        }

        if (slot == NAV_NEXT_SLOT) {
            event.setCancelled(true);
            goToNextPage((Player) event.getWhoClicked());
        }
        else if (slot == NAV_PREV_SLOT && currentPage > 0) {
            event.setCancelled(true);
            goToPreviousPage((Player) event.getWhoClicked());
        }
        // Otherwise, allow normal item interaction
    }

    private void goToNextPage(Player player) {
        if (player == null) return;

        // If we’re on the last page, the player must buy/unlock a new page
        if (currentPage == pages.size() - 1) {
            EconomyManager econ = new EconomyManager(Bukkit.getPluginManager().getPlugin("LevelPlugin"));
            int currentBalance = econ.getBalance(player);

            // Use currentPageCost instead of a fixed 100
            if (currentBalance < currentPageCost) {
                player.sendMessage(ChatColor.RED + "You need " + currentPageCost + " coins to buy this page!");
                return;
            }

            // Deduct coins
            econ.deductCoins(player, currentPageCost);
            player.sendMessage(ChatColor.GREEN + "You purchased a new storage page for "
                + currentPageCost + " coins!");

            // Create a new page
            pages.add(createBlankPage(pages.size() + 1));

            // Double the cost for the *next* new page
            currentPageCost *= 2;
        }

        // Move to the (newly created or existing) next page
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

    /**
     * Example usage of your FileHandler from previous code.
     */
    public void saveToDisk() {
        FileHandler fileHandler = new FileHandler();
        fileHandler.saveStorage(ownerId, pages);
    }

    public void loadFromDisk() {
        FileHandler fileHandler = new FileHandler();
        List<Inventory> loadedPages = fileHandler.loadStorage(ownerId);
        if (!loadedPages.isEmpty()) {
            pages.clear();
            pages.addAll(loadedPages);
        }
        currentPage = 0;
    }

    /**
     * If using StorageEvents to track your custom inventories,
     * obtain it however you prefer (static reference, plugin getter, etc.).
     */
    private StorageEvents getStorageEvents() {
        // Replace with your actual retrieval logic
        return null;
    }

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
