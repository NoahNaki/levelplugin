package me.nakilex.levelplugin.player.fishing.minigame;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/** Selects games and routes player input to the one active game per player. */
public class FishingMiniGameManager {
    private final Main plugin;
    private final Map<UUID, FishingMiniGame> activeGames = new HashMap<>();

    public FishingMiniGameManager(Main plugin) { this.plugin = plugin; }

    public void startRandom(Player player, double durationMultiplier, Consumer<Boolean> completion) {
        UUID uuid = player.getUniqueId();
        cancel(uuid);
        List<String> enabled = new ArrayList<>(plugin.getConfig().getStringList("fishing-mini-games.enabled"));
        if (enabled.isEmpty()) enabled.addAll(List.of("click", "click_v2", "hold", "tension", "dance", "accurate_click"));
        String type = enabled.get(ThreadLocalRandom.current().nextInt(enabled.size())).toLowerCase();
        FishingMiniGame game = create(type, player, durationMultiplier, success -> {
            activeGames.remove(uuid);
            completion.accept(success);
        });
        activeGames.put(uuid, game);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                ChatColor.AQUA + "A fish is hooked! " + ChatColor.GRAY + "Complete the mini-game to reel it in.");
        game.start();
    }

    private FishingMiniGame create(String type, Player player, double multiplier, Consumer<Boolean> completion) {
        FileConfiguration config = plugin.getConfig();
        long duration = Math.max(1000L, Math.round(config.getLong("fishing-mini-games." + type + ".duration-ms", 5000L) * multiplier));
        return switch (type) {
            case "hold" -> new HoldFishingMiniGame(plugin, player, duration, config, completion);
            case "click_v2" -> new ProgressClickFishingMiniGame(plugin, player, duration, config, completion);
            case "tension" -> new TensionFishingMiniGame(plugin, player, duration, config, completion);
            case "dance" -> new DanceFishingMiniGame(plugin, player, duration, config, completion);
            case "accurate_click" -> new AccurateClickFishingMiniGame(plugin, player, duration, config, completion);
            default -> new ClickFishingMiniGame(plugin, player, duration, config, completion);
        };
    }

    public boolean isPlaying(UUID uuid) { return activeGames.containsKey(uuid); }
    public void click(UUID uuid) { FishingMiniGame game = activeGames.get(uuid); if (game != null) game.handleClick(); }
    public void sneak(UUID uuid, boolean sneaking) { FishingMiniGame game = activeGames.get(uuid); if (game != null) game.handleSneak(sneaking); }
    public void move(UUID uuid, FishingMiniGame.Movement movement) { FishingMiniGame game = activeGames.get(uuid); if (game != null) game.handleMovement(movement); }
    public void cancel(UUID uuid) { FishingMiniGame game = activeGames.remove(uuid); if (game != null && !game.isFinished()) game.cancel(); }
    public void shutdown() { new ArrayList<>(activeGames.keySet()).forEach(this::cancel); }
}
