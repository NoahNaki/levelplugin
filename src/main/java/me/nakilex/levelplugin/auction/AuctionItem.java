package me.nakilex.levelplugin.auction;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Simple data holder for an auction listing.
 */
public class AuctionItem {
    private final UUID id;
    private final UUID owner;
    private final String ownerName;
    private final ItemStack item;
    private final int price;
    private final long timestamp;

    public AuctionItem(UUID id, UUID owner, String ownerName, ItemStack item, int price, long timestamp) {
        this.id = id;
        this.owner = owner;
        this.ownerName = ownerName;
        this.item = item;
        this.price = price;
        this.timestamp = timestamp;
    }

    public UUID getId() { return id; }
    public UUID getOwner() { return owner; }
    public String getOwnerName() { return ownerName; }
    public ItemStack getItem() { return item; }
    public int getPrice() { return price; }
    public long getTimestamp() { return timestamp; }
}
