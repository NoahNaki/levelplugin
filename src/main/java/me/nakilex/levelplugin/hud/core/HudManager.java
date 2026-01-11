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
        this.assetRegistry = buildAssetRegistry(config.getImages(), resolvedRoot);
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
        packBuilder.build(resolvedOutputFolder.toString(), config.getNamespace(), assetRegistry, resolvedRoot);
        int bossBarLines = config.isMergeBossBar() ? 1 : config.getBossbarLines();
        Key fontKey = Key.key(config.getNamespace(), "hud_generated");
        this.renderer = new HudBossBarRenderer(bossBarLines, config.getLineHeightPx(),
                config.getCanvasWidthPx(), config.getCanvasHeightPx(), config.isMergeBossBar(), fontKey);
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
                List<HudResolvedElement> resolved = resolveLayoutElements(player, layout, placement, context,
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
        java.nio.file.Path resolvedOutput = resolveOutputFolder();
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                "HUD resolvedSourceTexturesFolder: " + resolvedRoot);
        List<String> missing = packBuilder.collectMissingTextures(
                resolvedRoot.toString(), assetRegistry);
        if (missing.isEmpty()) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.SUCCESS, "All HUD textures found in pack.");
        } else {
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
        List<String> missingFont = packBuilder.collectMissingFontTextures(resolvedOutput, config.getNamespace());
        if (missingFont.isEmpty()) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.SUCCESS, "HUD font JSON references are valid.");
            return;
        }
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.WARNING,
                "Missing HUD font textures: " + missingFont.size());
        int fontLimit = Math.min(5, missingFont.size());
        for (int i = 0; i < fontLimit; i++) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.WARNING, "- " + missingFont.get(i));
        }
        if (missingFont.size() > fontLimit) {
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
                resolveLayoutElements(player, layout, placement, context, groupBase, debugEntries);
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
                resolved.addAll(resolveLayoutElements(player, layout, placement, context, groupBase, debugEntries));
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
            double requestedScale = element.getScale();
            double bucketScale = assetRegistry == null ? 1.0 : HudAssetRegistry.bucketScale(requestedScale);
            if (element.getType() == HudElementType.TEXT) {
                bucketScale = 1.0;
            }
            String text = switch (element.getType()) {
                case TEXT -> placeholderService.resolve(player, element.getText());
                case IMAGE -> resolveImageGlyph(element.getAssetId(), bucketScale);
                case BAR -> resolveBarGlyph(player, element.getAssetId(), bucketScale);
            };
            if (text == null || text.isBlank()) {
                continue;
            }
            ElementMetrics metrics = measureElementMetrics(element, text, bucketScale);
            ElementMetrics baseMetrics = measureBaseMetrics(element, text);
            int offsetX = element.getPosition().pixelX();
            int offsetY = element.getPosition().pixelY();
            layouts.add(new ElementLayout(element, text, offsetX, offsetY,
                    metrics.width(), metrics.height(), baseMetrics.width(), baseMetrics.height(),
                    requestedScale, bucketScale));
        }
        Map<String, AnchorBounds> anchors = buildAnchorBounds(layouts, groupBase, placement.getAlign());
        for (ElementLayout layoutElement : layouts) {
            HudElement element = layoutElement.element();
            int width = layoutElement.width();
            int height = layoutElement.height();
            int x = groupBase.x() + layoutElement.offsetX() + alignmentShift(placement.getAlign(), width);
            int y = groupBase.y() + layoutElement.offsetY();
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
            resolved.add(new HudResolvedElement(element.getId(), layoutElement.text(), x, y, 0,
                    width, height, element.getLayer(), 1.0, align));
            if (debugEntries != null) {
                debugEntries.add(buildDebugEntry(element.getId(), groupBase.x(), groupBase.y(),
                        layoutElement.offsetX(), layoutElement.offsetY(), x, y, layoutElement.baseWidth(),
                        layoutElement.baseHeight(), width, height, layoutElement.requestedScale(),
                        layoutElement.bucketScale()));
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

    private ElementMetrics measureElementMetrics(HudElement element, String text, double bucketScale) {
        if (element == null) {
            return new ElementMetrics(0, 0);
        }
        return switch (element.getType()) {
            case TEXT -> new ElementMetrics(pixelLength(text), DEFAULT_TEXT_HEIGHT_PX);
            case IMAGE, BAR -> new ElementMetrics(
                    assetRegistry == null ? 0 : assetRegistry.getAssetWidth(element.getAssetId(), bucketScale),
                    assetRegistry == null ? 0 : assetRegistry.getAssetHeight(element.getAssetId(), bucketScale));
        };
    }

    private ElementMetrics measureBaseMetrics(HudElement element, String text) {
        if (element == null) {
            return new ElementMetrics(0, 0);
        }
        return switch (element.getType()) {
            case TEXT -> new ElementMetrics(pixelLength(text), DEFAULT_TEXT_HEIGHT_PX);
            case IMAGE, BAR -> new ElementMetrics(
                    assetRegistry == null ? 0 : assetRegistry.getBaseAssetWidth(element.getAssetId()),
                    assetRegistry == null ? 0 : assetRegistry.getBaseAssetHeight(element.getAssetId()));
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
                Integer.MAX_VALUE - 1, 1.0, HudTextAlign.LEFT));
        resolved.add(new HudResolvedElement("hud_debug_origin_" + placement.getLayoutId(),
                originLabel, groupBase.x(), groupBase.y(), row, originWidth, DEFAULT_TEXT_HEIGHT_PX,
                Integer.MAX_VALUE - 1, 1.0, HudTextAlign.LEFT));
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
                    + " requestedScale=" + entry.requestedScale()
                    + " bucketScale=" + entry.bucketScale()
                    + " base=" + entry.baseWidth() + "x" + entry.baseHeight()
                    + " variant=" + entry.variantWidth() + "x" + entry.variantHeight();
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
                                       int baseWidth,
                                       int baseHeight,
                                       int variantWidth,
                                       int variantHeight,
                                       double requestedScale,
                                       double bucketScale) {
        int lineIndex = clampLineIndex(finalY);
        return new DebugEntry(id, moduleOriginX, moduleOriginY, localOffsetX, localOffsetY, finalX, finalY,
                lineIndex, baseWidth, baseHeight, variantWidth, variantHeight, requestedScale, bucketScale);
    }

    private int clampLineIndex(int yPx) {
        int lineHeight = Math.max(1, config.getLineHeightPx());
        int line = Math.floorDiv(yPx, lineHeight);
        int maxLines = config.isMergeBossBar() ? 1 : Math.max(1, config.getBossbarLines());
        return Math.max(0, Math.min(maxLines - 1, line));
    }

    private record ElementLayout(HudElement element, String text, int offsetX, int offsetY,
                                 int width, int height, int baseWidth, int baseHeight,
                                 double requestedScale, double bucketScale) {
    }

    private record ElementMetrics(int width, int height) {
    }

    private record DebugEntry(String id,
                              int moduleOriginX,
                              int moduleOriginY,
                              int localOffsetX,
                              int localOffsetY,
                              int finalX,
                              int finalY,
                              int lineIndex,
                              int baseWidth,
                              int baseHeight,
                              int variantWidth,
                              int variantHeight,
                              double requestedScale,
                              double bucketScale) {
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
                                                java.nio.file.Path sourceTextureRoot) {
        HudAssetRegistry registry = new HudAssetRegistry();
        HudGlyphAllocator allocator = new HudGlyphAllocator();
        me.nakilex.levelplugin.hud.assets.HudTextureResolver resolver = new me.nakilex.levelplugin.hud.assets.HudTextureResolver();
        for (HudImageDefinition definition : images.values()) {
            registry.registerDefinition(definition);
            if (definition.getType() == HudImageType.LISTENER) {
                int frames = definition.getSplit() > 0 ? definition.getSplit() : definition.getFrames().size();
                List<HudGlyph> glyphs = new ArrayList<>();
                List<String> baseTextures = new ArrayList<>();
                for (int index = 1; index <= frames; index++) {
                    String texture = resolveFrameTexture(definition, index, resolver);
                    if (texture == null || texture.isBlank()) {
                        continue;
                    }
                    baseTextures.add(texture);
                    glyphs.add(createGlyph(allocator.next(), texture, sourceTextureRoot));
                }
                if (glyphs.isEmpty()) {
                    plugin.getLogger().warning("HUD bar '" + definition.getId() + "' has no frames configured.");
                }
                registry.registerBarFrames(definition.getId(), glyphs);
                registerBarScaleVariants(registry, allocator, definition.getId(), glyphs, baseTextures);
            } else {
                if (definition.getTexture() != null && !definition.getTexture().isBlank()) {
                    String texture = resolver.resolveSingle(definition.getTexture());
                    HudGlyph baseGlyph = createGlyph(allocator.next(), texture, sourceTextureRoot);
                    registry.registerGlyph(definition.getId(), baseGlyph);
                    registerImageScaleVariants(registry, allocator, definition.getId(), baseGlyph);
                } else {
                    plugin.getLogger().warning("HUD image '" + definition.getId() + "' is missing a texture path.");
                }
            }
        }
        return registry;
    }

    private void registerImageScaleVariants(HudAssetRegistry registry,
                                            HudGlyphAllocator allocator,
                                            String id,
                                            HudGlyph baseGlyph) {
        if (registry == null || id == null || baseGlyph == null) {
            return;
        }
        for (double scale : HudAssetRegistry.SCALE_BUCKETS) {
            if (scale == 1.0) {
                continue;
            }
            String variantTexture = buildVariantTexturePath(baseGlyph.texturePath(), scale);
            HudGlyph scaled = createScaledGlyph(allocator.next(), baseGlyph, variantTexture, scale);
            registry.registerGlyphVariant(id, scale, scaled, baseGlyph.texturePath());
        }
    }

    private void registerBarScaleVariants(HudAssetRegistry registry,
                                          HudGlyphAllocator allocator,
                                          String id,
                                          List<HudGlyph> baseFrames,
                                          List<String> baseTextures) {
        if (registry == null || id == null || baseFrames == null || baseFrames.isEmpty()) {
            return;
        }
        for (double scale : HudAssetRegistry.SCALE_BUCKETS) {
            if (scale == 1.0) {
                continue;
            }
            List<HudGlyph> scaledFrames = new ArrayList<>();
            for (int i = 0; i < baseFrames.size(); i++) {
                HudGlyph baseGlyph = baseFrames.get(i);
                String baseTexture = i < baseTextures.size() ? baseTextures.get(i) : baseGlyph.texturePath();
                String variantTexture = buildVariantTexturePath(baseTexture, scale);
                scaledFrames.add(createScaledGlyph(allocator.next(), baseGlyph, variantTexture, scale));
            }
            registry.registerBarFramesVariant(id, scale, scaledFrames, baseFrames);
        }
    }

    private String buildVariantTexturePath(String baseTexture, double scale) {
        if (baseTexture == null || baseTexture.isBlank()) {
            return "";
        }
        int dot = baseTexture.lastIndexOf('.');
        int scaled = (int) Math.round(scale * 100);
        String suffix = "_s" + String.format("%03d", scaled);
        if (dot <= 0) {
            return baseTexture + suffix;
        }
        return baseTexture.substring(0, dot) + suffix + baseTexture.substring(dot);
    }

    private HudGlyph createScaledGlyph(char codepoint, HudGlyph baseGlyph, String texture, double scale) {
        int baseWidth = baseGlyph == null ? 0 : baseGlyph.width();
        int baseHeight = baseGlyph == null ? 0 : baseGlyph.height();
        int width = baseWidth <= 0 ? 0 : Math.max(1, (int) Math.round(baseWidth * scale));
        int height = baseHeight <= 0 ? 0 : Math.max(1, (int) Math.round(baseHeight * scale));
        return new HudGlyph(codepoint, texture, width, height);
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
