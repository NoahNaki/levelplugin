package me.nakilex.levelplugin.quests.dialogue.hud;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/** Approximate vanilla-font pixel width helper for action-bar HUD cursor resets. */
public final class DialogueTextWidth {
    private DialogueTextWidth() {
    }

    public static int width(Component component) {
        String plain = PlainTextComponentSerializer.plainText().serialize(component == null ? Component.empty() : component);
        return width(plain);
    }

    public static int width(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        int width = 0;

        for (char c : text.toCharArray()) {
            width += charWidth(c);
        }

        return width;
    }

    private static int charWidth(char c) {
        if (c == ' ') return 4;
        if ("il.,'!:;|".indexOf(c) >= 0) return 2;
        if ("mwMW@#".indexOf(c) >= 0) return 7;
        return 6;
    }
}
