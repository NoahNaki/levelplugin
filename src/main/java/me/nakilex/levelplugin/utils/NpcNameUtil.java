package me.nakilex.levelplugin.utils;

import org.bukkit.ChatColor;

import java.util.Locale;

/**
 * Utility helpers for normalizing and comparing NPC names regardless of color codes or spacing.
 */
public final class NpcNameUtil {
    /**
     * Leading "[NPC]" badge, as it appears once a name has been colour-stripped and lowercased.
     * Citizens can only show one line of text on the nameplate, so labelled NPCs carry the badge
     * in their actual name; matching has to look past it to find the plain name underneath.
     */
    private static final java.util.regex.Pattern NPC_BADGE_PREFIX =
            java.util.regex.Pattern.compile("^\\[\\s*npc\\s*]\\s*");

    private NpcNameUtil() {
    }

    /**
     * Drop a leading "[NPC]" badge from an already-normalized name.
     *
     * @param normalizedName colour-stripped, lowercased name
     * @return the name without its badge prefix
     */
    public static String stripBadgePrefix(String normalizedName) {
        if (normalizedName == null) {
            return null;
        }
        return NPC_BADGE_PREFIX.matcher(normalizedName).replaceFirst("").trim();
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
        return stripBadgePrefix(collapsedWhitespace.toLowerCase(Locale.ROOT));
    }

    public static boolean equalsNormalized(String npcName, String expectedName) {
        String normalizedActual = normalize(npcName);
        String normalizedExpected = normalize(expectedName);
        return normalizedActual != null
                && normalizedExpected != null
                && normalizedActual.equals(normalizedExpected);
    }
}
