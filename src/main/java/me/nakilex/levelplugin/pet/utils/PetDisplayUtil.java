package me.nakilex.levelplugin.pet.utils;

import me.nakilex.levelplugin.pet.PetDefinition;
import me.nakilex.levelplugin.utils.ChatUtil;
import org.bukkit.ChatColor;

public final class PetDisplayUtil {
    private PetDisplayUtil() {
    }

    public static String formatDisplayName(PetDefinition definition) {
        if (definition == null) {
            return "";
        }
        String stripped = getStrippedPetName(definition);
        return "" + definition.rarity().getColor() + ChatUtil.applyEmojis(stripped);
    }

    public static String formatSummonedName(String ownerName, PetDefinition definition, int level) {
        if (definition == null) {
            return "";
        }
        String safeOwner = ownerName == null || ownerName.isBlank() ? "Unknown" : ownerName;
        int safeLevel = Math.max(1, level);
        String petName = getStrippedPetName(definition);
        ChatColor rarityColor = definition.rarity().getColor();
        return ChatColor.DARK_GRAY + "[" + ChatColor.GRAY + "Lv" + safeLevel + ChatColor.DARK_GRAY + "] "
                + rarityColor + ChatUtil.applyEmojis(safeOwner + "'s " + petName);
    }

    private static String getStrippedPetName(PetDefinition definition) {
        String raw = definition.displayName() == null || definition.displayName().isBlank()
                ? definition.id()
                : definition.displayName();
        return ChatColor.stripColor(raw);
    }
}
