package me.nakilex.levelplugin.hud.core;

import java.util.Locale;

public enum HudAnchor {
    TOP_LEFT,
    TOP_CENTER,
    TOP_RIGHT,
    CENTER_LEFT,
    CENTER,
    CENTER_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_CENTER,
    BOTTOM_RIGHT;

    public static HudAnchor from(String raw) {
        if (raw == null || raw.isBlank()) {
            return TOP_LEFT;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        for (HudAnchor anchor : values()) {
            if (anchor.name().equals(normalized)) {
                return anchor;
            }
        }
        return TOP_LEFT;
    }
}
