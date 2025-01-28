package me.nakilex.levelplugin.merchant.listeners;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.merchant.gui.MerchantGUI;
import me.nakilex.levelplugin.merchant.managers.MerchantManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public class MerchantListener implements Listener {

    private final EconomyManager economyManager;

    // If you have a global instance of EconomyManager in Main,
    // you could do `this.economyManager = Main.getInstance().getEconomyManager();`
    // or pass it in via constructor.
    public MerchantListener(EconomyManager economyManager) {
        this.economyManager = economyManager;
    }

    @EventHandler
    public void onMerchantClick(InventoryClickEvent event) {
        // Check that this click is in the Merchant GUI
        Inventory inv = event.getInventory();
        if (inv == null) return;

        // Compare with the title from MerchantGUI
        if (!ChatColor.stripColor(event.getView().getTitle())
            .equals(ChatColor.stripColor("Merchant"))) {
            return; // Not our Merchant GUI
        }

        // If they clicked outside or in their own inventory, just allow normal behavior
        if (event.getClickedInventory() == null
            || event.getClickedInventory().equals(event.getWhoClicked().getInventory())) {
            return;
        }

        // We also want to see which slot they clicked
        int slot = event.getRawSlot();

        // If they clicked the "Sell" button (emerald) in slot 8:
        if (slot == 8) {
            event.setCancelled(true);

            Player player = (Player) event.getWhoClicked();
            int totalCoins = 0;

            // Loop through slots 0-7 to collect items
            for (int i = 0; i < 8; i++) {
                // Check each slot
                if (inv.getItem(i) != null) {
                    // Attempt to get the CustomItem
                    CustomItem cItem = ItemManager.getInstance()
                        .getCustomItemFromItemStack(inv.getItem(i));

                    if (cItem != null) {
                        // Calculate value
                        totalCoins += MerchantManager.getInstance().getSellPrice(cItem);
                    }

                    // Clear the slot after "selling"
                    inv.setItem(i, null);
                }
            }

            // Add coins to the player's balance
            economyManager.addCoins(player, totalCoins);

            // Notify the player
            if (totalCoins > 0) {
                player.sendMessage(ChatColor.GREEN + "You sold your items for "
                    + totalCoins + " coins!");
            } else {
                player.sendMessage(ChatColor.YELLOW + "No valid items to sell.");
            }
        }
        // else: Let them place/remove items in slots 0-7 normally
        // If you want them to be able to move items around, you can remove the cancel.
        // event.setCancelled(false);
    }
}
