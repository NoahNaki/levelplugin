package me.nakilex.levelplugin.pet.utils;

import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.pet.PetDefinition;
import me.nakilex.levelplugin.pet.PetEffectDefinition;
import me.nakilex.levelplugin.pet.PetProgression;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

public final class PetTooltipUtil {
    private PetTooltipUtil() {
    }

    public static List<String> buildPetLore(PetDefinition definition, int level,
                                            int currentXp,
                                            int tier,
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
        if (!effects.isEmpty()) {
            lore.add(TooltipUtil.sectionHeader("Effects"));
            for (PetEffectDefinition effect : effects) {
                lore.add(ChatColor.GRAY + "• " + ChatColor.WHITE + formatEffect(effect));
            }
            lore.add(" ");
        }
        return lore;
    }

    private static String formatEffect(PetEffectDefinition effect) {
        if (effect == null || effect.type() == null) {
            return "Unknown";
        }
        int level = effect.baseAmplifier() + 1;
        return effect.type().getName() + " " + level;
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
            return ChatColor.GRAY + TooltipUtil.progressBar(1, 1, 15) + ChatColor.GRAY + " Max";
        }
        int currentLevelXp = PetProgression.xpForLevel(level, definition.xpPerLevel());
        int nextLevelXp = PetProgression.xpForLevel(level + 1, definition.xpPerLevel());
        int span = Math.max(1, nextLevelXp - currentLevelXp);
        int progress = Math.max(0, currentXp - currentLevelXp);
        String bar = TooltipUtil.progressBar(progress, span, 15);
        return bar + " " + ChatColor.GRAY + progress + ChatColor.GOLD + "/" + ChatColor.GRAY + span
                + " <glyph:experience_orb_icon>";
    }
}
