package me.nakilex.levelplugin.auctionhouse;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Represents a single listing in the auction house.
 */
public class AuctionItem {
    private final UUID seller;
    private final ItemStack item;
    private final int price;
    private final long timestamp;

    public AuctionItem(UUID seller, ItemStack item, int price) {
        this.seller = seller;
        this.item = item;
        this.price = price;
        this.timestamp = System.currentTimeMillis();
    }

    public UUID getSeller() {
        return seller;
    }

    public ItemStack getItem() {
        return item;
    }

    public int getPrice() {
        return price;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
