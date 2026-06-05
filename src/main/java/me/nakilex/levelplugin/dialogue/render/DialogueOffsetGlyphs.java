package me.nakilex.levelplugin.dialogue.render;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

/**
 * Pixel offset glyph helpers from the levelplugin_dialogue:offset_chars font.
 */
public final class DialogueOffsetGlyphs {
    public static final Key OFFSET_FONT = Key.key("levelplugin_dialogue", "offset_chars");

    public static final String NEGATIVE_ONE_PIXEL = "\uF800";
    public static final String POSITIVE_ONE_PIXEL = "\uF801";

    private DialogueOffsetGlyphs() {
    }

    public static String pixels(int pixels) {
        if (pixels == 0) {
            return "";
        }
        String glyph = pixels < 0 ? NEGATIVE_ONE_PIXEL : POSITIVE_ONE_PIXEL;
        return glyph.repeat(Math.abs(pixels));
    }

    public static Component component(int pixels) {
        if (pixels == 0) {
            return Component.empty();
        }
        return Component.text(pixels(pixels)).font(OFFSET_FONT);
    }
}
