package me.nakilex.levelplugin.pet.utils;

import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.pet.PetDefinition;
import me.nakilex.levelplugin.pet.PetEffectDefinition;
import me.nakilex.levelplugin.pet.utils.PetDisplayUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.GuiUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class PetGuiUtil {
    private PetGuiUtil() {
    }

    public static ItemStack createPetIcon(PetDefinition definition, int level,
                                          int currentXp,
                                          int tier,
                                          List<PetEffectDefinition> effects,
                                          int copies,
                                          boolean equipped) {
        List<String> lore = PetTooltipUtil.buildPetLore(definition, level, currentXp, tier, effects);
        lore.add(" ");
        lore.add("§7Copies: §f" + copies);
        lore.add(TooltipUtil.selectionLine(equipped, equipped ? "Equipped" : "Select to equip"));
        lore.addAll(TooltipUtil.clickInstructions(equipped ? "to unequip" : "to equip", "to invest tier"));
        String name = PetDisplayUtil.formatDisplayName(definition);
        ItemStack item = GuiUtil.createGuiItem(Material.ARMOR_STAND, name, lore);
        ItemUtil.applyRarityTooltipStyle(item, definition.rarity());
        TooltipUtil.centerItemName(item);
        return item;
    }
}
