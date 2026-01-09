package me.nakilex.levelplugin.hud.core;

import java.util.Locale;

public enum HudTextAlign {
    LEFT,
    CENTER,
    RIGHT;

    public static HudTextAlign from(String value) {
        if (value == null) {
            return LEFT;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        try {
            return HudTextAlign.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return LEFT;
        }
    }
}
