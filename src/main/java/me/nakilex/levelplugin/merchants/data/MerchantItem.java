package me.nakilex.levelplugin.merchants.data;

import me.nakilex.levelplugin.items.tools.CustomTool;

public class MerchantItem {
    private final int slot;
    private final int itemId;
    private final CustomTool tool;
    private final int amount;
    private final int cost;
    private final int gems;       // ← new field

    public MerchantItem(int slot, int itemId, int amount, int cost, int gems) {
        this(slot, itemId, null, amount, cost, gems);
    }

    public MerchantItem(int slot, CustomTool tool, int amount, int cost, int gems) {
        this(slot, -1, tool, amount, cost, gems);
    }

    private MerchantItem(int slot, int itemId, CustomTool tool, int amount, int cost, int gems) {
        this.slot = slot;
        this.itemId = itemId;
        this.tool = tool;
        this.amount = amount;
        this.cost = cost;
        this.gems = gems;
    }

    public int getSlot()        { return slot; }
    public int getItemId()      { return itemId; }
    public int getAmount()      { return amount; }
    public int getCost()        { return cost; }
    public int getGems()        { return gems; }  // ← new getter
    public boolean isTool()     { return tool != null; }
    public CustomTool getTool() { return tool; }
}
