package me.nakilex.levelplugin.pet.utils;

import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.pet.PetDefinition;
import me.nakilex.levelplugin.pet.PetEffectDefinition;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.StatType;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.GuiUtil;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PetGuiUtil {
    private PetGuiUtil() {
    }

    public static ItemStack createPetIcon(PetDefinition definition, int level,
                                          int currentXp,
                                          int tier,
                                          Map<StatType, Integer> ownershipStats,
                                          List<PetEffectDefinition> effects,
                                          int copies,
                                          boolean equipped) {
        List<String> lore = PetTooltipUtil.buildPetLore(definition, level, currentXp, tier, ownershipStats, effects);
        lore.add(" ");
        lore.add("§7Copies: §f" + copies);
        lore.add(TooltipUtil.selectionLine(equipped, equipped ? "Equipped" : "Select to equip"));
        lore.addAll(TooltipUtil.clickInstructions(equipped ? "to unequip" : "to equip", "to invest tier"));
        String name = PetDisplayUtil.formatDisplayName(definition);
        ItemStack item = createDefinitionIcon(definition, name, lore);
        ItemUtil.applyRarityTooltipStyle(item, definition.rarity());
        ItemUtil.setVisualEnchanted(item, equipped);
        TooltipUtil.centerItemName(item);
        return item;
    }

    public static ItemStack createRarityPetIcon(PetDefinition definition, String name, List<String> lore) {
        ItemStack item = createDefinitionIcon(definition, name, lore);
        ItemUtil.applyRarityTooltipStyle(item, definition.rarity());
        TooltipUtil.centerItemName(item);
        return item;
    }

    private static ItemStack createDefinitionIcon(PetDefinition definition, String name, List<String> lore) {
        for (String modelId : definition.modelIds()) {
            String nexoIconId = cubeesIconId(modelId);
            ItemStack icon = GuiUtil.getNexoItemIfPresent(nexoIconId, name, lore);
            if (icon != null) {
                return icon;
            }
        }
        return GuiUtil.getRarityPetIconItem(definition.rarity(), name, lore);
    }

    private static String cubeesIconId(String modelId) {
        if (modelId == null) {
            return null;
        }
        String normalized = modelId.trim().toLowerCase(Locale.ROOT);
        if (!normalized.startsWith("cubee-") || normalized.length() == "cubee-".length()) {
            return null;
        }
        return "cubees-icon-" + normalized.substring("cubee-".length());
    }
}
