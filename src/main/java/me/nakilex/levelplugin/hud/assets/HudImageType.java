package me.nakilex.levelplugin.hud.assets;

import java.util.Locale;

public enum HudImageType {
    SINGLE,
    LISTENER;

    public static HudImageType from(String value) {
        if (value == null) {
            return SINGLE;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        try {
            return HudImageType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return SINGLE;
        }
    }
}
