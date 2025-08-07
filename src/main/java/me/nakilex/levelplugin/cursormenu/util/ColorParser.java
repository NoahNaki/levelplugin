package me.nakilex.levelplugin.cursormenu.util;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.md_5.bungee.api.ChatColor;

/**
 * Converts MiniMessage or legacy '&' color codes into Bukkit's legacy
 * section format. This class is intentionally simple so it can be reused
 * anywhere in the plugin that requires flexible color parsing.
 */
public final class ColorParser {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private ColorParser() {}

    public static String parse(String input) {
        if (input == null) return "";
        // First try MiniMessage, falling back to legacy codes
        try {
            return ChatColor.translateAlternateColorCodes('&',
                    MINI_MESSAGE.deserialize(input).toLegacy());
        } catch (Exception e) {
            return ChatColor.translateAlternateColorCodes('&', input);
        }
    }
}
