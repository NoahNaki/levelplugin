package me.nakilex.levelplugin.auctionhouse.gui;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.items.ItemBuilder;
import me.nakilex.levelplugin.auctionhouse.AuctionHouseManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ListingMenu {
    public static final String TITLE = ChatColor.DARK_GREEN + "List Item";

    private final AuctionHouseManager manager;
    private double price = 1.0;
    private ItemStack item;

    public ListingMenu(AuctionHouseManager manager) {
        this.manager = manager;
    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, TITLE);
        gui.setItem(11, item == null ? new ItemStack(Material.AIR) : item);
        gui.setItem(15, createPriceItem());
        gui.setItem(18, getOraxenItem("cross", ChatColor.RED + "Cancel"));
        gui.setItem(26, getOraxenItem("check", ChatColor.GREEN + "Confirm"));
        player.openInventory(gui);
    }

    private ItemStack createPriceItem() {
        ItemStack i = new ItemStack(Material.PAPER);
        ItemMeta m = i.getItemMeta();
        if (m != null) {
            m.setDisplayName(ChatColor.YELLOW + "Set Price: " + price);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Left/Right click +/-1");
            lore.add(ChatColor.GRAY + "Shift click +/-10");
            m.setLore(lore);
            i.setItemMeta(m);
        }
        return i;
    }

    private ItemStack create(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack getOraxenItem(String id, String name) {
        ItemBuilder builder = OraxenItems.getItemById(id);
        if (builder == null) return new ItemStack(Material.BARRIER);
        ItemStack item = builder.build();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void handleClick(Player player, int slot, boolean shift, boolean right) {
        if (slot == 11) {
            // set listing item from cursor
            ItemStack cursor = player.getItemOnCursor();
            if (cursor != null && cursor.getType() != Material.AIR) {
                this.item = cursor.clone();
                player.setItemOnCursor(null);
                open(player);
            }
        } else if (slot == 15) {
            double delta = shift ? 10 : 1;
            if (right) delta = -delta;
            price = Math.max(1, price + delta);
            open(player);
        } else if (slot == 26) {
            if (item != null) {
                manager.listItem(player, item, price);
                player.closeInventory();
            }
        } else if (slot == 18) {
            if (item != null) player.getInventory().addItem(item);
            player.closeInventory();
        }
    }
}
