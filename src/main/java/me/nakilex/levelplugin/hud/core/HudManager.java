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
import org.bukkit.command.CommandSender;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
        this.assetRegistry = buildAssetRegistry(config.getImages());
        HudPackBuilder packBuilder = new HudPackBuilder(plugin);
        plugin.getLogger().info("HUD sourceTexturesFolder: " + config.getSourceTexturesFolder());
        plugin.getLogger().info("HUD imagesConfigPath: " + config.getImagesConfigPath());
        logTextureSample(config.getImages());
        List<String> missing = packBuilder.collectMissingTextures(config.getSourceTexturesFolder(), assetRegistry);
        if (!missing.isEmpty()) {
            plugin.getLogger().warning("HUD textures missing from source folder (" + missing.size() + "). Example: " + missing.get(0));
        }
        packBuilder.build(config.getOutputFolder(), config.getNamespace(), assetRegistry);
        int bossBarLines = config.isMergeBossBar() ? 1 : config.getBossbarLines();
        this.renderer = new HudBossBarRenderer(bossBarLines, config.getLineHeightPx(),
                config.getCanvasWidthPx(), config.isMergeBossBar());
        if (this.bossBarDisplay != null) {
            this.bossBarDisplay.clearAll();
        }
        this.bossBarDisplay = new HudBossBarDisplay(bossBarLines, BarColor.WHITE, BarStyle.SOLID);
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
                    HudResolvedElement resolved = resolveElement(player, element, placement);
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
        List<String> missing = packBuilder.collectMissingTextures(
                config.getSourceTexturesFolder(), assetRegistry);
        if (missing.isEmpty()) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.SUCCESS, "All HUD textures found in pack.");
            return;
        }
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.WARNING,
                "Missing HUD textures: " + missing.size());
        int limit = Math.min(5, missing.size());
        for (int i = 0; i < limit; i++) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.WARNING, "- " + missing.get(i));
        }
        if (missing.size() > limit) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.WARNING, "... and more");
        }
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
                for (HudElement element : layout.getElements()) {
                    if (!element.shouldRender(player, context)) {
                        continue;
                    }
                    HudResolvedElement resolvedElement = resolveElement(player, element, placement);
                    if (resolvedElement != null && !resolvedElement.getText().isBlank()) {
                        resolved.add(resolvedElement);
                    }
                }
            }
        }
        return new HudCanvas(resolved);
    }

    private HudResolvedElement resolveElement(Player player, HudElement element, HudLayoutPlacement placement) {
        String text = switch (element.getType()) {
            case TEXT -> placeholderService.resolve(player, element.getText());
            case IMAGE -> resolveImageGlyph(element.getAssetId());
            case BAR -> resolveBarGlyph(player, element.getAssetId());
        };
        if (text == null || text.isBlank()) {
            return null;
        }
        int x = placement.getOffsetX() + element.getX();
        int y = placement.getOffsetY() + element.getY();
        return new HudResolvedElement(element.getId(), text, x, y, element.getLayer(),
                element.getScale(), element.getAlign());
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
        List<HudGlyph> frames = assetRegistry.getBarFrames(assetId.toLowerCase(Locale.ROOT));
        if (frames.isEmpty()) {
            return "";
        }
        if (index < 0) {
            index = 0;
        } else if (index >= frames.size()) {
            index = frames.size() - 1;
        }
        return String.valueOf(frames.get(index).codepoint());
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

    private HudAssetRegistry buildAssetRegistry(Map<String, HudImageDefinition> images) {
        HudAssetRegistry registry = new HudAssetRegistry();
        HudGlyphAllocator allocator = new HudGlyphAllocator();
        for (HudImageDefinition definition : images.values()) {
            registry.registerDefinition(definition);
            if (definition.getType() == HudImageType.LISTENER) {
                int frames = definition.getSplit() > 0 ? definition.getSplit() + 1 : definition.getFrames().size();
                List<HudGlyph> glyphs = new ArrayList<>();
                for (int index = 0; index < frames; index++) {
                    String texture = resolveFrameTexture(definition, index);
                    if (texture == null || texture.isBlank()) {
                        continue;
                    }
                    glyphs.add(new HudGlyph(allocator.next(), texture));
                }
                if (glyphs.isEmpty()) {
                    plugin.getLogger().warning("HUD bar '" + definition.getId() + "' has no frames configured.");
                }
                registry.registerBarFrames(definition.getId(), glyphs);
            } else {
                if (definition.getTexture() != null && !definition.getTexture().isBlank()) {
                    registry.registerGlyph(definition.getId(), new HudGlyph(allocator.next(), definition.getTexture()));
                } else {
                    plugin.getLogger().warning("HUD image '" + definition.getId() + "' is missing a texture path.");
                }
            }
        }
        return registry;
    }

    private String resolveFrameTexture(HudImageDefinition definition, int index) {
        if (definition.getFrames().size() > index) {
            return definition.getFrames().get(index);
        }
        if (definition.getFrames().isEmpty() && definition.getSplit() > 0) {
            return definition.getTexture();
        }
        if (definition.getTexture() == null || definition.getTexture().isBlank()) {
            return "";
        }
        String base = definition.getTexture();
        String suffix = "_frame_" + index + ".png";
        if (base.endsWith(".png")) {
            base = base.substring(0, base.length() - 4);
        }
        return base + suffix;
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
