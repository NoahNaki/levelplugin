package me.nakilex.levelplugin.auctionhouse;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Represents a single auction listing.
 */
public class AuctionListing {
    private final int id;
    private final UUID seller;
    private final ItemStack item;
    private final double price;
    private final long expireAt;
    private boolean active;

    public AuctionListing(int id, UUID seller, ItemStack item, double price, long expireAt, boolean active) {
        this.id = id;
        this.seller = seller;
        this.item = item;
        this.price = price;
        this.expireAt = expireAt;
        this.active = active;
    }

    public int getId() {
        return id;
    }

    public UUID getSeller() {
        return seller;
    }

    public ItemStack getItem() {
        return item;
    }

    public double getPrice() {
        return price;
    }

    public long getExpireAt() {
        return expireAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
