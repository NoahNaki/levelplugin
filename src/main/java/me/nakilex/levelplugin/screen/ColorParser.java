package me.nakilex.levelplugin.screen;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

/**
 * Utility for converting MiniMessage or ampersand coded strings into
 * legacy section-colour strings.
 */
public final class ColorParser {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private ColorParser() {
    }

    /**
     * Parse a MiniMessage formatted string to legacy section codes.
     */
    public static String fromMiniMessage(String input) {
        Component c = MINI.deserialize(input);
        return LEGACY.serialize(c);
    }

    /**
     * Translate ampersand based colour codes to legacy section codes.
     */
    public static String fromAmpersand(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}
