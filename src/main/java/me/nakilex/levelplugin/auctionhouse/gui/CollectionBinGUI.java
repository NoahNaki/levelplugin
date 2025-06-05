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

import java.util.List;

public class CollectionBinGUI {
    public static final String TITLE = ChatColor.DARK_GREEN + "Collection Bin";

    private final AuctionHouseManager manager;

    public CollectionBinGUI(AuctionHouseManager manager) {
        this.manager = manager;
    }

    public void open(Player player) {
        List<ItemStack> items = manager.getBin(player.getUniqueId());
        Inventory gui = Bukkit.createInventory(null, 54, TITLE);
        for (int i = 0; i < items.size() && i < 54; i++) {
            gui.setItem(i, items.get(i));
        }
        gui.setItem(53, getOraxenItem("cross", ChatColor.RED + "Close"));
        player.openInventory(gui);
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
}
