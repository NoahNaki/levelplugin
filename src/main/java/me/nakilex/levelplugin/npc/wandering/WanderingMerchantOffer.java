package me.nakilex.levelplugin.npc.wandering;

import org.bukkit.inventory.ItemStack;

/** Simple record of an item, cost and quantity for the wandering merchant. */
public class WanderingMerchantOffer {
    private final ItemStack item;
    private final int cost;
    private final boolean featured;
    private int stock;

    public WanderingMerchantOffer(ItemStack item, int cost, int stock) {
        this(item, cost, stock, false);
    }

    public WanderingMerchantOffer(ItemStack item, int cost, int stock, boolean featured) {
        this.item = item;
        this.cost = cost;
        this.stock = stock;
        this.featured = featured;
    }

    public ItemStack getItem() { return item; }
    public int getCost() { return cost; }
    public boolean isFeatured() { return featured; }
    public int getStock() { return stock; }
    public void decrement() { if (stock > 0) stock--; }
}
