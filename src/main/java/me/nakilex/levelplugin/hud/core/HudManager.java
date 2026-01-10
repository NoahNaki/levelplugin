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
import me.nakilex.levelplugin.hud.render.HudBossBarDisplay;
import me.nakilex.levelplugin.hud.render.HudBossBarRenderer;
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
    private final Main plugin;
    private final HudConfigLoader configLoader;
    private final HudPlaceholderCache placeholderCache;
    private final HudPlaceholderService placeholderService;
    private final HudInputListener inputListener = new HudInputListener();
    private final Map<UUID, HudPlayerState> playerStates = new ConcurrentHashMap<>();
    private BukkitTask task;
    private HudConfig config;
    private HudRenderer renderer;
    private HudBossBarDisplay bossBarDisplay;
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
        if (bossBarDisplay != null) {
            bossBarDisplay.clearAll();
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
        packBuilder.build(resolvedOutputFolder.toString(), config.getNamespace(), assetRegistry);
        int bossBarLines = config.isMergeBossBar() ? 1 : config.getBossbarLines();
        Key fontKey = Key.key(config.getNamespace(), "hud_generated");
        this.renderer = new HudBossBarRenderer(bossBarLines, config.getLineHeightPx(),
                config.getCanvasWidthPx(), config.getCanvasHeightPx(), config.isMergeBossBar(), fontKey,
                assetRegistry.getGlyphWidths());
        if (this.bossBarDisplay != null) {
            this.bossBarDisplay.clearAll();
        }
        this.bossBarDisplay = new HudBossBarDisplay(bossBarLines, BossBar.Color.WHITE, BossBar.Overlay.NOTCHED_20);
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
            if (bossBarDisplay != null) {
                bossBarDisplay.clear(player);
            }
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "HUD disabled.");
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
        HudConditionContext context = new HudConditionContext(placeholderService);
        int shown = 0;
        int hidden = 0;
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
                HudPositionResolver.ResolvedPosition layoutBase = HudPositionResolver.resolve(
                        placement.getPosition(),
                        config.getCanvasWidthPx(),
                        config.getCanvasHeightPx());
                Map<String, AnchorBounds> anchors = buildAnchors(layout, layoutBase);
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
                    HudResolvedElement resolved = resolveElement(player, element, layoutBase, anchors);
                    if (resolved != null && !resolved.getText().isBlank()) {
                        shown++;
                        String line = resolved.getId() + " x=" + resolved.getX() + " y=" + resolved.getY()
                                + " layer=" + resolved.getLayer() + " align=" + resolved.getAlign()
                                + " -> " + resolved.getText();
                        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO, line);
                    }
                }
            }
        }
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
        if (config == null || renderer == null || bossBarDisplay == null) {
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
        bossBarDisplay.update(player, output);
    }

    private void refreshHudResourcePack() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            ResourcePackUtil.refresh(player);
        }
    }

    private HudCanvas buildCanvas(Player player) {
        List<HudResolvedElement> resolved = new ArrayList<>();
        if (config == null) {
            return new HudCanvas(resolved);
        }
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
                HudPositionResolver.ResolvedPosition layoutBase = HudPositionResolver.resolve(
                        placement.getPosition(),
                        config.getCanvasWidthPx(),
                        config.getCanvasHeightPx());
                Map<String, AnchorBounds> anchors = buildAnchors(layout, layoutBase);
                for (HudElement element : layout.getElements()) {
                    if (!element.shouldRender(player, context)) {
                        continue;
                    }
                    HudResolvedElement resolvedElement = resolveElement(player, element, layoutBase, anchors);
                    if (resolvedElement != null && !resolvedElement.getText().isBlank()) {
                        resolved.add(resolvedElement);
                    }
                }
            }
        }
        return new HudCanvas(resolved);
    }

    private HudResolvedElement resolveElement(Player player, HudElement element,
                                              HudPositionResolver.ResolvedPosition layoutBase,
                                              Map<String, AnchorBounds> anchors) {
        String text = switch (element.getType()) {
            case TEXT -> placeholderService.resolve(player, element.getText());
            case IMAGE -> resolveImageGlyph(element.getAssetId());
            case BAR -> resolveBarGlyph(player, element.getAssetId());
        };
        if (text == null || text.isBlank()) {
            return null;
        }
        HudPositionResolver.ResolvedPosition elementPos = HudPositionResolver.resolve(
                element.getPosition(),
                config.getCanvasWidthPx(),
                config.getCanvasHeightPx());
        int x = layoutBase.x() + elementPos.x();
        int y = layoutBase.y() + elementPos.y();
        HudTextAlign align = element.getAlign();
        if (element.getType() == HudElementType.TEXT && anchors != null && element.getAnchorId() != null
                && !element.getAnchorId().isBlank()) {
            AnchorBounds anchor = anchors.get(element.getAnchorId().toLowerCase(Locale.ROOT));
            if (anchor != null) {
                int textWidth = (int) Math.round(pixelLength(text) * element.getScale());
                switch (element.getAlign()) {
                    case CENTER -> x = anchor.centerX() - textWidth / 2 + elementPos.x();
                    case RIGHT -> x = anchor.rightX() - textWidth + elementPos.x();
                    case LEFT -> x = anchor.leftX() + elementPos.x();
                }
                y = anchor.topY() + elementPos.y();
                align = HudTextAlign.LEFT;
            }
        }
        return new HudResolvedElement(element.getId(), text, x, y, element.getLayer(),
                element.getScale(), align);
    }

    private Map<String, AnchorBounds> buildAnchors(HudLayout layout,
                                                   HudPositionResolver.ResolvedPosition layoutBase) {
        Map<String, AnchorBounds> anchors = new HashMap<>();
        if (layout == null) {
            return anchors;
        }
        for (HudElement element : layout.getElements()) {
            if (element.getType() == HudElementType.TEXT) {
                continue;
            }
            String id = element.getId();
            if (id == null || id.isBlank()) {
                continue;
            }
            int width = element.getType() == HudElementType.IMAGE || element.getType() == HudElementType.BAR
                    ? assetRegistry.getAssetWidth(element.getAssetId())
                    : 0;
            int height = element.getType() == HudElementType.IMAGE || element.getType() == HudElementType.BAR
                    ? assetRegistry.getAssetHeight(element.getAssetId())
                    : 0;
            int scaledWidth = (int) Math.round(width * element.getScale());
            int scaledHeight = (int) Math.round(height * element.getScale());
            HudPositionResolver.ResolvedPosition elementPos = HudPositionResolver.resolve(
                    element.getPosition(),
                    config.getCanvasWidthPx(),
                    config.getCanvasHeightPx());
            int x = layoutBase.x() + elementPos.x();
            int y = layoutBase.y() + elementPos.y();
            anchors.put(id.toLowerCase(Locale.ROOT), new AnchorBounds(x, y, scaledWidth, scaledHeight));
        }
        return anchors;
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

    private String resolveImageGlyph(String assetId) {
        if (assetRegistry == null || assetId == null) {
            return "";
        }
        HudGlyph glyph = assetRegistry.getGlyph(assetId.toLowerCase(Locale.ROOT));
        return glyph == null ? "" : String.valueOf(glyph.codepoint());
    }

    private String resolveBarGlyph(Player player, String assetId) {
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
        List<HudGlyph> frames = assetRegistry.getBarFrames(assetId.toLowerCase(Locale.ROOT));
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
                for (int index = 1; index <= frames; index++) {
                    String texture = resolveFrameTexture(definition, index, resolver);
                    if (texture == null || texture.isBlank()) {
                        continue;
                    }
                    glyphs.add(createGlyph(allocator.next(), texture, sourceTextureRoot));
                }
                if (glyphs.isEmpty()) {
                    plugin.getLogger().warning("HUD bar '" + definition.getId() + "' has no frames configured.");
                }
                registry.registerBarFrames(definition.getId(), glyphs);
            } else {
                if (definition.getTexture() != null && !definition.getTexture().isBlank()) {
                    String texture = resolver.resolveSingle(definition.getTexture());
                    registry.registerGlyph(definition.getId(), createGlyph(allocator.next(), texture, sourceTextureRoot));
                } else {
                    plugin.getLogger().warning("HUD image '" + definition.getId() + "' is missing a texture path.");
                }
            }
        }
        return registry;
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
            if (bossBarDisplay != null) {
                bossBarDisplay.clear(event.getPlayer());
            }
            playerStates.remove(event.getPlayer().getUniqueId());
            placeholderCache.clear(event.getPlayer());
        }
    }
}
