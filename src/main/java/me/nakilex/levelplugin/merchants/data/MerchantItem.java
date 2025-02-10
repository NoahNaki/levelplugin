package me.nakilex.levelplugin.merchants.data;

public class MerchantItem {
    private final int slot;
    private final int itemId;
    private final int amount;
    private final int cost;

    public MerchantItem(int slot, int itemId, int amount, int cost) {
        this.slot = slot;
        this.itemId = itemId;
        this.amount = amount;
        this.cost = cost;
    }

    public int getSlot() {
        return slot;
    }

    public int getItemId() {
        return itemId;
    }

    public int getAmount() {
        return amount;
    }

    public int getCost() {
        return cost;
    }
}
