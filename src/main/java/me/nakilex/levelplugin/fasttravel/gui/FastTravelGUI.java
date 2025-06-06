package me.nakilex.levelplugin.fasttravel.gui;

import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.fasttravel.FastTravelManager;
import me.nakilex.levelplugin.fasttravel.data.FastTravelPoint;
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

import java.util.HashMap;
import java.util.Map;

public class FastTravelGUI implements Listener {
    private final FastTravelManager manager;
    private final EconomyManager economy;
    private final Map<Player, Inventory> open = new HashMap<>();

    public FastTravelGUI(FastTravelManager manager, EconomyManager economy) {
        this.manager = manager;
        this.economy = economy;
    }

    public void open(Player player) {
        int size = 9 * Math.max(1, (int) Math.ceil(manager.getPoints().size() / 9.0));
        Inventory gui = Bukkit.createInventory(null, size, "Fast Travel");
        for (FastTravelPoint pt : manager.getPoints()) {
            boolean unlocked = manager.isUnlocked(player, pt.getName());
            ItemStack item = new ItemStack(unlocked ? Material.LODESTONE : Material.BARRIER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(pt.getColor() + pt.getName());
                if (unlocked) {
                    int cost = (int) player.getLocation().distance(pt.getLocation());
                    meta.setLore(java.util.List.of(ChatColor.GRAY + pt.getDescription(), ChatColor.YELLOW + "Cost: " + cost + " coins"));
                } else {
                    meta.setLore(java.util.List.of(ChatColor.DARK_GRAY + "Locked"));
                }
                item.setItemMeta(meta);
            }
            gui.addItem(item);
        }
        open.put(player, gui);
        player.openInventory(gui);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory inv = open.get(player);
        if (inv == null || !inv.equals(event.getInventory())) return;
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inv.getSize()) return;
        ItemStack clicked = inv.getItem(slot);
        if (clicked == null || clicked.getType() == Material.BARRIER) return;

        FastTravelPoint target = (FastTravelPoint) manager.getPoints().toArray()[slot];
        if (!manager.isUnlocked(player, target.getName())) return;
        int cost = (int) player.getLocation().distance(target.getLocation());
        if (economy.getBalance(player) < cost) {
            player.sendMessage(ChatColor.RED + "You need " + cost + " coins to travel.");
            return;
        }
        economy.deductCoins(player, cost);
        player.teleport(target.getLocation());
        player.sendMessage(ChatColor.GREEN + "Fast traveled to " + target.getName() + " for " + cost + " coins.");
        player.closeInventory();
    }
}
