package me.nakilex.levelplugin.hud.core;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.hud.assets.HudAssetRegistry;
import me.nakilex.levelplugin.hud.assets.HudGlyph;
import me.nakilex.levelplugin.hud.assets.HudGlyphAllocator;
import me.nakilex.levelplugin.hud.assets.HudImageDefinition;
import me.nakilex.levelplugin.hud.assets.HudImageType;
import me.nakilex.levelplugin.hud.assets.HudPackBuilder;
import me.nakilex.levelplugin.hud.assets.HudSplitType;
import me.nakilex.levelplugin.hud.conditions.HudConditionContext;
import me.nakilex.levelplugin.hud.config.HudConfig;
import me.nakilex.levelplugin.hud.config.HudConfigLoader;
import me.nakilex.levelplugin.hud.input.HudInputListener;
import me.nakilex.levelplugin.hud.placeholders.HudPlaceholderCache;
import me.nakilex.levelplugin.hud.placeholders.HudPlaceholderRegistry;
import me.nakilex.levelplugin.hud.placeholders.HudPlaceholderService;
import me.nakilex.levelplugin.hud.render.HudActionBarDisplay;
import me.nakilex.levelplugin.hud.render.HudBossBarDisplay;
import me.nakilex.levelplugin.hud.render.HudBossBarRenderer;
import me.nakilex.levelplugin.hud.render.HudDisplay;
import me.nakilex.levelplugin.hud.render.HudRenderChannel;
import me.nakilex.levelplugin.hud.render.HudRenderOutput;
import me.nakilex.levelplugin.hud.render.HudRenderer;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.DefaultFontInfo;
import me.nakilex.levelplugin.utils.ResourcePackUtil;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.io.IOException;

public class HudManager {
    private static final int BASELINE_OFFSET_PX = 54;
    private static final int DEFAULT_TEXT_HEIGHT_PX = 8;
    private static final long DEBUG_LOG_INTERVAL_MS = 1000L;
    private final Main plugin;
    private final HudConfigLoader configLoader;
    private final HudPlaceholderCache placeholderCache;
    private final HudPlaceholderService placeholderService;
    private final HudInputListener inputListener = new HudInputListener();
    private final Map<UUID, HudPlayerState> playerStates = new ConcurrentHashMap<>();
    private BukkitTask task;
    private HudConfig config;
    private HudRenderer renderer;
    private HudDisplay hudDisplay;
    private HudAssetRegistry assetRegistry;

    public HudManager(Main plugin) {
        this.plugin = plugin;
        this.configLoader = new HudConfigLoader(plugin);
        this.placeholderCache = new HudPlaceholderCache(150);
        this.placeholderService = new HudPlaceholderService(new HudPlaceholderRegistry(), placeholderCache);
    }

    public void enable() {
        reload();
        Bukkit.getPluginManager().registerEvents(inputListener, plugin);
        Bukkit.getPluginManager().registerEvents(new HudPlayerListener(), plugin);
    }

    public void disable() {
        stopTask();
        if (hudDisplay != null) {
            hudDisplay.clearAll();
        }
        playerStates.clear();
        placeholderCache.clearAll();
    }

    public void reload() {
        this.config = configLoader.load();
        placeholderCache.setTtlMs(config.getPlaceholderCacheTtlMs());
        java.nio.file.Path resolvedRoot = resolveSourceTextureRoot();
        this.assetRegistry = buildAssetRegistry(config.getImages(), config.getLayouts(), resolvedRoot);
        HudPackBuilder packBuilder = new HudPackBuilder(plugin);
        java.nio.file.Path resolvedOutputFolder = resolveOutputFolder();
        plugin.getLogger().info("HUD sourceTexturesFolder: " + config.getSourceTexturesFolder());
        plugin.getLogger().info("HUD resolvedSourceTexturesFolder: " + resolvedRoot);
        plugin.getLogger().info("HUD imagesConfigPath: " + config.getImagesConfigPath());
        plugin.getLogger().info("HUD resolvedOutputFolder: " + resolvedOutputFolder);
        logTextureSample(config.getImages());
        List<String> missing = packBuilder.collectMissingTextures(resolvedRoot.toString(), assetRegistry);
        if (!missing.isEmpty()) {
            plugin.getLogger().warning("HUD textures missing from source folder (" + missing.size() + "). Example: " + missing.get(0));
        }
        packBuilder.build(resolvedOutputFolder.toString(), config.getNamespace(), assetRegistry);
        List<ShaderElement> shaderElements = buildShaderElements(config.getModules(), config.getLayouts());
        Map<String, net.kyori.adventure.text.format.TextColor> shaderColors = buildShaderColors(shaderElements);
        ShaderBuild shaderBuild = buildShaderCases(shaderElements);
        packBuilder.buildTextShader(resolvedOutputFolder.toString(),
                config.getNamespace(),
                shaderBuild.maxId(),
                shaderBuild.lines());
        int bossBarLines = config.isMergeBossBar() ? 1 : config.getBossbarLines();
        Key fontKey = Key.key(config.getNamespace(), "hud_generated");
        this.renderer = new HudBossBarRenderer(bossBarLines, config.getLineHeightPx(),
                config.getCanvasWidthPx(), config.getCanvasHeightPx(), config.isMergeBossBar(), fontKey, shaderColors);
        if (this.hudDisplay != null) {
            this.hudDisplay.clearAll();
        }
        this.hudDisplay = createDisplay(config.getRenderChannel(), bossBarLines);
        refreshHudResourcePack();
        stopTask();
        startTask();
        placeholderCache.clearAll();
        plugin.getLogger().info("HUD config reloaded.");
    }

    public void toggle(Player player) {
        if (player == null) {
            return;
        }
        HudPlayerState state = playerStates.computeIfAbsent(player.getUniqueId(), id -> new HudPlayerState());
        if (!state.isEnabled()) {
            state.setEnabled(true);
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS, "HUD enabled.");
        } else {
            state.setEnabled(false);
            if (hudDisplay != null) {
                hudDisplay.clear(player);
            }
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "HUD disabled.");
        }
    }

    public void toggleDebugMode(Player player) {
        if (player == null) {
            return;
        }
        HudPlayerState state = playerStates.computeIfAbsent(player.getUniqueId(), id -> new HudPlayerState());
        boolean enabled = !state.isDebugMode();
        state.setDebugMode(enabled);
        state.setLastDebugLogMs(System.currentTimeMillis());
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                enabled ? "HUD debug mode enabled." : "HUD debug mode disabled.");
        if (enabled) {
            logDebugEntries(player, buildDebugEntries(player), true);
        }
    }

    public void setDebugMode(Player player, boolean enabled) {
        if (player == null) {
            return;
        }
        HudPlayerState state = playerStates.computeIfAbsent(player.getUniqueId(), id -> new HudPlayerState());
        state.setDebugMode(enabled);
        state.setLastDebugLogMs(System.currentTimeMillis());
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                enabled ? "HUD debug mode enabled." : "HUD debug mode disabled.");
        if (enabled) {
            logDebugEntries(player, buildDebugEntries(player), true);
        }
    }

    public void debug(Player player) {
        if (player == null) {
            return;
        }
        if (config == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "No HUD configuration loaded.");
            return;
        }
        int shown = 0;
        int hidden = 0;
        HudConditionContext context = new HudConditionContext(placeholderService);
        List<String> moduleIds = config.getDefaultModules().isEmpty()
                ? new ArrayList<>(config.getModules().keySet())
                : config.getDefaultModules();
        List<DebugEntry> debugEntries = new ArrayList<>();
        for (String moduleId : moduleIds) {
            HudModule module = config.getModules().get(moduleId.toLowerCase(Locale.ROOT));
            if (module == null) {
                continue;
            }
            for (HudLayoutPlacement placement : module.getPlacements()) {
                HudLayout layout = config.getLayouts().get(placement.getLayoutId().toLowerCase(Locale.ROOT));
                if (layout == null) {
                    continue;
                }
                for (HudElement element : layout.getElements()) {
                    List<String> failures = new ArrayList<>();
                    for (me.nakilex.levelplugin.hud.conditions.HudCondition condition : element.getConditions()) {
                        if (!condition.matches(player, context)) {
                            failures.add(condition.describe(player, context));
                        }
                    }
                    if (!failures.isEmpty()) {
                        hidden++;
                        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                                element.getId() + " hidden: " + String.join("; ", failures));
                        continue;
                    }
                }
                HudPositionResolver.ResolvedPosition groupBase = resolveGroupBase(placement);
                List<HudResolvedElement> resolved = resolveLayoutElements(player, moduleId.toLowerCase(Locale.ROOT), layout, placement, context,
                        groupBase, debugEntries);
                shown += resolved.size();
            }
        }
        logDebugEntries(player, debugEntries, true);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                "HUD debug summary: shown=" + shown + ", hidden=" + hidden);
    }

    public void debugAssets(CommandSender sender) {
        if (sender == null) {
            return;
        }
        if (config == null || assetRegistry == null) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR, "HUD assets not loaded.");
            return;
        }
        HudPackBuilder packBuilder = new HudPackBuilder(plugin);
        java.nio.file.Path resolvedRoot = resolveSourceTextureRoot();
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                "HUD resolvedSourceTexturesFolder: " + resolvedRoot);
        List<String> missing = packBuilder.collectMissingTextures(
                resolvedRoot.toString(), assetRegistry);
        if (missing.isEmpty()) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.SUCCESS, "All HUD textures found in pack.");
            return;
        }
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.WARNING,
                "Missing HUD textures: " + missing.size());
        int limit = Math.min(5, missing.size());
        for (int i = 0; i < limit; i++) {
            java.nio.file.Path fullPath = resolvedRoot.resolve(missing.get(i)).normalize();
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.WARNING, "- " + fullPath);
        }
        if (missing.size() > limit) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.WARNING, "... and more");
        }
    }

    public void testGlyph(Player player) {
        if (player == null) {
            return;
        }
        if (config == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "No HUD configuration loaded.");
            return;
        }
        String namespace = config.getNamespace() == null || config.getNamespace().isBlank()
                ? "betterhud"
                : config.getNamespace();
        Key fontKey = Key.key(namespace, "hud_fantasy_hud_image");
        boolean refreshed = ResourcePackUtil.refresh(player);
        if (!refreshed) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "Unable to refresh resource pack. Check server resource pack settings.");
        }
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                "Rendering test glyph with font " + namespace + ":hud_fantasy_hud_image.");
        Component glyph = Component.text("\uE000").font(fontKey);
        Component message = Component.text("HUD glyph: ").append(glyph);
        player.sendMessage(message);
    }

    private void startTask() {
        int interval = config == null ? 20 : config.getUpdateIntervalTicks();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, interval);
    }

    private void stopTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        if (config == null || renderer == null || hudDisplay == null) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            HudPlayerState state = playerStates.computeIfAbsent(player.getUniqueId(), id -> new HudPlayerState());
            if (!state.isEnabled()) {
                continue;
            }
            updatePlayer(player);
        }
    }

    private void updatePlayer(Player player) {
        HudCanvas canvas = buildCanvas(player);
        HudRenderOutput output = renderer.render(canvas);
        hudDisplay.update(player, output);
    }

    public Component getHudActionBarComponent(Player player) {
        if (config == null || renderer == null) {
            return Component.empty();
        }
        if (config.getRenderChannel() != HudRenderChannel.ACTIONBAR) {
            return Component.empty();
        }
        HudCanvas canvas = buildCanvas(player);
        HudRenderOutput output = renderer.render(canvas);
        return buildActionBarComponent(output);
    }

    private Component buildActionBarComponent(HudRenderOutput output) {
        if (output == null) {
            return Component.empty();
        }
        List<Component> lines = output.getBossBarLineComponents();
        Component combined = Component.empty();
        boolean first = true;
        for (Component line : lines) {
            if (line == null || line.equals(Component.empty())) {
                continue;
            }
            if (!first) {
                combined = combined.append(Component.newline());
            }
            combined = combined.append(line);
            first = false;
        }
        return combined;
    }

    private HudDisplay createDisplay(HudRenderChannel channel, int bossBarLines) {
        HudRenderChannel resolved = channel == null ? HudRenderChannel.ACTIONBAR : channel;
        return switch (resolved) {
            case BOSSBAR -> new HudBossBarDisplay(bossBarLines, BossBar.Color.WHITE, BossBar.Overlay.NOTCHED_20);
            case ACTIONBAR -> new HudActionBarDisplay();
        };
    }

    private void refreshHudResourcePack() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            ResourcePackUtil.refresh(player);
        }
    }

    private List<DebugEntry> buildDebugEntries(Player player) {
        List<DebugEntry> debugEntries = new ArrayList<>();
        if (config == null || player == null) {
            return debugEntries;
        }
        HudConditionContext context = new HudConditionContext(placeholderService);
        List<String> moduleIds = config.getDefaultModules().isEmpty()
                ? new ArrayList<>(config.getModules().keySet())
                : config.getDefaultModules();
        for (String moduleId : moduleIds) {
            HudModule module = config.getModules().get(moduleId.toLowerCase(Locale.ROOT));
            if (module == null) {
                continue;
            }
            for (HudLayoutPlacement placement : module.getPlacements()) {
                HudLayout layout = config.getLayouts().get(placement.getLayoutId().toLowerCase(Locale.ROOT));
                if (layout == null) {
                    continue;
                }
                HudPositionResolver.ResolvedPosition groupBase = resolveGroupBase(placement);
                resolveLayoutElements(player, moduleId.toLowerCase(Locale.ROOT), layout, placement, context, groupBase, debugEntries);
            }
        }
        return debugEntries;
    }

    private HudCanvas buildCanvas(Player player) {
        List<HudResolvedElement> resolved = new ArrayList<>();
        if (config == null) {
            return new HudCanvas(resolved);
        }
        HudPlayerState state = playerStates.computeIfAbsent(player.getUniqueId(), id -> new HudPlayerState());
        boolean debugEnabled = state.isDebugMode();
        List<DebugEntry> debugEntries = debugEnabled ? new ArrayList<>() : null;
        HudConditionContext context = new HudConditionContext(placeholderService);
        List<String> moduleIds = config.getDefaultModules().isEmpty()
                ? new ArrayList<>(config.getModules().keySet())
                : config.getDefaultModules();
        for (String moduleId : moduleIds) {
            String normalizedModuleId = moduleId.toLowerCase(Locale.ROOT);
            HudModule module = config.getModules().get(normalizedModuleId);
            if (module == null) {
                continue;
            }
            for (HudLayoutPlacement placement : module.getPlacements()) {
                HudLayout layout = config.getLayouts().get(placement.getLayoutId().toLowerCase(Locale.ROOT));
                if (layout == null) {
                    continue;
                }
                HudPositionResolver.ResolvedPosition anchorBase = HudPositionResolver.resolveAnchorBase(
                        placement.getPosition().anchor(),
                        config.getCanvasWidthPx(),
                        config.getCanvasHeightPx());
                HudPositionResolver.ResolvedPosition groupBase = resolveGroupBase(placement);
                resolved.addAll(resolveLayoutElements(player, normalizedModuleId, layout, placement, context, groupBase, debugEntries));
                if (debugEnabled) {
                    appendDebugMarkers(resolved, placement, anchorBase, groupBase);
                }
            }
        }
        if (debugEnabled) {
            maybeLogDebugEntries(player, debugEntries, state);
        }
        return new HudCanvas(resolved);
    }

    private List<HudResolvedElement> resolveLayoutElements(Player player,
                                                           String moduleId,
                                                           HudLayout layout,
                                                           HudLayoutPlacement placement,
                                                           HudConditionContext context,
                                                           HudPositionResolver.ResolvedPosition groupBase,
                                                           List<DebugEntry> debugEntries) {
        List<HudResolvedElement> resolved = new ArrayList<>();
        if (layout == null || placement == null) {
            return resolved;
        }
        List<ElementLayout> layouts = new ArrayList<>();
        for (HudElement element : layout.getElements()) {
            if (!element.shouldRender(player, context)) {
                continue;
            }
            String text = switch (element.getType()) {
                case TEXT -> placeholderService.resolve(player, element.getText());
                case IMAGE -> resolveImageGlyph(element.getAssetId(), element.getScale());
                case BAR -> resolveBarGlyph(player, element.getAssetId(), element.getScale());
            };
            if (text == null || text.isBlank()) {
                continue;
            }
            ElementMetrics metrics = measureElementMetrics(element, text);
            int offsetX = scaleOffset(element.getPosition().pixelX(), metrics.scaleFactor());
            int offsetY = scaleOffset(element.getPosition().pixelY(), metrics.scaleFactor());
            layouts.add(new ElementLayout(element, text, offsetX, offsetY, metrics.width(), metrics.height()));
        }
        Map<String, AnchorBounds> anchors = buildAnchorBounds(layouts, groupBase, placement.getAlign());
        for (ElementLayout layoutElement : layouts) {
            HudElement element = layoutElement.element();
            int width = layoutElement.width();
            int height = layoutElement.height();
            int x = groupBase.x() + layoutElement.offsetX() + alignmentShift(placement.getAlign(), width);
            int y = groupBase.y() + layoutElement.offsetY();
            GuiOffset guiOffset = combineGuiOffsets(placement.getPosition(), element.getPosition());
            HudTextAlign align = element.getAlign();
            if (element.getType() == HudElementType.TEXT && element.getAnchorId() != null
                    && !element.getAnchorId().isBlank()) {
                AnchorBounds anchor = anchors.get(element.getAnchorId().toLowerCase(Locale.ROOT));
                if (anchor != null) {
                    switch (element.getAlign()) {
                        case CENTER -> x = anchor.centerX() - width / 2 + layoutElement.offsetX();
                        case RIGHT -> x = anchor.rightX() - width + layoutElement.offsetX();
                        case LEFT -> x = anchor.leftX() + layoutElement.offsetX();
                    }
                    y = anchor.topY() + layoutElement.offsetY();
                    align = HudTextAlign.LEFT;
                }
            }
            double renderScale = element.getType() == HudElementType.TEXT ? element.getScale() : 1.0;
            String shaderKey = buildShaderKey(moduleId, layout.getId(), element.getId());
            resolved.add(new HudResolvedElement(element.getId(), layoutElement.text(), x, y, 0,
                    width, height, element.getLayer(), renderScale, align, shaderKey, guiOffset.xPercent(), guiOffset.yPercent()));
            if (debugEntries != null) {
                debugEntries.add(buildDebugEntry(element.getId(), groupBase.x(), groupBase.y(),
                        layoutElement.offsetX(), layoutElement.offsetY(), x, y, width, height, renderScale));
            }
        }
        return resolved;
    }

    private HudPositionResolver.ResolvedPosition resolveGroupBase(HudLayoutPlacement placement) {
        HudPositionResolver.ResolvedPosition base = HudPositionResolver.resolve(
                placement.getPosition(),
                config.getCanvasWidthPx(),
                config.getCanvasHeightPx());
        if (config.isApplyBaselineOffset()) {
            return new HudPositionResolver.ResolvedPosition(base.x(), base.y() - BASELINE_OFFSET_PX);
        }
        return base;
    }

    private Map<String, AnchorBounds> buildAnchorBounds(List<ElementLayout> layouts,
                                                        HudPositionResolver.ResolvedPosition groupBase,
                                                        HudTextAlign align) {
        Map<String, AnchorBounds> anchors = new HashMap<>();
        for (ElementLayout layoutElement : layouts) {
            HudElement element = layoutElement.element();
            if (element.getType() == HudElementType.TEXT) {
                continue;
            }
            String id = element.getId();
            if (id == null || id.isBlank()) {
                continue;
            }
            int width = layoutElement.width();
            int height = layoutElement.height();
            int x = groupBase.x() + layoutElement.offsetX() + alignmentShift(align, width);
            int y = groupBase.y() + layoutElement.offsetY();
            anchors.put(id.toLowerCase(Locale.ROOT), new AnchorBounds(x, y, width, height));
        }
        return anchors;
    }

    private int alignmentShift(HudTextAlign align, int width) {
        if (align == null) {
            return 0;
        }
        return switch (align) {
            case CENTER -> -(width / 2);
            case RIGHT -> -width;
            case LEFT -> 0;
        };
    }

    private ElementMetrics measureElementMetrics(HudElement element, String text) {
        if (element == null) {
            return new ElementMetrics(0, 0, 1.0);
        }
        return switch (element.getType()) {
            case TEXT -> new ElementMetrics(pixelLength(text), DEFAULT_TEXT_HEIGHT_PX, 1.0);
            case IMAGE, BAR -> resolveAssetMetrics(element.getAssetId(), element.getScale());
        };
    }

    private void appendDebugMarkers(List<HudResolvedElement> resolved,
                                    HudLayoutPlacement placement,
                                    HudPositionResolver.ResolvedPosition anchorBase,
                                    HudPositionResolver.ResolvedPosition groupBase) {
        if (resolved == null || placement == null) {
            return;
        }
        int row = placement.getPosition().row();
        String anchorLabel = org.bukkit.ChatColor.RED + "A";
        String originLabel = org.bukkit.ChatColor.YELLOW + "O";
        int anchorWidth = pixelLength(anchorLabel);
        int originWidth = pixelLength(originLabel);
        resolved.add(new HudResolvedElement("hud_debug_anchor_" + placement.getLayoutId(),
                anchorLabel, anchorBase.x(), anchorBase.y(), row, anchorWidth, DEFAULT_TEXT_HEIGHT_PX,
                Integer.MAX_VALUE - 1, 1.0, HudTextAlign.LEFT, "hud_debug_anchor", 0.0, 0.0));
        resolved.add(new HudResolvedElement("hud_debug_origin_" + placement.getLayoutId(),
                originLabel, groupBase.x(), groupBase.y(), row, originWidth, DEFAULT_TEXT_HEIGHT_PX,
                Integer.MAX_VALUE - 1, 1.0, HudTextAlign.LEFT, "hud_debug_origin", 0.0, 0.0));
    }

    private void maybeLogDebugEntries(Player player, List<DebugEntry> entries, HudPlayerState state) {
        if (player == null || state == null || entries == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - state.getLastDebugLogMs() < DEBUG_LOG_INTERVAL_MS) {
            return;
        }
        state.setLastDebugLogMs(now);
        logDebugEntries(player, entries, false);
    }

    private void logDebugEntries(Player player, List<DebugEntry> entries, boolean forceHeader) {
        if (player == null || entries == null) {
            return;
        }
        if (forceHeader) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO, "HUD debug positions:");
        }
        for (DebugEntry entry : entries) {
            String line = entry.id() + " module=(" + entry.moduleOriginX() + "," + entry.moduleOriginY() + ")"
                    + " local=(" + entry.localOffsetX() + "," + entry.localOffsetY() + ")"
                    + " final=(" + entry.finalX() + "," + entry.finalY() + ")"
                    + " lineHeight=" + config.getLineHeightPx()
                    + " line=" + entry.lineIndex()
                    + " w=" + entry.width() + " h=" + entry.height() + " scale=" + entry.scale();
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO, line);
        }
    }

    private DebugEntry buildDebugEntry(String id,
                                       int moduleOriginX,
                                       int moduleOriginY,
                                       int localOffsetX,
                                       int localOffsetY,
                                       int finalX,
                                       int finalY,
                                       int width,
                                       int height,
                                       double scale) {
        int lineIndex = clampLineIndex(finalY);
        int scaledWidth = (int) Math.round(width * scale);
        int scaledHeight = (int) Math.round(height * scale);
        return new DebugEntry(id, moduleOriginX, moduleOriginY, localOffsetX, localOffsetY, finalX, finalY,
                lineIndex, scaledWidth, scaledHeight, scale);
    }

    private int clampLineIndex(int yPx) {
        int lineHeight = Math.max(1, config.getLineHeightPx());
        int line = Math.floorDiv(yPx, lineHeight);
        int maxLines = config.isMergeBossBar() ? 1 : Math.max(1, config.getBossbarLines());
        return Math.max(0, Math.min(maxLines - 1, line));
    }

    private record ElementLayout(HudElement element, String text, int offsetX, int offsetY,
                                 int width, int height) {
    }

    private record ElementMetrics(int width, int height, double scaleFactor) {
    }

    private record GuiOffset(double xPercent, double yPercent) {
    }

    private record ShaderElement(String key, int id, double guiXPercent, double guiYPercent, int layer) {
    }

    private record ShaderBuild(int maxId, List<String> lines) {
    }

    private record DebugEntry(String id,
                              int moduleOriginX,
                              int moduleOriginY,
                              int localOffsetX,
                              int localOffsetY,
                              int finalX,
                              int finalY,
                              int lineIndex,
                              int width,
                              int height,
                              double scale) {
    }

    private int pixelLength(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int px = 0;
        boolean previousCode = false;
        boolean bold = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§') {
                previousCode = true;
                continue;
            }
            if (previousCode) {
                previousCode = false;
                bold = c == 'l' || c == 'L';
                continue;
            }
            if (c >= 0xE000 && c <= 0xF8FF) {
                int glyphWidth = assetRegistry.getGlyphWidths().getOrDefault(c, DefaultFontInfo.SPACE.getLength());
                px += glyphWidth + 1;
                continue;
            }
            DefaultFontInfo dFI = DefaultFontInfo.getDefaultFontInfo(c);
            px += (bold ? DefaultFontInfo.getBoldLength() : dFI.getLength()) + 1;
        }
        return px;
    }

    private GuiOffset combineGuiOffsets(HudPosition placementPosition, HudPosition elementPosition) {
        double placementX = placementPosition == null ? 0.0 : placementPosition.guiX();
        double placementY = placementPosition == null ? 0.0 : placementPosition.guiY();
        double elementX = elementPosition == null ? 0.0 : elementPosition.guiX();
        double elementY = elementPosition == null ? 0.0 : elementPosition.guiY();
        return new GuiOffset(placementX + elementX, placementY + elementY);
    }

    private String buildShaderKey(String moduleId, String layoutId, String elementId) {
        String safeModule = moduleId == null ? "unknown" : moduleId;
        String safeLayout = layoutId == null ? "layout" : layoutId;
        String safeElement = elementId == null ? "element" : elementId;
        return safeModule + ":" + safeLayout + ":" + safeElement;
    }

    private List<ShaderElement> buildShaderElements(Map<String, HudModule> modules, Map<String, HudLayout> layouts) {
        if (modules == null || layouts == null) {
            return List.of();
        }
        List<ShaderElement> elements = new ArrayList<>();
        List<String> moduleIds = new ArrayList<>(modules.keySet());
        moduleIds.sort(String::compareTo);
        int nextId = 1;
        for (String moduleId : moduleIds) {
            HudModule module = modules.get(moduleId);
            if (module == null) {
                continue;
            }
            for (HudLayoutPlacement placement : module.getPlacements()) {
                if (placement == null) {
                    continue;
                }
                String layoutId = placement.getLayoutId();
                HudLayout layout = layoutId == null ? null : layouts.get(layoutId.toLowerCase(Locale.ROOT));
                if (layout == null) {
                    continue;
                }
                for (HudElement element : layout.getElements()) {
                    if (element == null) {
                        continue;
                    }
                    GuiOffset guiOffset = combineGuiOffsets(placement.getPosition(), element.getPosition());
                    String shaderKey = buildShaderKey(moduleId, layout.getId(), element.getId());
                    elements.add(new ShaderElement(shaderKey, nextId++, guiOffset.xPercent(),
                            guiOffset.yPercent(), element.getLayer()));
                }
            }
        }
        return elements;
    }

    private Map<String, net.kyori.adventure.text.format.TextColor> buildShaderColors(List<ShaderElement> elements) {
        if (elements == null || elements.isEmpty()) {
            return Map.of();
        }
        Map<String, net.kyori.adventure.text.format.TextColor> colors = new HashMap<>();
        for (ShaderElement element : elements) {
            colors.put(element.key(), encodeShaderColor(element.id()));
        }
        return colors;
    }

    private net.kyori.adventure.text.format.TextColor encodeShaderColor(int id) {
        int safeId = Math.max(1, id);
        int rgb = safeId & 0x00FFFFFF;
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return net.kyori.adventure.text.format.TextColor.color(r, g, b);
    }

    private ShaderBuild buildShaderCases(List<ShaderElement> elements) {
        if (elements == null || elements.isEmpty()) {
            return new ShaderBuild(0, List.of());
        }
        List<String> cases = new ArrayList<>();
        int maxId = 0;
        for (ShaderElement element : elements) {
            cases.add("        case " + element.id() + ":");
            if (element.guiXPercent() != 0.0) {
                cases.add("            xGui = ui.x * " + ((float) element.guiXPercent()) + " / 100.0;");
            }
            if (element.guiYPercent() != 0.0) {
                cases.add("            yGui = ui.y * " + ((float) element.guiYPercent()) + " / 100.0;");
            }
            if (element.layer() != 0) {
                cases.add("            layer = " + element.layer() + ";");
            }
            cases.add("            break;");
            maxId = Math.max(maxId, element.id());
        }
        return new ShaderBuild(maxId, cases);
    }

    private record AnchorBounds(int leftX, int topY, int width, int height) {
        int rightX() {
            return leftX + width;
        }

        int centerX() {
            return leftX + (width / 2);
        }
    }

    private String resolveImageGlyph(String assetId, double scale) {
        if (assetRegistry == null || assetId == null) {
            return "";
        }
        HudGlyph glyph = assetRegistry.getGlyph(assetId.toLowerCase(Locale.ROOT), scale);
        return glyph == null ? "" : String.valueOf(glyph.codepoint());
    }

    private String resolveBarGlyph(Player player, String assetId, double scale) {
        if (assetRegistry == null || assetId == null) {
            return "";
        }
        HudImageDefinition def = config.getImages().get(assetId.toLowerCase(Locale.ROOT));
        if (def == null) {
            return "";
        }
        double current = readNumber(player, def.getCurrent());
        double max = readNumber(player, def.getMax());
        double progress = max <= 0 ? 0.0 : Math.min(1.0, Math.max(0.0, current / max));
        int split = Math.max(1, def.getSplit());
        int index = (int) Math.round(progress * split);
        if (def.getSplitType() == HudSplitType.DOWN) {
            index = split - index;
        }
        if (index <= 0) {
            return "";
        }
        List<HudGlyph> frames = assetRegistry.getBarFrames(assetId.toLowerCase(Locale.ROOT), scale);
        if (frames.isEmpty()) {
            return "";
        }
        int frameIndex = Math.min(index, frames.size()) - 1;
        return String.valueOf(frames.get(frameIndex).codepoint());
    }

    private double readNumber(Player player, String token) {
        if (token == null || token.isBlank()) {
            return 0.0;
        }
        String value = token.startsWith("papi:")
                ? placeholderService.resolveValue(player, "%" + token.substring(5) + "%")
                : placeholderService.resolveValue(player, token);
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }

    private HudAssetRegistry buildAssetRegistry(Map<String, HudImageDefinition> images,
                                                Map<String, HudLayout> layouts,
                                                java.nio.file.Path sourceTextureRoot) {
        HudAssetRegistry registry = new HudAssetRegistry();
        HudGlyphAllocator allocator = new HudGlyphAllocator();
        me.nakilex.levelplugin.hud.assets.HudTextureResolver resolver = new me.nakilex.levelplugin.hud.assets.HudTextureResolver();
        Map<String, java.util.Set<Double>> scalesByAssetId = collectAssetScales(layouts);
        for (HudImageDefinition definition : images.values()) {
            registry.registerDefinition(definition);
            String definitionId = definition.getId();
            String normalizedId = definitionId == null ? "" : definitionId.toLowerCase(Locale.ROOT);
            java.util.Set<Double> scales = scalesByAssetId.getOrDefault(normalizedId, java.util.Set.of(1.0));
            if (definition.getType() == HudImageType.LISTENER) {
                int frames = definition.getSplit() > 0 ? definition.getSplit() : definition.getFrames().size();
                List<HudGlyph> baseFrames = new ArrayList<>();
                for (int index = 1; index <= frames; index++) {
                    String texture = resolveFrameTexture(definition, index, resolver);
                    if (texture == null || texture.isBlank()) {
                        continue;
                    }
                    baseFrames.add(createGlyph(allocator.next(), texture, sourceTextureRoot));
                }
                if (baseFrames.isEmpty()) {
                    plugin.getLogger().warning("HUD bar '" + definition.getId() + "' has no frames configured.");
                }
                registry.registerBaseBarFrames(definition.getId(), baseFrames);
                for (double scale : scales) {
                    List<HudGlyph> scaledFrames = new ArrayList<>();
                    boolean useBase = isScaleOne(scale);
                    for (HudGlyph baseGlyph : baseFrames) {
                        if (useBase) {
                            scaledFrames.add(baseGlyph);
                        } else {
                            scaledFrames.add(scaleGlyph(baseGlyph, allocator.next(), scale));
                        }
                    }
                    if (!scaledFrames.isEmpty()) {
                        registry.registerBarFrames(definition.getId(), scale, scaledFrames);
                    }
                }
            } else {
                if (definition.getTexture() != null && !definition.getTexture().isBlank()) {
                    String texture = resolver.resolveSingle(definition.getTexture());
                    HudGlyph baseGlyph = createGlyph(allocator.next(), texture, sourceTextureRoot);
                    registry.registerBaseGlyph(definition.getId(), baseGlyph);
                    for (double scale : scales) {
                        HudGlyph scaledGlyph = isScaleOne(scale)
                                ? baseGlyph
                                : scaleGlyph(baseGlyph, allocator.next(), scale);
                        registry.registerGlyph(definition.getId(), scale, scaledGlyph);
                    }
                } else {
                    plugin.getLogger().warning("HUD image '" + definition.getId() + "' is missing a texture path.");
                }
            }
        }
        return registry;
    }

    private ElementMetrics resolveAssetMetrics(String assetId, double scale) {
        if (assetRegistry == null || assetId == null || assetId.isBlank()) {
            return new ElementMetrics(0, 0, 1.0);
        }
        double normalizedScale = scale <= 0 ? 1.0 : scale;
        int baseWidth = assetRegistry.getBaseAssetWidth(assetId);
        int baseHeight = assetRegistry.getBaseAssetHeight(assetId);
        if (baseWidth <= 0 || baseHeight <= 0) {
            return new ElementMetrics(0, 0, 1.0);
        }
        int scaledHeight = Math.max(1, (int) Math.round(baseHeight * normalizedScale));
        double scaleFactor = (double) scaledHeight / baseHeight;
        int scaledWidth = (int) Math.round(baseWidth * scaleFactor);
        return new ElementMetrics(scaledWidth, scaledHeight, scaleFactor);
    }

    private int scaleOffset(int offset, double scaleFactor) {
        return (int) Math.round(offset * scaleFactor);
    }

    private Map<String, java.util.Set<Double>> collectAssetScales(Map<String, HudLayout> layouts) {
        Map<String, java.util.Set<Double>> scales = new HashMap<>();
        if (layouts == null) {
            return scales;
        }
        for (HudLayout layout : layouts.values()) {
            for (HudElement element : layout.getElements()) {
                if (element.getType() != HudElementType.IMAGE && element.getType() != HudElementType.BAR) {
                    continue;
                }
                String assetId = element.getAssetId();
                if (assetId == null || assetId.isBlank()) {
                    continue;
                }
                double scale = element.getScale() <= 0 ? 1.0 : element.getScale();
                scales.computeIfAbsent(assetId.toLowerCase(Locale.ROOT), key -> new java.util.HashSet<>())
                        .add(scale);
            }
        }
        return scales;
    }

    private boolean isScaleOne(double scale) {
        return Math.abs(scale - 1.0) < 0.0001;
    }

    private HudGlyph scaleGlyph(HudGlyph baseGlyph, char codepoint, double scale) {
        if (baseGlyph == null) {
            return new HudGlyph(codepoint, "", 0, 0);
        }
        int baseWidth = baseGlyph.width();
        int baseHeight = baseGlyph.height();
        if (baseWidth <= 0 || baseHeight <= 0) {
            return new HudGlyph(codepoint, baseGlyph.texturePath(), 0, 0);
        }
        double normalizedScale = scale <= 0 ? 1.0 : scale;
        int scaledHeight = Math.max(1, (int) Math.round(baseHeight * normalizedScale));
        double scaleFactor = (double) scaledHeight / baseHeight;
        int scaledWidth = (int) Math.round(baseWidth * scaleFactor);
        return new HudGlyph(codepoint, baseGlyph.texturePath(), scaledWidth, scaledHeight);
    }

    private HudGlyph createGlyph(char codepoint, String texture, java.nio.file.Path sourceTextureRoot) {
        int width = 0;
        int height = 0;
        java.nio.file.Path texturePath = resolveTexturePath(sourceTextureRoot, texture);
        if (texturePath != null) {
            try {
                java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(texturePath.toFile());
                if (image != null) {
                    width = image.getWidth();
                    height = image.getHeight();
                }
            } catch (IOException ex) {
                plugin.getLogger().warning("Failed to read HUD texture size for '" + texture + "': " + ex.getMessage());
            }
        }
        return new HudGlyph(codepoint, texture, width, height);
    }

    private java.nio.file.Path resolveTexturePath(java.nio.file.Path sourceTextureRoot, String texture) {
        if (sourceTextureRoot == null || texture == null || texture.isBlank()) {
            return null;
        }
        return sourceTextureRoot.resolve(texture).normalize();
    }

    private String resolveFrameTexture(HudImageDefinition definition,
                                       int index,
                                       me.nakilex.levelplugin.hud.assets.HudTextureResolver resolver) {
        if (definition.getFrames().size() > index) {
            return definition.getFrames().get(index);
        }
        if (definition.getFrames().isEmpty() && definition.getSplit() > 0) {
            return resolver.resolveBarFrame(definition, index);
        }
        if (definition.getTexture() == null || definition.getTexture().isBlank()) {
            return "";
        }
        return resolver.resolveBarFrame(definition, index);
    }

    private void logTextureSample(Map<String, HudImageDefinition> images) {
        int count = 0;
        for (HudImageDefinition definition : images.values()) {
            if (definition.getTexture() == null || definition.getTexture().isBlank()) {
                continue;
            }
            plugin.getLogger().info("HUD texture sample: " + definition.getTexture());
            count++;
            if (count >= 5) {
                break;
            }
        }
    }

    private java.nio.file.Path resolveSourceTextureRoot() {
        String configured = config.getSourceTexturesFolder();
        java.nio.file.Path root = java.nio.file.Paths.get(configured);
        if (!root.isAbsolute()) {
            root = Bukkit.getWorldContainer().toPath().resolve(root);
        }
        return root.toAbsolutePath().normalize();
    }

    private java.nio.file.Path resolveOutputFolder() {
        String configured = config.getOutputFolder();
        java.nio.file.Path output = java.nio.file.Paths.get(configured);
        if (!output.isAbsolute()) {
            java.io.File pluginsFolder = plugin.getDataFolder().getParentFile();
            output = pluginsFolder.toPath().resolve(output);
        }
        return output.toAbsolutePath().normalize();
    }

    private class HudPlayerListener implements Listener {
        @EventHandler
        public void onQuit(PlayerQuitEvent event) {
            if (hudDisplay != null) {
                hudDisplay.clear(event.getPlayer());
            }
            playerStates.remove(event.getPlayer().getUniqueId());
            placeholderCache.clear(event.getPlayer());
        }
    }
}
