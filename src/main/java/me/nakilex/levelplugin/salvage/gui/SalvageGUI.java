package me.nakilex.levelplugin.salvage.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class SalvageGUI {

    private static final String GUI_TITLE = ChatColor.DARK_GREEN + "Merchant";
    private static final int GUI_SIZE = 9; // 1 row of 9 slots
    private static final int SELL_SLOT = 8; // Last slot in the row

    /**
     * Opens the salvage GUI for the given player
     */
    public static void openMerchantGUI(Player player) {
        Inventory merchantInv = Bukkit.createInventory(null, GUI_SIZE, GUI_TITLE);

        // Place the "Sell" button (emerald) in slot 8
        merchantInv.setItem(SELL_SLOT, createSellItem());

        // Open it for the player
        player.openInventory(merchantInv);
    }

    /**
     * Creates the emerald ItemStack representing the "Sell Items" button
     */
    private static ItemStack createSellItem() {
        ItemStack emerald = new ItemStack(Material.EMERALD);
        ItemMeta meta = emerald.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "Sell Items");
            emerald.setItemMeta(meta);
        }
        return emerald;
    }
}
