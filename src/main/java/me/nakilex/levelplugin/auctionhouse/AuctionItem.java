package me.nakilex.levelplugin.auctionhouse;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Represents a single listing in the auction house.
 */
public class AuctionItem {
    private final UUID seller;
    private final ItemStack item;
    private final int startingPrice;
    private final int binPrice;
    private int currentBid;
    private UUID highestBidder;
    private final long startTime;
    private final long endTime;
    private AuctionStatus status;
    private final int listingTax;
    private final AuctionCategory category;

    public AuctionItem(UUID seller, ItemStack item, int startingPrice, int binPrice, long durationHours, int listingTax) {
        this.seller = seller;
        this.item = item;
        this.startingPrice = startingPrice;
        this.binPrice = binPrice;
        this.currentBid = 0;
        this.highestBidder = null;
        this.startTime = System.currentTimeMillis();
        this.endTime = startTime + (durationHours * 3600000L);
        this.status = AuctionStatus.ACTIVE;
        this.listingTax = listingTax;
        this.category = AuctionCategory.fromItem(item);
    }

    public UUID getSeller() {
        return seller;
    }

    public ItemStack getItem() {
        return item;
    }

    public int getStartingPrice() {
        return startingPrice;
    }

    public int getBinPrice() {
        return binPrice;
    }

    public int getCurrentBid() {
        return currentBid;
    }

    public void setCurrentBid(int currentBid) {
        this.currentBid = currentBid;
    }

    public UUID getHighestBidder() {
        return highestBidder;
    }

    public void setHighestBidder(UUID highestBidder) {
        this.highestBidder = highestBidder;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    public AuctionCategory getCategory() {
        return category;
    }

    public int getListingTax() {
        return listingTax;
    }
}
