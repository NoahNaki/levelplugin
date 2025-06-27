package me.nakilex.levelplugin.player.classes.gui;

import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class AwakenWarriorMenu {
    private static final String TITLE = ChatColor.DARK_GREEN + "Warrior Awakening";

    public static void open(Player player) {
        Inventory inv = Bukkit.createInventory(player, 27, TITLE);
        inv.setItem(11, create(Material.IRON_AXE, ChatColor.RED + "Barbarian"));
        inv.setItem(13, create(Material.WOODEN_SHOVEL, ChatColor.GREEN + "Stay Warrior"));
        inv.setItem(15, create(Material.DIAMOND_SWORD, ChatColor.AQUA + "Dragonian"));
        player.openInventory(inv);
    }

    private static ItemStack create(Material mat, String name) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            it.setItemMeta(meta);
        }
        return it;
    }

    public static void handleSelect(Player player, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;
        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        PlayerClass chosen;
        switch (name.toLowerCase()) {
            case "barbarian":
                chosen = PlayerClass.BARBARIAN; break;
            case "dragonian":
                chosen = PlayerClass.DRAGONIAN; break;
            default:
                chosen = PlayerClass.WARRIOR; break;
        }
        StatsManager.getInstance().getPlayerStats(player.getUniqueId()).playerClass = chosen;
        player.closeInventory();
        player.sendMessage(ChatColor.GREEN + "You are now a " + ChatColor.AQUA + chosen.name());
    }
}
