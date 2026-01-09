package me.nakilex.levelplugin.hud.core;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.hud.config.HudConfig;
import me.nakilex.levelplugin.hud.config.HudConfigLoader;
import me.nakilex.levelplugin.hud.input.HudInputListener;
import me.nakilex.levelplugin.hud.placeholders.HudPlaceholderCache;
import me.nakilex.levelplugin.hud.placeholders.HudPlaceholderRegistry;
import me.nakilex.levelplugin.hud.placeholders.HudPlaceholderService;
import me.nakilex.levelplugin.hud.render.HudRenderer;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HudManager {
    private final Main plugin;
    private final HudConfigLoader configLoader;
    private final HudRenderer renderer = new HudRenderer();
    private final HudPlaceholderCache placeholderCache;
    private final HudPlaceholderService placeholderService;
    private final HudInputListener inputListener = new HudInputListener();
    private final Map<UUID, HudPlayerState> playerStates = new ConcurrentHashMap<>();
    private final Set<UUID> disabledPlayers = ConcurrentHashMap.newKeySet();
    private BukkitTask task;
    private HudConfig config;

    public HudManager(Main plugin) {
        this.plugin = plugin;
        this.configLoader = new HudConfigLoader(plugin);
        this.placeholderCache = new HudPlaceholderCache(150);
        this.placeholderService = new HudPlaceholderService(new HudPlaceholderRegistry(), placeholderCache);
    }

    public void enable() {
        reload();
        Bukkit.getPluginManager().registerEvents(inputListener, plugin);
    }

    public void disable() {
        stopTask();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendActionBar("");
        }
        playerStates.clear();
        disabledPlayers.clear();
        placeholderCache.clearAll();
    }

    public void reload() {
        this.config = configLoader.load();
        placeholderCache.setTtlMs(config.getPlaceholderCacheTtlMs());
        stopTask();
        startTask();
        placeholderCache.clearAll();
        plugin.getLogger().info("HUD config reloaded.");
    }

    public void toggle(Player player) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        if (disabledPlayers.remove(playerId)) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS, "HUD enabled.");
        } else {
            disabledPlayers.add(playerId);
            player.sendActionBar("");
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "HUD disabled.");
        }
    }

    public void debug(Player player) {
        if (player == null) {
            return;
        }
        HudLayout layout = getLayout();
        if (layout == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "No HUD layout configured.");
            return;
        }
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO, "HUD layout: " + layout.getId());
        List<String> lines = renderer.describe(player, layout, placeholderService);
        for (String line : lines) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO, line);
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
        HudLayout layout = getLayout();
        if (layout == null) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (disabledPlayers.contains(player.getUniqueId())) {
                continue;
            }
            updatePlayer(player, layout);
        }
    }

    private void updatePlayer(Player player, HudLayout layout) {
        String rendered = renderer.render(player, layout, placeholderService);
        HudPlayerState state = playerStates.computeIfAbsent(player.getUniqueId(), id -> new HudPlayerState());
        if (!rendered.equals(state.getLastRendered())) {
            player.sendActionBar(rendered);
            state.setLastRendered(rendered);
        }
    }

    private HudLayout getLayout() {
        if (config == null) {
            return null;
        }
        String layoutId = config.getDefaultLayout();
        HudLayout layout = config.getLayouts().get(layoutId);
        if (layout != null) {
            return layout;
        }
        return config.getLayouts().values().stream().findFirst().orElse(null);
    }
}
