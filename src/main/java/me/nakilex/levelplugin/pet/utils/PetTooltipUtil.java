package me.nakilex.levelplugin.pet.utils;

import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.pet.PetDefinition;
import me.nakilex.levelplugin.pet.PetEffectDefinition;
import me.nakilex.levelplugin.pet.PetProgression;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.StatType;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class PetTooltipUtil {
    private PetTooltipUtil() {
    }

    public static List<String> buildPetLore(PetDefinition definition, int level,
                                            int currentXp,
                                            int tier,
                                            Map<StatType, Integer> ownershipStats,
                                            List<PetEffectDefinition> effects) {
        List<String> lore = new ArrayList<>();
        lore.add(rarityLine(definition.rarity()));
        lore.add(" ");
        lore.add(tierLine(tier));
        lore.add(" ");
        lore.add(ChatColor.GRAY + "Level " + ChatColor.WHITE + level);
        lore.add(progressLine(definition, level, currentXp));
        lore.add(progressBarLine(definition, level, currentXp));

        lore.add(" ");
        appendOwnershipSection(lore, ownershipStats);
        appendEffectSection(lore, effects);
        return lore;
    }

    private static void appendOwnershipSection(List<String> lore, Map<StatType, Integer> ownershipStats) {
        if (ownershipStats == null || ownershipStats.isEmpty()) {
            return;
        }
        lore.add(TooltipUtil.sectionHeader("Ownership Effect"));
        for (StatType stat : StatType.DISPLAY_ORDER) {
            int value = ownershipStats.getOrDefault(stat, 0);
            if (value == 0) {
                continue;
            }
            lore.add(TooltipUtil.bulletLine(GuiUtil.formatStatLine(stat, value, false)));
        }
        lore.add(" ");
    }

    private static void appendEffectSection(List<String> lore, List<PetEffectDefinition> effects) {
        if (effects == null || effects.isEmpty()) {
            return;
        }
        lore.add(TooltipUtil.sectionHeader("Equipped Effect"));
        for (PetEffectDefinition effect : effects) {
            String line = TooltipUtil.bulletLine(ChatColor.WHITE + formatEffect(effect));
            lore.addAll(TooltipUtil.wrapLoreLine(line, 220, ChatColor.DARK_GRAY + "  " + ChatColor.GRAY));
        }
        lore.add(" ");
    }

    private static String formatEffect(PetEffectDefinition effect) {
        if (effect == null || effect.type() == null) {
            return "Unknown";
        }
        return effect.type().displayName() + ": " + effect.type().formatDescription(effect.baseValue());
    }

    private static String rarityLine(ItemRarity rarity) {
        if (rarity == null) {
            rarity = ItemRarity.COMMON;
        }
        return rarity.getColor() + rarity.getSymbol() + "<glyph:pet>";
    }

    private static String tierLine(int tier) {
        int safeTier = Math.max(1, Math.min(5, tier));
        String filled = GuiUtil.glyphStars(safeTier);
        return ChatColor.GOLD + "TIER " + ChatColor.YELLOW + filled;
    }

    private static String progressLine(PetDefinition definition, int level, int currentXp) {
        if (level >= definition.maxLevel()) {
            return ChatColor.GRAY + "Max level reached.";
        }
        int nextLevel = level + 1;
        int currentLevelXp = PetProgression.xpForLevel(level, definition.xpPerLevel());
        int nextLevelXp = PetProgression.xpForLevel(nextLevel, definition.xpPerLevel());
        int span = Math.max(1, nextLevelXp - currentLevelXp);
        int progress = Math.max(0, currentXp - currentLevelXp);
        double percent = Math.min(100.0, Math.round((progress * 10000.0 / span)) / 100.0);
        return ChatColor.GRAY + "Progress to Level " + ChatColor.YELLOW + nextLevel + ChatColor.GRAY + ": "
                + ChatColor.YELLOW + String.format("%.2f", percent) + "%";
    }

    private static String progressBarLine(PetDefinition definition, int level, int currentXp) {
        if (level >= definition.maxLevel()) {
            return ChatColor.GRAY + TooltipUtil.expProgressBarByPixels(1, 1, 168) + ChatColor.GRAY + " Max";
        }
        int currentLevelXp = PetProgression.xpForLevel(level, definition.xpPerLevel());
        int nextLevelXp = PetProgression.xpForLevel(level + 1, definition.xpPerLevel());
        int span = Math.max(1, nextLevelXp - currentLevelXp);
        int progress = Math.max(0, currentXp - currentLevelXp);
        String bar = TooltipUtil.expProgressBarByPixels(progress, span, 168);
        return bar + " " + ChatColor.GRAY + progress + ChatColor.GOLD + "/" + ChatColor.GRAY + span
                + " <glyph:experience_orb_icon>";
    }
}
