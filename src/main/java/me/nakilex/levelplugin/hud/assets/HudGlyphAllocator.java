package me.nakilex.levelplugin.hud.assets;

public class HudGlyphAllocator {
    private static final int PUA_START = 0xE000;
    private static final int PUA_END = 0xF8FF;
    private int next = PUA_START;

    public char next() {
        if (next > PUA_END) {
            throw new IllegalStateException("HUD glyph space exhausted.");
        }
        return (char) next++;
    }
}
