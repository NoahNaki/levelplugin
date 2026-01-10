package me.nakilex.levelplugin.hud.config;

import me.nakilex.levelplugin.hud.assets.HudImageDefinition;
import me.nakilex.levelplugin.hud.core.HudLayout;
import me.nakilex.levelplugin.hud.core.HudModule;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class HudConfig {
    private final int updateIntervalTicks;
    private final int placeholderCacheTtlMs;
    private final int canvasWidthPx;
    private final int canvasHeightPx;
    private final int lineHeightPx;
    private final int bossbarLines;
    private final boolean mergeBossBar;
    private final String namespace;
    private final String outputFolder;
    private final String sourceTexturesFolder;
    private final String imagesConfigPath;
    private final List<String> defaultModules;
    private final Map<String, HudModule> modules;
    private final Map<String, HudLayout> layouts;
    private final Map<String, HudImageDefinition> images;

    public HudConfig(int updateIntervalTicks,
                     int placeholderCacheTtlMs,
                     int canvasWidthPx,
                     int canvasHeightPx,
                     int lineHeightPx,
                     int bossbarLines,
                     boolean mergeBossBar,
                     String namespace,
                     String outputFolder,
                     String sourceTexturesFolder,
                     String imagesConfigPath,
                     List<String> defaultModules,
                     Map<String, HudModule> modules,
                     Map<String, HudLayout> layouts,
                     Map<String, HudImageDefinition> images) {
        this.updateIntervalTicks = updateIntervalTicks;
        this.placeholderCacheTtlMs = placeholderCacheTtlMs;
        this.canvasWidthPx = canvasWidthPx;
        this.canvasHeightPx = canvasHeightPx;
        this.lineHeightPx = lineHeightPx;
        this.bossbarLines = bossbarLines;
        this.mergeBossBar = mergeBossBar;
        this.namespace = namespace;
        this.outputFolder = outputFolder;
        this.sourceTexturesFolder = sourceTexturesFolder;
        this.imagesConfigPath = imagesConfigPath;
        this.defaultModules = defaultModules == null ? List.of() : List.copyOf(defaultModules);
        this.modules = modules == null ? Map.of() : Map.copyOf(modules);
        this.layouts = layouts == null ? Map.of() : Map.copyOf(layouts);
        this.images = images == null ? Map.of() : Map.copyOf(images);
    }

    public int getUpdateIntervalTicks() {
        return updateIntervalTicks;
    }

    public int getPlaceholderCacheTtlMs() {
        return placeholderCacheTtlMs;
    }

    public int getCanvasWidthPx() {
        return canvasWidthPx;
    }

    public int getCanvasHeightPx() {
        return canvasHeightPx;
    }

    public int getLineHeightPx() {
        return lineHeightPx;
    }

    public int getBossbarLines() {
        return bossbarLines;
    }

    public boolean isMergeBossBar() {
        return mergeBossBar;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getOutputFolder() {
        return outputFolder;
    }

    public String getSourceTexturesFolder() {
        return sourceTexturesFolder;
    }

    public String getImagesConfigPath() {
        return imagesConfigPath;
    }

    public List<String> getDefaultModules() {
        return Collections.unmodifiableList(defaultModules);
    }

    public Map<String, HudModule> getModules() {
        return Collections.unmodifiableMap(modules);
    }

    public Map<String, HudLayout> getLayouts() {
        return Collections.unmodifiableMap(layouts);
    }

    public Map<String, HudImageDefinition> getImages() {
        return Collections.unmodifiableMap(images);
    }
}
