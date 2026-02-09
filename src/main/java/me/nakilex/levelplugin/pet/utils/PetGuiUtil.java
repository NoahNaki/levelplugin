package me.nakilex.levelplugin.pet.utils;

import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.pet.PetDefinition;
import me.nakilex.levelplugin.pet.PetEffectDefinition;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.StatType;
import me.nakilex.levelplugin.utils.ChatUtil;
import me.nakilex.levelplugin.utils.GuiUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public final class PetGuiUtil {
    private PetGuiUtil() {
    }

    public static ItemStack createPetIcon(PetDefinition definition, int level,
                                          Map<StatType, Integer> stats,
                                          List<PetEffectDefinition> effects) {
        List<String> lore = PetTooltipUtil.buildPetLore(definition, level, stats, effects);
        String name = ChatUtil.applyEmojis(definition.displayName());
        ItemStack item = GuiUtil.createGuiItem(Material.ARMOR_STAND, name, lore);
        ItemUtil.applyRarityTooltipStyle(item, definition.rarity());
        return item;
    }
}
