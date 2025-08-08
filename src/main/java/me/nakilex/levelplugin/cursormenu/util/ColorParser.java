package me.nakilex.levelplugin.cursormenu.util;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/** Utility for converting MiniMessage or ampersand codes to legacy strings. */
public final class ColorParser {
    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private ColorParser() {}

    public static String parse(String input) {
        if (input == null) return "";
        if (input.contains("<")) {
            return LEGACY.serialize(MINI.deserialize(input));
        }
        return LEGACY.serialize(LEGACY.deserialize(input.replace('&', '§')));
    }
}
