package me.nakilex.levelplugin.auctionhouse.gui;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.items.ItemBuilder;
import me.nakilex.levelplugin.auctionhouse.AuctionHouseManager;
import me.nakilex.levelplugin.auctionhouse.AuctionListing;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class AuctionGUI {
    public static final String TITLE = ChatColor.DARK_GREEN + "Auction House";

    private final AuctionHouseManager manager;

    public AuctionGUI(AuctionHouseManager manager) {
        this.manager = manager;
    }

    public void open(Player player, int page) {
        int size = 54;
        Inventory gui = Bukkit.createInventory(null, size, TITLE);
        List<AuctionListing> listings = new ArrayList<>(manager.getListings());

        int start = page * 45;
        for (int i = 0; i < 45; i++) {
            int index = start + i;
            if (index >= listings.size()) break;
            AuctionListing listing = listings.get(index);
            ItemStack item = listing.getItem().clone();
            ItemMeta meta = item.getItemMeta();
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Seller: " + Bukkit.getOfflinePlayer(listing.getSeller()).getName());
            lore.add(ChatColor.YELLOW + "Price: " + new DecimalFormat("0.##").format(listing.getPrice()));
            long remaining = (listing.getExpireAt() - System.currentTimeMillis()) / 1000L;
            lore.add(ChatColor.GRAY + "Time Left: " + remaining / 3600 + "h");
            lore.add(ChatColor.GREEN + "Left-click to buy");
            meta.setLore(lore);
            item.setItemMeta(meta);
            gui.setItem(i, item);
        }

        ItemStack info = getOraxenItem("info", ChatColor.YELLOW + "Information");
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setLore(List.of(
                    ChatColor.GRAY + "Browse player listings.",
                    ChatColor.GRAY + "Use the anvil to list items."
            ));
            info.setItemMeta(infoMeta);
        }
        gui.setItem(8, info);
        gui.setItem(45, getOraxenItem("cross", ChatColor.RED + "Close"));
        gui.setItem(49, create(Material.PLAYER_HEAD, ChatColor.YELLOW + "Your Listings"));
        gui.setItem(50, create(Material.ENDER_CHEST, ChatColor.YELLOW + "Collection Bin"));
        gui.setItem(51, create(Material.ANVIL, ChatColor.GREEN + "List Item"));
        gui.setItem(52, getOraxenItem("arrow_left", ChatColor.YELLOW + "Prev"));
        gui.setItem(53, getOraxenItem("arrow_right", ChatColor.YELLOW + "Next"));

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
