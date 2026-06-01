package me.nakilex.levelplugin.player.fishing.minigame;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
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
    private final Map<UUID, FishingMiniGame> activeGames = new HashMap<>();

    public FishingMiniGameManager(Main plugin) {
        this.plugin = plugin;
        instance = this;
    }

    public static FishingMiniGameManager getInstance() { return instance; }
    public static List<String> supportedTypes() { return SUPPORTED_TYPES; }

    public void startRandom(Player player, FishingDifficultyProfile profile, Consumer<Boolean> completion) {
        startRandom(player, profile, null, completion);
    }

    public void startRandom(Player player, FishingDifficultyProfile profile, DebugContext debugContext,
                            Consumer<Boolean> completion) {
        List<String> enabled = new ArrayList<>(plugin.getConfig().getStringList("fishing-mini-games.enabled"));
        enabled.removeIf(type -> !isSupportedType(type));
        if (enabled.isEmpty()) enabled.addAll(SUPPORTED_TYPES);
        start(player, enabled.get(ThreadLocalRandom.current().nextInt(enabled.size())), profile, debugContext, completion);
    }

    /** Starts a named game for admin testing while reusing the normal lifecycle and cleanup path. */
    public boolean start(Player player, String type, FishingDifficultyProfile profile, Consumer<Boolean> completion) {
        return start(player, type, profile, null, completion);
    }

    private boolean start(Player player, String type, FishingDifficultyProfile profile, DebugContext debugContext,
                          Consumer<Boolean> completion) {
        String normalizedType = type.toLowerCase(Locale.ROOT);
        if (!isSupportedType(normalizedType)) return false;
        FishingDifficultyProfile safeProfile = profile == null ? FishingDifficultyProfile.normal() : profile;
        UUID uuid = player.getUniqueId();
        cancel(uuid);
        FishingMiniGame game = create(normalizedType, player, safeProfile, success -> {
            activeGames.remove(uuid);
            completion.accept(success);
        });
        activeGames.put(uuid, game);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                ChatColor.AQUA + "A fish is hooked! " + ChatColor.GRAY + "Complete the mini-game to reel it in.");
        logDebug(player, normalizedType, safeProfile, debugContext);
        game.start();
        return true;
    }

    private void logDebug(Player player, String type, FishingDifficultyProfile profile, DebugContext context) {
        if (!plugin.getConfig().getBoolean("fishing-mini-games.debug", false)) return;
        String fishId = context == null ? "test" : context.fishId();
        String rarity = context == null ? "N/A" : context.rarity();
        int fishingLevel = context == null ? -1 : context.fishingLevel();
        plugin.getLogger().info("Fishing mini-game debug: player=" + player.getName()
                + ", hookedFish=" + fishId + ", rarity=" + rarity + ", fishingLevel=" + fishingLevel
                + ", miniGame=" + type + ", difficulty=" + profile.tier());
    }

    public static boolean isSupportedType(String type) {
        return type != null && SUPPORTED_TYPES.contains(type.toLowerCase(Locale.ROOT));
    }

    private FishingMiniGame create(String type, Player player, FishingDifficultyProfile profile, Consumer<Boolean> completion) {
        FileConfiguration config = plugin.getConfig();
        long duration = Math.max(1000L, Math.round(config.getLong("fishing-mini-games." + type + ".duration-ms", 5000L)
                * profile.durationMultiplier()));
        return switch (type) {
            case "hold" -> new HoldFishingMiniGame(plugin, player, duration, config, profile, completion);
            case "click_v2" -> new ProgressClickFishingMiniGame(plugin, player, duration, config, profile, completion);
            case "tension" -> new TensionFishingMiniGame(plugin, player, duration, config, profile, completion);
            case "dance" -> new DanceFishingMiniGame(plugin, player, duration, config, profile, completion);
            case "accurate_click" -> new AccurateClickFishingMiniGame(plugin, player, duration, config, profile, completion);
            default -> new ClickFishingMiniGame(plugin, player, duration, config, profile, completion);
        };
    }

    public boolean isPlaying(UUID uuid) { return activeGames.containsKey(uuid); }
    public void click(UUID uuid) { FishingMiniGame game = activeGames.get(uuid); if (game != null) game.handleClick(); }
    public void rightClick(UUID uuid) { FishingMiniGame game = activeGames.get(uuid); if (game != null) game.handleRightClick(); }
    public void sneak(UUID uuid, boolean sneaking) { FishingMiniGame game = activeGames.get(uuid); if (game != null) game.handleSneak(sneaking); }
    public void move(UUID uuid, FishingMiniGame.Movement movement) { FishingMiniGame game = activeGames.get(uuid); if (game != null) game.handleMovement(movement); }
    public void cancel(UUID uuid) { FishingMiniGame game = activeGames.remove(uuid); if (game != null && !game.isFinished()) game.cancel(); }
    public void shutdown() { new ArrayList<>(activeGames.keySet()).forEach(this::cancel); }

    public record DebugContext(String fishId, String rarity, int fishingLevel) { }
}
