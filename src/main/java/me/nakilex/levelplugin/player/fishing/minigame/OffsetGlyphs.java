package me.nakilex.levelplugin.player.fishing.minigame;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

/** Backwards-compatible fishing offset glyph facade backed by the shared glyph utility. */
public final class OffsetGlyphs {
    public static final Key FONT = me.nakilex.levelplugin.utils.glyph.OffsetGlyphs.DEFAULT_FONT;

    private OffsetGlyphs() { }

    public static Component component(int pixels) {
        return me.nakilex.levelplugin.utils.glyph.OffsetGlyphs.component(pixels, FONT);
    }

    public static String characters(int pixels) {
        return me.nakilex.levelplugin.utils.glyph.OffsetGlyphs.characters(pixels);
    }
}
