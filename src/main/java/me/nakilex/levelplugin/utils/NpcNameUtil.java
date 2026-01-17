package me.nakilex.levelplugin.utils;

import org.bukkit.ChatColor;

import java.util.Locale;

/**
 * Utility helpers for normalizing and comparing NPC names regardless of color codes or spacing.
 */
public final class NpcNameUtil {
    private NpcNameUtil() {
    }

    public static String normalize(String npcName) {
        if (npcName == null) {
            return null;
        }
        String stripped = ChatColor.stripColor(npcName);
        if (stripped == null) {
            return null;
        }
        String trimmed = stripped.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        String collapsedWhitespace = trimmed.replaceAll("\\s+", " ");
        return collapsedWhitespace.toLowerCase(Locale.ROOT);
    }

    public static boolean equalsNormalized(String npcName, String expectedName) {
        String normalizedActual = normalize(npcName);
        String normalizedExpected = normalize(expectedName);
        return normalizedActual != null
                && normalizedExpected != null
                && normalizedActual.equals(normalizedExpected);
    }
}
