package me.nakilex.levelplugin.merchants.data;

import me.nakilex.levelplugin.items.tools.CustomTool;

public class MerchantItem {
    private final int slot;
    private final int itemId;
    private final CustomTool tool;
    private final int amount;
    private final int cost;
    private final int gems;       // ← new field
    private final int accountLimit;

    public MerchantItem(int slot, int itemId, int amount, int cost, int gems) {
        this(slot, itemId, null, amount, cost, gems, 0);
    }

    public MerchantItem(int slot, CustomTool tool, int amount, int cost, int gems) {
        this(slot, -1, tool, amount, cost, gems, 0);
    }

    public MerchantItem(int slot, int itemId, int amount, int cost, int gems, int accountLimit) {
        this(slot, itemId, null, amount, cost, gems, accountLimit);
    }

    public MerchantItem(int slot, CustomTool tool, int amount, int cost, int gems, int accountLimit) {
        this(slot, -1, tool, amount, cost, gems, accountLimit);
    }

    private MerchantItem(int slot, int itemId, CustomTool tool, int amount, int cost, int gems, int accountLimit) {
        this.slot = slot;
        this.itemId = itemId;
        this.tool = tool;
        this.amount = amount;
        this.cost = cost;
        this.gems = gems;
        this.accountLimit = accountLimit;
    }

    public int getSlot()        { return slot; }
    public int getItemId()      { return itemId; }
    public int getAmount()      { return amount; }
    public int getCost()        { return cost; }
    public int getGems()        { return gems; }  // ← new getter
    public int getAccountLimit() { return accountLimit; }
    public boolean isTool()     { return tool != null; }
    public CustomTool getTool() { return tool; }
}
