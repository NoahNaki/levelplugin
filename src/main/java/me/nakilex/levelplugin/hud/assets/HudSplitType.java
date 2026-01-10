package me.nakilex.levelplugin.hud.assets;

import java.util.Locale;

public enum HudSplitType {
    UP,
    DOWN,
    LEFT,
    RIGHT;

    public static HudSplitType from(String value) {
        if (value == null) {
            return UP;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        try {
            return HudSplitType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return UP;
        }
    }
}
