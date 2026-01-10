package me.nakilex.levelplugin.hud.core;

import java.util.Locale;

public enum HudElementType {
    TEXT,
    IMAGE,
    BAR;

    public static HudElementType from(String value) {
        if (value == null) {
            return TEXT;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        try {
            return HudElementType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return TEXT;
        }
    }
}
