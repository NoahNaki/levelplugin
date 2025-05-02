package me.nakilex.levelplugin.salvage.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class SalvageGUI {

    // 3 rows of 9 = 27 slots
    private static final int GUI_SIZE  = 27;
    // last slot in the inventory
    public static final int SELL_SLOT = GUI_SIZE - 1;

    private static final String GUI_TITLE = ChatColor.DARK_GREEN + "Merchant";

    public static void openMerchantGUI(Player player) {
        Inventory merchantInv = Bukkit.createInventory(null, GUI_SIZE, GUI_TITLE);

        // Place the "Sell" button (emerald) in slot 26
        merchantInv.setItem(SELL_SLOT, createSellItem());

        player.openInventory(merchantInv);
    }

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
