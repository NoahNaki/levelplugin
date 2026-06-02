package me.nakilex.levelplugin.player.fishing.minigame;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.fishing.data.FishDefinition;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/** Selects games and routes player input to the one active game per player. */
public class FishingMiniGameManager {
    private static final List<String> SUPPORTED_TYPES = List.of("click", "click_v2", "hold", "tension", "dance", "accurate_click");
    private static FishingMiniGameManager instance;

    private final Main plugin;
    private final Map<UUID, FishingMiniGameSession> activeGames = new HashMap<>();

    public FishingMiniGameManager(Main plugin) {
        this.plugin = plugin;
        instance = this;
    }

    public static FishingMiniGameManager getInstance() { return instance; }
    public static List<String> supportedTypes() { return SUPPORTED_TYPES; }

    public void startRandom(Player player, FishHook hook, boolean inLava, FishDefinition hookedFish,
                            FishingDifficultyProfile profile, DebugContext debugContext,
                            Consumer<Boolean> completion) {
        List<String> enabled = new ArrayList<>(plugin.getConfig().getStringList("fishing-mini-games.enabled"));
        enabled.removeIf(type -> !isSupportedType(type));
        if (enabled.isEmpty()) enabled.addAll(SUPPORTED_TYPES);
        start(player, enabled.get(ThreadLocalRandom.current().nextInt(enabled.size())), hook, inLava,
                hookedFish, profile, debugContext, completion);
    }

    /** Starts a named game for admin testing while reusing the normal lifecycle and cleanup path. */
    public boolean start(Player player, String type, FishingDifficultyProfile profile, Consumer<Boolean> completion) {
        return start(player, type, null, false, null, profile, null, completion);
    }

    private boolean start(Player player, String type, FishHook hook, boolean inLava, FishDefinition hookedFish,
                          FishingDifficultyProfile profile, DebugContext debugContext,
                          Consumer<Boolean> completion) {
        String normalizedType = type.toLowerCase(Locale.ROOT);
        if (!isSupportedType(normalizedType)) return false;
        FishingDifficultyProfile safeProfile = profile == null ? FishingDifficultyProfile.normal() : profile;
        UUID uuid = player.getUniqueId();
        cancel(uuid);
        long durationMs = resolveDurationMs(normalizedType, safeProfile);
        long startedAtMs = System.currentTimeMillis();
        FishingMiniGameSession.Store store = new FishingMiniGameSession.Store(uuid, hook, inLava, hookedFish,
                safeProfile, startedAtMs, startedAtMs + durationMs);
        FishingMiniGame game = create(normalizedType, player, durationMs, safeProfile, success -> {
            FishingMiniGameSession completed = activeGames.remove(uuid);
            cleanupHook(completed != null ? completed.store().hook() : hook);
            completion.accept(success);
        });
        activeGames.put(uuid, new FishingMiniGameSession(game, store));
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                ChatColor.AQUA + "A fish is hooked! " + ChatColor.GRAY + "Complete the mini-game to reel it in.");
        logDebug(player, normalizedType, durationMs, safeProfile, debugContext);
        game.start();
        return true;
    }

    private long resolveDurationMs(String type, FishingDifficultyProfile profile) {
        FileConfiguration config = plugin.getConfig();
        String path = "fishing-mini-games." + type + ".duration-ms";
        String tierPath = path + "." + profile.tier().name().toLowerCase(Locale.ROOT);
        if (config.isLong(tierPath) || config.isInt(tierPath)) {
            double configuredDuration = config.getLong(tierPath);
            double baseTierMultiplier = FishingDifficultyProfile.forTier(profile.tier()).durationMultiplier();
            return Math.max(1000L, Math.round(configuredDuration * profile.durationMultiplier() / baseTierMultiplier));
        }
        return Math.max(1000L, Math.round(config.getLong(path, 5000L) * profile.durationMultiplier()));
    }

    private void logDebug(Player player, String type, long durationMs, FishingDifficultyProfile profile, DebugContext context) {
        if (!debugEnabled()) return;
        String fishId = context == null ? "test" : context.fishId();
        String rarity = context == null ? "N/A" : context.rarity();
        int fishingLevel = context == null ? -1 : context.fishingLevel();
        plugin.getLogger().info("Fishing mini-game debug: player=" + player.getName()
                + ", hookedFish=" + fishId + ", rarity=" + rarity + ", fishingLevel=" + fishingLevel
                + ", miniGame=" + type + ", difficulty=" + profile.tier() + ", configuredDurationMs=" + durationMs);
    }

    public void logVanillaState(Player player, String message) {
        if (debugEnabled()) plugin.getLogger().info("Fishing mini-game debug: player=" + player.getName() + ", " + message);
    }

    private boolean debugEnabled() {
        return plugin.getConfig().getBoolean("fishing-mini-games.debug", false);
    }

    public static boolean isSupportedType(String type) {
        return type != null && SUPPORTED_TYPES.contains(type.toLowerCase(Locale.ROOT));
    }

    private FishingMiniGame create(String type, Player player, long durationMs, FishingDifficultyProfile profile,
                                   Consumer<Boolean> completion) {
        FileConfiguration config = plugin.getConfig();
        return switch (type) {
            case "hold" -> new HoldFishingMiniGame(plugin, player, durationMs, config, profile, completion);
            case "click_v2" -> new ProgressClickFishingMiniGame(plugin, player, durationMs, config, profile, completion);
            case "tension" -> new TensionFishingMiniGame(plugin, player, durationMs, config, profile, completion);
            case "dance" -> new DanceFishingMiniGame(plugin, player, durationMs, config, profile, completion);
            case "accurate_click" -> new AccurateClickFishingMiniGame(plugin, player, durationMs, config, profile, completion);
            default -> new ClickFishingMiniGame(plugin, player, durationMs, config, profile, completion);
        };
    }

    public boolean isPlaying(UUID uuid) { return activeGames.containsKey(uuid); }
    public FishingMiniGameSession.Store getStore(UUID uuid) {
        FishingMiniGameSession session = activeGames.get(uuid);
        return session == null ? null : session.store();
    }
    public void click(UUID uuid) { FishingMiniGame game = getGame(uuid); if (game != null) game.handleClick(); }
    public void rightClick(UUID uuid) { FishingMiniGame game = getGame(uuid); if (game != null && game.usesRightClickInput()) game.handleRightClick(); }
    public void sneak(UUID uuid, boolean sneaking) { FishingMiniGame game = getGame(uuid); if (game != null) game.handleSneak(sneaking); }
    public void move(UUID uuid, FishingMiniGame.Movement movement) { FishingMiniGame game = getGame(uuid); if (game != null) game.handleMovement(movement); }
    public void cancel(UUID uuid) {
        FishingMiniGameSession session = activeGames.remove(uuid);
        if (session == null) return;
        cleanupHook(session.store().hook());
        if (!session.game().isFinished()) session.game().cancel();
    }
    public void shutdown() { new ArrayList<>(activeGames.keySet()).forEach(this::cancel); }

    private FishingMiniGame getGame(UUID uuid) {
        FishingMiniGameSession session = activeGames.get(uuid);
        return session == null ? null : session.game();
    }

    private void cleanupHook(FishHook hook) {
        if (hook != null && hook.isValid()) hook.remove();
    }

    public record DebugContext(String fishId, String rarity, int fishingLevel) { }
}
