package me.nakilex.levelplugin.player.classes.gui;

import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class AwakenWarriorGUI implements Listener {

    public static void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_RED + "Choose Path");
        inv.setItem(11, createItem(Material.IRON_AXE, ChatColor.RED + "Barbarian"));
        inv.setItem(13, createItem(Material.IRON_SWORD, ChatColor.GRAY + "Stay Warrior"));
        inv.setItem(15, createItem(Material.DIAMOND_SWORD, ChatColor.AQUA + "Dragonian"));
        player.openInventory(inv);
    }

    private static ItemStack createItem(Material mat, String name) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            it.setItemMeta(meta);
        }
        return it;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!ChatColor.stripColor(e.getView().getTitle()).equalsIgnoreCase("Choose Path")) return;
        e.setCancelled(true);
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null) return;
        String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        PlayerClass newClass = null;
        switch (name.toLowerCase()) {
            case "barbarian": newClass = PlayerClass.BARBARIAN; break;
            case "stay warrior": newClass = PlayerClass.WARRIOR; break;
            case "dragonian": newClass = PlayerClass.DRAGONIAN; break;
        }
        if (newClass != null) {
            StatsManager.getInstance().getPlayerStats(player.getUniqueId()).playerClass = newClass;
            player.sendMessage(ChatColor.GREEN + "Class changed to " + newClass.name());
            player.closeInventory();
        }
    }
}
