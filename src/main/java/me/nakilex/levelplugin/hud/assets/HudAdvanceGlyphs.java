package me.nakilex.levelplugin.hud.assets;

import java.util.LinkedHashMap;
import java.util.Map;

public final class HudAdvanceGlyphs {
    public static final int MAX_ADVANCE = 128;
    private static final int PUA_END = 0xF8FF;
    private static final int RANGE_SIZE = MAX_ADVANCE * 2;
    private static final int RANGE_START = PUA_END - RANGE_SIZE + 1;

    private HudAdvanceGlyphs() {
    }

    public static int advanceRangeStart() {
        return RANGE_START;
    }

    public static boolean isAdvanceGlyph(char codepoint) {
        return codepoint >= RANGE_START && codepoint <= PUA_END;
    }

    public static char codepointForAdvance(int advancePx) {
        if (advancePx == 0 || Math.abs(advancePx) > MAX_ADVANCE) {
            throw new IllegalArgumentException("Advance out of range: " + advancePx);
        }
        int offset = Math.abs(advancePx) - 1;
        int base = advancePx < 0 ? RANGE_START : RANGE_START + MAX_ADVANCE;
        return (char) (base + offset);
    }

    public static Map<Character, Integer> buildAdvanceMap() {
        Map<Character, Integer> advances = new LinkedHashMap<>();
        for (int i = MAX_ADVANCE; i >= 1; i--) {
            advances.put(codepointForAdvance(-i), -i);
        }
        for (int i = 1; i <= MAX_ADVANCE; i++) {
            advances.put(codepointForAdvance(i), i);
        }
        return advances;
    }
}
