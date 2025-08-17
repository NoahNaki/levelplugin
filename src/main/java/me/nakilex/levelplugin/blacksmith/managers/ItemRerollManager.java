package me.nakilex.levelplugin.blacksmith.managers;

import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.StatType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ItemRerollManager {

    private static final int BASE_COST = 100;

    /**
     * Reroll a specific stat on the given item based on its range.
     * The ItemStack is updated in-place to reflect the new value.
     */
    /**
     * Reroll a stat and return the difference between the new and old value.
     * A negative return value indicates the stat decreased.
     */
    public int rerollStat(Player player, ItemStack stack, CustomItem item, StatType stat) {
        if (item == null || stack == null) return 0;

        int before = getValue(item, stat);

        switch (stat) {
            case STR -> item.setBaseStr(item.getStrRange().roll());
            case INT -> item.setBaseIntel(item.getIntelRange().roll());
            case AGI -> item.setBaseAgi(item.getAgiRange().roll());
            case DEX -> item.setBaseDex(item.getDexRange().roll());
            case VIT -> {
                if (item.getHpRange().getMax() > 0) {
                    item.setBaseHp(item.getHpRange().roll());
                }
                if (item.getDefRange().getMax() > 0) {
                    item.setBaseDef(item.getDefRange().roll());
                }
            }
            case WIL -> {
                // Items currently have no inherent Will range; nothing to reroll.
            }
        }

        ItemStack updated = ItemUtil.createItemStackFromCustomItem(item, stack.getAmount(), player);
        ItemUtil.applyUpdatedStack(stack, updated);

        int after = getValue(item, stat);
        return after - before;
    }

    /**
     * Calculate the coin cost to reroll a single stat based on item rarity.
     */
    public int getRerollCost(CustomItem item) {
        if (item == null) return BASE_COST;
        int rarityMultiplier = item.getRarity().ordinal() + 1;
        return BASE_COST * rarityMultiplier;
    }

    /**
     * Check if the item actually has the given stat available to reroll.
     */
    public boolean hasStat(CustomItem item, StatType stat) {
        if (item == null) return false;
        return switch (stat) {
            case STR -> item.getStrRange().getMax() > 0;
            case INT -> item.getIntelRange().getMax() > 0;
            case AGI -> item.getAgiRange().getMax() > 0;
            case DEX -> item.getDexRange().getMax() > 0;
            case VIT -> item.getHpRange().getMax() > 0 || item.getDefRange().getMax() > 0;
            case WIL -> false;
        };
    }

    private int getValue(CustomItem item, StatType stat) {
        return switch (stat) {
            case STR -> item.getStr();
            case INT -> item.getIntel();
            case AGI -> item.getAgi();
            case DEX -> item.getDex();
            case VIT -> item.getHp() + item.getDef();
            case WIL -> item.getWil();
        };
    }
}
