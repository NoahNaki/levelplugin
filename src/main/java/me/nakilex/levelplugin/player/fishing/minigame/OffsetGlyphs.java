package me.nakilex.levelplugin.player.fishing.minigame;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

/** Builds compact pixel-spacing components backed by the customfishing offset font. */
public final class OffsetGlyphs {
    public static final Key FONT = Key.key("customfishing:offset_chars");
    private static final int[] STEPS = {128, 64, 32, 16, 8, 4, 2, 1};

    private OffsetGlyphs() { }

    public static Component component(int pixels) { return Component.text(characters(pixels)).font(FONT); }

    public static String characters(int pixels) {
        if (pixels == 0) return "";
        boolean positive = pixels > 0;
        int remaining = Math.abs(pixels);
        StringBuilder output = new StringBuilder();
        for (int step : STEPS) {
            while (remaining >= step) {
                int power = Integer.numberOfTrailingZeros(step);
                output.append((char) ((positive ? 0xF810 : 0xF800) + power));
                remaining -= step;
            }
        }
        return output.toString();
    }
}
