package me.nakilex.levelplugin.luxbridge.util;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

public final class LuxBridgeFormat {
    private LuxBridgeFormat() {}

    public static String placeholders(Player player, String text) {
        if (text == null) return "";
        return PlaceholderAPI.setPlaceholders(player, PlaceholderAPI.setBracketPlaceholders(player, text));
    }

    public static String miniMessageText(String text) {
        if (text == null) return "";
        return text.replace("&", "§");
    }

    public static String color(String color, String fallback) {
        if (color == null || color.isBlank()) return fallback;
        String c = color.trim();
        if (c.startsWith("#") && c.length() == 7) return c;
        if (c.startsWith("<#") && c.endsWith(">")) return c.substring(1, c.length() - 1);
        return fallback;
    }

    public static String stripMini(String text) {
        if (text == null) return "";
        return text.replaceAll("<[^>]+>", "").replaceAll("[§&][0-9A-FK-ORa-fk-or]", "");
    }
}
