package me.nakilex.levelplugin.screen.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Utility to convert MiniMessage or legacy ampersand colour codes into
 * section symbol strings. This keeps the menu configuration human readable
 * while allowing vanilla clients to display coloured text.
 */
public final class ColorParser {
    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacySection();

    private ColorParser() {}

    public static String parse(String input) {
        if (input == null || input.isEmpty()) return "";
        if (input.indexOf('<') >= 0 && input.indexOf('>') > input.indexOf('<')) {
            Component comp = MINI.deserialize(input);
            return LEGACY.serialize(comp);
        }
        return input.replace('&', '§');
    }
}
