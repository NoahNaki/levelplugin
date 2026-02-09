package me.nakilex.levelplugin.pet.utils;

import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.pet.PetDefinition;
import me.nakilex.levelplugin.pet.PetEffectDefinition;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.StatType;
import me.nakilex.levelplugin.utils.TooltipUtil;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class PetTooltipUtil {
    private PetTooltipUtil() {
    }

    public static List<String> buildPetLore(PetDefinition definition, int level,
                                            Map<StatType, Integer> stats,
                                            List<PetEffectDefinition> effects) {
        List<String> lore = new ArrayList<>();
        lore.add(TooltipUtil.rarityLine(definition.rarity()));
        lore.add(ChatColor.GRAY + "Level " + ChatColor.WHITE + level);
        lore.add(" ");
        if (!stats.isEmpty()) {
            lore.add(TooltipUtil.sectionHeader("Stat Bonuses"));
            for (StatType type : StatType.DISPLAY_ORDER) {
                Integer value = stats.get(type);
                if (value != null && value != 0) {
                    lore.add(TooltipUtil.statLine(type.getDisplayName(), "+" + value, ChatColor.GREEN));
                }
            }
            lore.add(" ");
        }
        if (!effects.isEmpty()) {
            lore.add(TooltipUtil.sectionHeader("Effects"));
            for (PetEffectDefinition effect : effects) {
                lore.add(ChatColor.GRAY + "• " + ChatColor.WHITE + formatEffect(effect));
            }
            lore.add(" ");
        }
        lore.addAll(TooltipUtil.bulletList("Cosmetic companion", "Grants passive bonuses"));
        return lore;
    }

    public static String formatRarityName(ItemRarity rarity) {
        if (rarity == null) {
            rarity = ItemRarity.COMMON;
        }
        String name = rarity.name().charAt(0) + rarity.name().substring(1).toLowerCase();
        return "" + rarity.getColor() + name;
    }

    private static String formatEffect(PetEffectDefinition effect) {
        if (effect == null || effect.type() == null) {
            return "Unknown";
        }
        int level = effect.baseAmplifier() + 1;
        return effect.type().getName() + " " + level;
    }
}
