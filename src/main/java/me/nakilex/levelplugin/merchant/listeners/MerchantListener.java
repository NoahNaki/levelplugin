package me.nakilex.levelplugin.merchant.listeners;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.merchant.gui.MerchantGUI;
import me.nakilex.levelplugin.merchant.managers.MerchantManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class MerchantListener implements Listener {

    private final EconomyManager economyManager;

    public MerchantListener(EconomyManager economyManager) {
        this.economyManager = economyManager;
    }

    /**
     * Handles clicks inside the merchant GUI:
     * 1. Prevent invalid items from being placed into slots 0-7.
     * 2. "Sell" items if slot 8 is clicked.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (topInventory == null) return;

        // Check that this click is in the Merchant GUI by matching title
        if (!isMerchantInventory(event.getView().getTitle())) {
            return; // It's not the merchant inventory, do nothing
        }

        // If the click is in the player's own inventory, but SHIFT-click is used,
        // the item could go into the merchant inventory automatically, so we must verify.
        if (event.isShiftClick()) {
            // SHIFT-click means the server tries to move the stack to the other inventory automatically
            // If the top inventory is the merchant, let's see if it will land in slots 0-7
            if (event.getCurrentItem() != null) {
                ItemStack currentItem = event.getCurrentItem();
                CustomItem cItem = ItemManager.getInstance().getCustomItemFromItemStack(currentItem);

                // If it's not a valid custom item, cancel.
                if (cItem == null) {
                    event.setCancelled(true);
                }
            }
            // NOTE: Because SHIFT-click can fill multiple open slots, the best practice is
            // to ensure we only let SHIFT-click happen if the item is valid. If it's invalid, cancel.
            return;
        }

        // For normal clicks (including drop-and-drag within InventoryClickEvent):
        // Check if they're clicking in the merchant inventory slots
        if (event.getClickedInventory() != null
            && event.getClickedInventory().equals(topInventory)) {

            int slot = event.getRawSlot();

            // If they clicked the "Sell" button (emerald) in slot 8:
            if (slot == 8) {
                event.setCancelled(true);
                handleSellButtonClick(event);
            }
            else {
                // They are trying to place or move an item in slot 0-7
                // Validate that the item is a custom item
                if (slot >= 0 && slot < 8) {
                    ItemStack cursorItem = event.getCursor(); // Item on the cursor
                    // If they're moving an item from the cursor into the slot
                    if (cursorItem != null && cursorItem.getType() != Material.AIR) {
                        CustomItem cItem = ItemManager.getInstance().getCustomItemFromItemStack(cursorItem);
                        if (cItem == null) {
                            // Not a valid custom item
                            event.setCancelled(true);
                        }
                    }
                }
            }
        }
    }

    /**
     * Handles drag events where the player could be dragging items into multiple slots at once.
     */
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (topInventory == null) return;

        // Check if it's the merchant inventory
        if (!isMerchantInventory(event.getView().getTitle())) {
            return; // not our inventory
        }

        // If the drag is placing items into the top inventory, check all targeted slots
        for (int slot : event.getRawSlots()) {
            // If the slot is within the top inventory's range (in a single-row of size 9 => 0..8)
            if (slot < topInventory.getSize()) {
                // Specifically, if it's in 0-7 we need to check validity
                if (slot >= 0 && slot < 8) {
                    ItemStack draggedItem = event.getOldCursor(); // or event.getCursor()
                    if (draggedItem != null && draggedItem.getType() != Material.AIR) {
                        CustomItem cItem = ItemManager.getInstance().getCustomItemFromItemStack(draggedItem);
                        if (cItem == null) {
                            // Not valid -> cancel
                            event.setCancelled(true);
                            return;
                        }
                    }
                }
            }
        }
    }

    /**
     * Helper to detect if inventory title is the "Merchant" GUI.
     */
    private boolean isMerchantInventory(String title) {
        // Compare with MerchantGUI's title: ChatColor.DARK_GREEN + "Merchant"
        return ChatColor.stripColor(title).equalsIgnoreCase("Merchant");
    }

    /**
     * If slot 8 (emerald) is clicked, sell items from slots 0-7 and pay the player.
     */
    private void handleSellButtonClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory inv = event.getInventory();
        int totalCoins = 0;

        // Loop through slots 0-7 to collect items
        for (int i = 0; i < 8; i++) {
            ItemStack slotItem = inv.getItem(i);
            if (slotItem == null || slotItem.getType() == Material.AIR) {
                continue;
            }

            // Attempt to get the CustomItem
            CustomItem cItem = ItemManager.getInstance().getCustomItemFromItemStack(slotItem);
            if (cItem != null) {
                // Calculate value
                totalCoins += MerchantManager.getInstance().getSellPrice(cItem);
            }

            // Clear the slot after "selling"
            inv.setItem(i, null);
        }

        // Add coins to the player's balance
        economyManager.addCoins(player, totalCoins);

        // Notify the player
        if (totalCoins > 0) {
            player.sendMessage(ChatColor.GREEN + "You sold your items for " + totalCoins + " coins!");
        } else {
            player.sendMessage(ChatColor.YELLOW + "No valid items to sell.");
        }
    }
}
