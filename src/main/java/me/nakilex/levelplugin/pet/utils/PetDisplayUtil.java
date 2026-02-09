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
        String raw = definition.displayName() == null || definition.displayName().isBlank()
                ? definition.id()
                : definition.displayName();
        String stripped = ChatColor.stripColor(raw);
        return "" + definition.rarity().getColor() + ChatUtil.applyEmojis(stripped);
    }
}
