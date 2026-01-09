package me.nakilex.levelplugin.hud.config;

import me.nakilex.levelplugin.hud.core.HudLayout;

import java.util.Collections;
import java.util.Map;

public class HudConfig {
    private final int updateIntervalTicks;
    private final int placeholderCacheTtlMs;
    private final String defaultLayout;
    private final Map<String, HudLayout> layouts;

    public HudConfig(int updateIntervalTicks,
                     int placeholderCacheTtlMs,
                     String defaultLayout,
                     Map<String, HudLayout> layouts) {
        this.updateIntervalTicks = updateIntervalTicks;
        this.placeholderCacheTtlMs = placeholderCacheTtlMs;
        this.defaultLayout = defaultLayout;
        this.layouts = layouts == null ? Map.of() : Map.copyOf(layouts);
    }

    public int getUpdateIntervalTicks() {
        return updateIntervalTicks;
    }

    public int getPlaceholderCacheTtlMs() {
        return placeholderCacheTtlMs;
    }

    public String getDefaultLayout() {
        return defaultLayout;
    }

    public Map<String, HudLayout> getLayouts() {
        return Collections.unmodifiableMap(layouts);
    }
}
