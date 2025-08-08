package me.nakilex.levelplugin.cursormenu.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

/**
 * Utility for translating MiniMessage or legacy colour codes into the
 * section sign based strings used by the Bukkit API. This class is kept
 * intentionally simple so it can be reused by other modules that need a
 * lightweight colour parser without pulling in additional helpers.
 */
public final class ColorParser {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private ColorParser() {}

    /**
     * Parse the provided text into a legacy coloured string. Supports both
     * MiniMessage tags and the common '&' style colour codes.
     *
     * @param input raw message
     * @return legacy coloured string safe for chat and entity displays
     */
    public static String parse(String input) {
        if (input == null || input.isEmpty()) return "";
        // First translate traditional ampersand codes so MiniMessage can handle
        // any remaining tags without interference.
        String translated = ChatColor.translateAlternateColorCodes('&', input);
        Component comp = MINI.deserialize(translated);
        return LEGACY.serialize(comp);
    }
}
