package me.nakilex.levelplugin.player.fishing.minigame;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

/** Glyph catalogue and reusable CustomFishing-style subtitle layers for fishing mini-games. */
public final class FishingGlyphs {
    public static final Key DEFAULT_FONT = Key.key("customfishing:default");
    public static final Key ICONS_FONT = Key.key("customfishing:icons");
    public static final int HOLD_EFFECTIVE_WIDTH = 155;
    public static final int HOLD_JUDGEMENT_OFFSET = -160;
    public static final int HOLD_POINTER_WIDTH = 5;
    public static final int HOLD_JUDGEMENT_EASY_WIDTH = 45;
    public static final int HOLD_JUDGEMENT_NORMAL_WIDTH = 36;
    public static final int HOLD_JUDGEMENT_HARD_WIDTH = 27;

    public static final int TENSION_EFFECTIVE_WIDTH = 218;
    public static final int TENSION_FISH_OFFSET = -221;
    public static final int TENSION_FISH_WIDTH = 8;

    public static final int ACCURATE_POINTER_WIDTH = 5;

    public static final char POINTER = '\uB001';
    public static final char BAR_1 = '\uB002';
    public static final char BAR_2 = '\uB003';
    public static final char BAR_3 = '\uB004';
    public static final char BAR_4 = '\uB005';
    public static final char BAR_5 = '\uB006';
    public static final char BAR_6 = '\uB007';
    public static final char BAR_7 = '\uB008';
    public static final char BAR_8 = '\uB009';
    public static final char BAR_9 = '\uB00A';
    public static final char RAINBOW_BAR = '\uB00B';
    public static final char BAR_10 = '\uB00C';
    public static final char FISH = '\uB00D';
    public static final char STRUGGLING_FISH_1 = '\uB00E';
    public static final char STRUGGLING_FISH_2 = '\uB00F';
    public static final char STRUGGLING_FISH_3 = '\uB010';
    public static final char TENSION_BAR = '\uB011';
    public static final char JUDGEMENT_EASY = '\uB012';
    public static final char JUDGEMENT_NORMAL = '\uB013';
    public static final char JUDGEMENT_HARD = '\uB014';

    private FishingGlyphs() { }

    public static Component glyph(char glyph) { return Component.text(glyph).font(DEFAULT_FONT); }
    public static Component icon(char glyph) { return Component.text(glyph).font(ICONS_FONT); }
    public static Component progressIcon(double progress) { return icon(iconGlyph(0xB001, progress)); }
    public static Component strainIcon(double tension) { return icon(iconGlyph(0xB011, tension)); }

    /** CustomFishing hold subtitle: bar, judgement layer, rewind, then moving pointer layer. */
    public static Component hold(double pointer, double judgement, int judgementWidth, char judgementGlyph) {
        int judgementPosition = pixel(judgement, HOLD_EFFECTIVE_WIDTH - judgementWidth);
        int pointerPosition = pixel(pointer, HOLD_EFFECTIVE_WIDTH - HOLD_POINTER_WIDTH);
        return glyph(BAR_10)
                .append(OffsetGlyphs.component(HOLD_JUDGEMENT_OFFSET + judgementPosition))
                .append(glyph(judgementGlyph))
                .append(OffsetGlyphs.component(HOLD_EFFECTIVE_WIDTH - judgementPosition - judgementWidth))
                .append(OffsetGlyphs.component(-HOLD_EFFECTIVE_WIDTH - 1 + pointerPosition))
                .append(glyph(POINTER))
                .append(OffsetGlyphs.component(HOLD_EFFECTIVE_WIDTH - pointerPosition - HOLD_POINTER_WIDTH + 1));
    }

    /** CustomFishing tension subtitle: tension bar followed by a fish moving across its effective area. */
    public static Component tension(double progress, boolean struggling, int frame) {
        int fishPosition = pixel(1.0 - progress, TENSION_EFFECTIVE_WIDTH - TENSION_FISH_WIDTH);
        char fish = struggling ? switch (Math.floorMod(frame, 3)) {
            case 1 -> STRUGGLING_FISH_2;
            case 2 -> STRUGGLING_FISH_3;
            default -> STRUGGLING_FISH_1;
        } : FISH;
        return glyph(TENSION_BAR)
                .append(OffsetGlyphs.component(TENSION_FISH_OFFSET + fishPosition))
                .append(glyph(fish))
                .append(OffsetGlyphs.component(TENSION_EFFECTIVE_WIDTH - fishPosition - TENSION_FISH_WIDTH));
    }

    /** CustomFishing accurate-click subtitle: pre-drawn bar followed by a moving pointer layer. */
    public static Component accurateClick(char bar, double position, int pointerOffset, int sections) {
        int effectiveWidth = sections * 16;
        int pointerPosition = pixel(position, effectiveWidth - ACCURATE_POINTER_WIDTH);
        return glyph(bar)
                .append(OffsetGlyphs.component(pointerOffset + pointerPosition))
                .append(glyph(POINTER))
                .append(OffsetGlyphs.component(effectiveWidth - pointerPosition - ACCURATE_POINTER_WIDTH));
    }

    private static char iconGlyph(int start, double progress) {
        int index = Math.min(8, Math.max(0, (int) Math.floor(clamp(progress) * 9)));
        return (char) (start + index);
    }

    private static int pixel(double position, int width) { return (int) Math.round(clamp(position) * width); }
    private static double clamp(double value) { return Math.max(0.0, Math.min(1.0, value)); }
}
