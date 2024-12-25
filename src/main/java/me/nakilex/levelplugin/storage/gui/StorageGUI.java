package me.nakilex.levelplugin.storage.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class StorageGUI {

    private final Inventory inventory;

    public StorageGUI(Player player) {
        inventory = Bukkit.createInventory(null, InventoryType.CHEST, player.getName() + "'s Storage");
        setupGUI();
    }

    private void setupGUI() {
        // Example: Add a placeholder item
        ItemStack placeholder = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = placeholder.getItemMeta();
        meta.setDisplayName(" ");
        placeholder.setItemMeta(meta);

        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, placeholder);
        }
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true); // Prevent item movement for now
        // TODO: Handle actual item interactions later
    }

    public void handleClose(InventoryCloseEvent event) {
        // TODO: Save inventory contents when closed
    }

    public Inventory getInventory() {
        return inventory;
    }
}
