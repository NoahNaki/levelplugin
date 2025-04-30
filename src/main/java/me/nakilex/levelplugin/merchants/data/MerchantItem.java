package me.nakilex.levelplugin.merchants.data;

public class MerchantItem {
    private final int slot;
    private final int itemId;
    private final int amount;
    private final int cost;
    private final int gems;       // ← new field

    public MerchantItem(int slot, int itemId, int amount, int cost, int gems) {
        this.slot = slot;
        this.itemId = itemId;
        this.amount = amount;
        this.cost = cost;
        this.gems = gems;
    }

    public int getSlot()        { return slot; }
    public int getItemId()      { return itemId; }
    public int getAmount()      { return amount; }
    public int getCost()        { return cost; }
    public int getGems()        { return gems; }  // ← new getter
}
