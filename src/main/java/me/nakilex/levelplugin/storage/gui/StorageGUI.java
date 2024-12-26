package me.nakilex.levelplugin.storage.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


import java.util.ArrayList;
import java.util.List;

public class StorageGUI {

    private final List<Inventory> pages;
    private final Player owner;
    private int currentPage;

    public StorageGUI(Player player) {
        this.owner = player;
        this.pages = new ArrayList<>();
        this.currentPage = 0;
        loadStorage(); // Load saved data when initializing
        if (pages.isEmpty()) addPage(); // Ensure at least one page exists
    }


    // Create a new inventory page
    private void addPage() {
        Inventory page = Bukkit.createInventory(null, 54, "Storage - Page " + (pages.size() + 1));
        setupNavigationButtons(page);
        pages.add(page);

        // Register the new page with the listener
        me.nakilex.levelplugin.storage.listeners.StorageEvents.getInstance().registerGUI(page, this);
    }


    // Set up navigation buttons (Next and Previous)
    private void setupNavigationButtons(Inventory inventory) {
        ItemStack nextPage = new ItemStack(Material.ARROW);
        ItemMeta nextMeta = nextPage.getItemMeta();
        nextMeta.setDisplayName("Next Page");
        nextPage.setItemMeta(nextMeta);

        ItemStack prevPage = new ItemStack(Material.ARROW);
        ItemMeta prevMeta = prevPage.getItemMeta();
        prevMeta.setDisplayName("Previous Page");
        prevPage.setItemMeta(prevMeta);

        inventory.setItem(53, nextPage); // Next Page Button
        inventory.setItem(45, prevPage); // Previous Page Button
    }

    // Open the current page
    public void open(Player player) {
        Inventory currentInventory = pages.get(currentPage);

        // Ensure the current page is registered before opening
        me.nakilex.levelplugin.storage.listeners.StorageEvents.getInstance().registerGUI(currentInventory, this);

        player.openInventory(currentInventory);
    }


    // Handle clicks inside the GUI
    public void handleClick(InventoryClickEvent event) {
        System.out.println("DEBUG: Click detected inside GUI.");

        // Check if the inventory is a storage page
        if (!event.getView().getTitle().startsWith("Storage - Page")) {
            return; // Ignore clicks outside storage pages
        }

        // Validate the clicked item
        if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) {
            System.out.println("DEBUG: Clicked item is null or has no meta.");
            return;
        }

        String itemName = event.getCurrentItem().getItemMeta().getDisplayName();
        System.out.println("DEBUG: Clicked item name - " + itemName);

        // Handle navigation buttons
        if (itemName.equals("Next Page")) {
            System.out.println("DEBUG: Next Page button clicked.");
            event.setCancelled(true); // Cancel click on navigation button
            if (currentPage < pages.size() - 1) {
                currentPage++;
            } else {
                addPage(); // Add a new page if we're at the end
                currentPage++;
            }
            open(owner);
        } else if (itemName.equals("Previous Page")) {
            System.out.println("DEBUG: Previous Page button clicked.");
            event.setCancelled(true); // Cancel click on navigation button
            if (currentPage > 0) {
                currentPage--;
                open(owner);
            }
        } else {
            System.out.println("DEBUG: Non-navigation item clicked.");
            event.setCancelled(false); // Allow clicks on other items
        }
    }

    // Handle GUI close event
    public void handleClose(InventoryCloseEvent event) {
        saveStorage(); // Save inventory contents when closed
    }


    // Get the inventory of the current page
    public Inventory getInventory() {
        return pages.get(currentPage);
    }

    // Save all pages to a file
    public void saveStorage() {
        File storageFile = new File(Bukkit.getPluginManager().getPlugin("LevelPlugin").getDataFolder(), owner.getUniqueId() + "_storage.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(storageFile);

        for (int i = 0; i < pages.size(); i++) {
            List<Map<String, Object>> items = new ArrayList<>();
            for (int slot = 0; slot < 54; slot++) {
                ItemStack item = pages.get(i).getItem(slot);
                if (item != null && item.getType() != Material.AIR) {
                    items.add(item.serialize());
                } else {
                    items.add(null); // Placeholder for empty slots
                }
            }
            config.set("page" + i, items); // Save each page
        }

        try {
            config.save(storageFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Load all pages from a file
    public void loadStorage() {
        File storageFile = new File(Bukkit.getPluginManager().getPlugin("LevelPlugin").getDataFolder(), owner.getUniqueId() + "_storage.yml");
        if (!storageFile.exists()) return; // No saved data

        FileConfiguration config = YamlConfiguration.loadConfiguration(storageFile);
        pages.clear(); // Clear any existing pages

        int pageCount = 0;
        while (config.contains("page" + pageCount)) {
            Inventory page = Bukkit.createInventory(null, 54, "Storage - Page " + (pageCount + 1));
            List<Map<String, Object>> items = (List<Map<String, Object>>) config.getList("page" + pageCount);

            for (int slot = 0; slot < 54; slot++) {
                if (items.get(slot) != null) {
                    page.setItem(slot, ItemStack.deserialize(items.get(slot))); // Restore items
                }
            }
            setupNavigationButtons(page); // Add buttons to restored pages
            pages.add(page);
            pageCount++;
        }

        if (pages.isEmpty()) addPage(); // Ensure at least one page exists
    }


}
