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
    public void rerollStat(Player player, ItemStack stack, CustomItem item, StatType stat) {
        if (item == null || stack == null) return;

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
    }
}
