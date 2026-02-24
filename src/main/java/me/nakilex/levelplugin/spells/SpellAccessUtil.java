package me.nakilex.levelplugin.spells;

import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.player.classes.data.ClassUtil;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.classes.managers.PlayerClassManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class SpellAccessUtil {
    private SpellAccessUtil() {
    }

    public static boolean isHoldingValidClassWeapon(Player player) {
        if (player == null) {
            return false;
        }
        PlayerClass playerClass = PlayerClassManager.getInstance().getPlayerClass(player);
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        CustomItem customItem = ItemManager.getInstance().getCustomItemFromItemStack(mainHand);
        PlayerClass required = customItem == null ? null : PlayerClass.fromString(customItem.getClassRequirement());
        return ClassUtil.canUseWeapon(playerClass, mainHand, required);
    }
}

