package me.nakilex.levelplugin.hud.render;

import java.util.Locale;

public enum HudRenderChannel {
    ACTIONBAR,
    BOSSBAR;

    public static HudRenderChannel from(String raw) {
        if (raw == null || raw.isBlank()) {
            return ACTIONBAR;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        for (HudRenderChannel channel : values()) {
            if (channel.name().equals(normalized)) {
                return channel;
            }
        }
        return ACTIONBAR;
    }
}
