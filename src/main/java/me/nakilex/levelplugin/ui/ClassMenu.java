package me.nakilex.levelplugin.ui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ClassMenu {

    public static Inventory getClassSelectionMenu() {
        Inventory inv = Bukkit.createInventory(null, 9, ChatColor.DARK_GREEN + "Choose Your Class");

        inv.setItem(1, createMenuItem(Material.WOODEN_SHOVEL, ChatColor.GRAY + "Warrior"));
        inv.setItem(2, createMenuItem(Material.BOW, ChatColor.GREEN + "Archer"));
        inv.setItem(3, createMenuItem(Material.STICK, ChatColor.LIGHT_PURPLE + "Mage"));
        inv.setItem(4, createMenuItem(Material.IRON_SWORD, ChatColor.RED + "Rogue"));

        return inv;
    }

    private static ItemStack createMenuItem(Material mat, String displayName) {
        ItemStack item = new ItemStack(mat, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        item.setItemMeta(meta);
        return item;
    }
}
