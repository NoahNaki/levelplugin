package me.nakilex.levelplugin.player.fishing.minigame;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

/** Glyph catalogue and reusable layer composition for fishing mini-game resource-pack rendering. */
public final class FishingGlyphs {
    public static final Key DEFAULT_FONT = Key.key("customfishing:default");
    public static final Key ICONS_FONT = Key.key("customfishing:icons");
    public static final int BAR_WIDTH = 221;
    public static final int FISH_WIDTH = 10;
    public static final int POINTER_WIDTH = 6;

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
    public static final char TENSION_BAR = '\uB00C';
    public static final char FISH = '\uB00D';
    public static final char STRUGGLING_FISH_1 = '\uB00E';
    public static final char STRUGGLING_FISH_2 = '\uB00F';
    public static final char STRUGGLING_FISH_3 = '\uB010';
    public static final char JUDGEMENT_EASY = '\uB012';
    public static final char JUDGEMENT_NORMAL = '\uB013';
    public static final char JUDGEMENT_HARD = '\uB014';
    public static final char PROGRESS_ICON = '\uB01A';

    private FishingGlyphs() { }

    public static Component glyph(char glyph) { return Component.text(glyph).font(DEFAULT_FONT); }
    public static Component icon(char glyph) { return Component.text(glyph).font(ICONS_FONT); }
    public static Component bar(double progress) { return glyph(progressBar(progress)); }
    public static Component strain(double tension) { return icon((char) (0xB011 + Math.min(8, Math.max(0, (int) Math.floor(tension * 9))))); }

    public static Component movingPointer(double position) {
        return layered(BAR_1, pixel(position, POINTER_WIDTH), POINTER);
    }

    public static Component pointerWithJudgement(double pointerPosition, double judgementPosition,
                                                  char judgementGlyph, int judgementWidth) {
        int judgementX = pixel(judgementPosition, judgementWidth);
        int pointerX = pixel(pointerPosition, POINTER_WIDTH);
        return glyph(BAR_1)
                .append(OffsetGlyphs.component(-BAR_WIDTH + judgementX))
                .append(glyph(judgementGlyph))
                .append(OffsetGlyphs.component(-judgementX - judgementWidth + pointerX))
                .append(glyph(POINTER))
                .append(OffsetGlyphs.component(BAR_WIDTH - pointerX - POINTER_WIDTH));
    }

    public static Component fishWithJudgement(double fishPosition, double judgementPosition, char judgementGlyph, int judgementWidth) {
        return glyph(BAR_2)
                .append(OffsetGlyphs.component(-BAR_WIDTH + pixel(judgementPosition, judgementWidth)))
                .append(glyph(judgementGlyph))
                .append(OffsetGlyphs.component(-pixel(judgementPosition, judgementWidth) - judgementWidth))
                .append(OffsetGlyphs.component(pixel(fishPosition, FISH_WIDTH)))
                .append(glyph(FISH))
                .append(OffsetGlyphs.component(BAR_WIDTH - pixel(fishPosition, FISH_WIDTH) - FISH_WIDTH));
    }

    public static Component tension(double fishPosition, boolean struggling, int frame) {
        char fish = struggling ? switch (Math.floorMod(frame, 3)) {
            case 1 -> STRUGGLING_FISH_2;
            case 2 -> STRUGGLING_FISH_3;
            default -> STRUGGLING_FISH_1;
        } : FISH;
        return layered(TENSION_BAR, pixel(fishPosition, FISH_WIDTH), fish);
    }

    private static Component layered(char bar, int x, char foreground) {
        return glyph(bar).append(OffsetGlyphs.component(-BAR_WIDTH + x)).append(glyph(foreground))
                .append(OffsetGlyphs.component(BAR_WIDTH - x - (foreground == POINTER ? POINTER_WIDTH : FISH_WIDTH)));
    }

    private static int pixel(double position, int glyphWidth) {
        return (int) Math.round(Math.max(0.0, Math.min(1.0, position)) * (BAR_WIDTH - glyphWidth));
    }

    private static char progressBar(double progress) {
        int index = Math.min(9, Math.max(0, (int) Math.floor(progress * 10)));
        return (char) (BAR_1 + index);
    }
}
