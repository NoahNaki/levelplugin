package me.nakilex.levelplugin.player.fishing.minigame;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

import java.util.Map;

/** Builds compact pixel-spacing components backed by the customfishing offset font. */
public final class OffsetGlyphs {
    public static final Key FONT = Key.key("customfishing:offset_chars");
    private static final int[] STEPS = {128, 64, 32, 16, 8, 4, 2, 1};
    private static final Map<Integer, Character> NEGATIVE = Map.of(
            1, '\uF801', 2, '\uF802', 4, '\uF803', 8, '\uF804',
            16, '\uF805', 32, '\uF806', 64, '\uF807', 128, '\uF808');
    private static final Map<Integer, Character> POSITIVE = Map.of(
            1, '\uF811', 2, '\uF812', 4, '\uF813', 8, '\uF814',
            16, '\uF815', 32, '\uF816', 64, '\uF817', 128, '\uF818');

    private OffsetGlyphs() { }

    public static Component component(int pixels) { return Component.text(characters(pixels)).font(FONT); }

    public static String characters(int pixels) {
        if (pixels == 0) return "";
        Map<Integer, Character> glyphs = pixels > 0 ? POSITIVE : NEGATIVE;
        int remaining = Math.abs(pixels);
        StringBuilder output = new StringBuilder();
        for (int step : STEPS) {
            while (remaining >= step) {
                output.append(glyphs.get(step));
                remaining -= step;
            }
        }
        return output.toString();
    }
}
