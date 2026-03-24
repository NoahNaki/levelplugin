package me.nakilex.levelplugin.spells;

import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.data.WeaponType;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.tools.CustomTool;
import me.nakilex.levelplugin.items.tools.ToolManager;
import me.nakilex.levelplugin.player.classes.data.ClassUtil;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.classes.managers.PlayerClassManager;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class SpellAccessUtil {
    private SpellAccessUtil() {
    }


    public static boolean isHoldingWeapon(Player player) {
        if (player == null) {
            return false;
        }
        return WeaponType.matchType(player.getInventory().getItemInMainHand()) != null;
    }

    public static boolean isHoldingLifeSkillTool(Player player) {
        if (player == null) {
            return false;
        }
        CustomTool tool = ToolManager.getInstance().getTool(player.getInventory().getItemInMainHand(), false);
        return tool != null;
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

    public static String getHeldWeaponRequirementFailure(Player player) {
        if (player == null) {
            return "You cannot cast this skill right now.";
        }
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        CustomItem customItem = ItemManager.getInstance().getCustomItemFromItemStack(mainHand);
        if (customItem == null) {
            return null;
        }
        if (customItem.isBroken()) {
            return customItem.getBaseName() + " is broken and cannot cast skills.";
        }
        PlayerClass playerClass = PlayerClassManager.getInstance().getPlayerClass(player);
        PlayerClass required = PlayerClass.fromString(customItem.getClassRequirement());
        if (!ClassUtil.meetsRequirement(playerClass, required)) {
            return "You are not the right class to cast skills with " + customItem.getBaseName() + ".";
        }
        int playerLevel = LevelManager.getInstance().getLevel(player);
        if (playerLevel < customItem.getLevelRequirement()) {
            return "You need level " + customItem.getLevelRequirement()
                    + " to cast skills with " + customItem.getBaseName() + ".";
        }
        return null;
    }
}
