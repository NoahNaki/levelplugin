package me.nakilex.levelplugin.blacksmith.managers;

import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.StatType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ItemRerollManager {

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
            case HP  -> item.setBaseHp(item.getHpRange().roll());
            case DEF -> item.setBaseDef(item.getDefRange().roll());
        }

        ItemStack updated = ItemUtil.createItemStackFromCustomItem(item, stack.getAmount(), player);
        stack.setType(updated.getType());
        stack.setItemMeta(updated.getItemMeta());

        int after = getValue(item, stat);
        return after - before;
    }

    private int getValue(CustomItem item, StatType stat) {
        return switch (stat) {
            case STR -> item.getStr();
            case INT -> item.getIntel();
            case AGI -> item.getAgi();
            case DEX -> item.getDex();
            case HP  -> item.getHp();
            case DEF -> item.getDef();
        };
    }
}
