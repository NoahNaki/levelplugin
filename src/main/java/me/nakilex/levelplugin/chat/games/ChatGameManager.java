package me.nakilex.levelplugin.chat.games;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.StatType;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.utils.ChatFormatter;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/** Coordinates chat mini-games, handles rotation, and awards prizes. */
public class ChatGameManager {

    private final Main plugin;
    private final EconomyManager economyManager;
    private final LevelManager levelManager;
    private final StatsManager statsManager;
    private final ChatGamesConfig config;

    private final List<ChatGame> rotation = new ArrayList<>();
    private final Map<String, ChatGame> gamesById = new HashMap<>();

    private long rotationIntervalMinutes = 15L;
    private long rotationIntervalTicks = 15L * 60L * 20L;
    private BukkitTask rotationTask;
    private int index = -1;
    private volatile ChatGame activeGame;
    private volatile long activeGameStartMillis;

    public ChatGameManager(Main plugin,
                           EconomyManager economyManager,
                           LevelManager levelManager,
                           StatsManager statsManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.levelManager = levelManager;
        this.statsManager = statsManager;
        this.config = new ChatGamesConfig(plugin);
        registerGames();
        refreshRotationInterval();
    }

    private void registerGames() {
        register(new WordScrambleGame(config.getScrambleWords()));
        register(new TypeRacerGame(config.getTypeRacerPhrases()));
        register(new MathChallengeGame(config.getMathMinimum(), config.getMathMaximum(), config.getMathOperations()));
    }

    private void register(ChatGame game) {
        gamesById.put(game.getId().toLowerCase(Locale.ROOT), game);
        if (game.canPlay()) {
            rotation.add(game);
        } else {
            plugin.getLogger().warning(() -> "Chat game '" + game.getDisplayName() + "' is unavailable due to missing data.");
        }
    }

    /** Begin cycling through games. */
    public void start() {
        if (rotation.isEmpty()) {
            plugin.getLogger().warning("No chat games available to start. Check chat_games.yml");
            return;
        }
        refreshRotationInterval();
        startNextGame();
        scheduleRotationTask();
    }

    /** Cancel rotation and clear state. */
    public void stop() {
        if (rotationTask != null) {
            rotationTask.cancel();
            rotationTask = null;
        }
        ChatGame current = activeGame;
        activeGame = null;
        activeGameStartMillis = 0L;
        if (current != null) {
            current.stop(this);
        }
    }

    /** Reload game data and scheduler settings from disk. */
    public void reload() {
        Map<String, Boolean> previousStates = new HashMap<>();
        gamesById.forEach((id, game) -> previousStates.put(id, game.isEnabled()));

        stop();
        config.reload();
        rotation.clear();
        gamesById.clear();
        index = -1;
        registerGames();
        previousStates.forEach((id, enabled) -> {
            ChatGame game = gamesById.get(id);
            if (game != null) {
                game.setEnabled(enabled);
            }
        });
        refreshRotationInterval();
        if (rotation.isEmpty()) {
            plugin.getLogger().warning("No chat games available after reload. Check chat_games.yml");
            return;
        }
        start();
    }

    private void startNextGame() {
        ChatGame current = activeGame;
        if (current != null) {
            current.stop(this);
            activeGame = null;
            activeGameStartMillis = 0L;
        }
        if (rotation.isEmpty()) {
            return;
        }
        for (int attempts = 0; attempts < rotation.size(); attempts++) {
            index = (index + 1) % rotation.size();
            ChatGame candidate = rotation.get(index);
            if (!candidate.isEnabled() || !candidate.canPlay()) {
                continue;
            }
            activeGame = candidate;
            candidate.start(this);
            if (candidate.isRunning()) {
                activeGameStartMillis = System.currentTimeMillis();
                plugin.getLogger().info(() -> "Started chat game: " + candidate.getDisplayName());
                return;
            }
            activeGame = null;
        }
        plugin.getLogger().warning("All chat games are disabled or unavailable. Rotation paused.");
    }

    private void refreshRotationInterval() {
        long minutes = 15L;
        FileConfiguration cfg = plugin.getCustomConfig();
        if (cfg != null) {
            minutes = cfg.getLong("chat-games.interval-minutes", 15L);
        }
        if (minutes < 1L) {
            minutes = 1L;
        }
        rotationIntervalMinutes = minutes;
        long ticksPerMinute = 60L * 20L;
        long ticks;
        try {
            ticks = Math.multiplyExact(rotationIntervalMinutes, ticksPerMinute);
        } catch (ArithmeticException ex) {
            ticks = Long.MAX_VALUE;
        }
        if (ticks <= 0L) {
            ticks = 15L * ticksPerMinute;
        }
        if (ticks > Integer.MAX_VALUE) {
            ticks = Integer.MAX_VALUE;
        }
        rotationIntervalTicks = ticks;
    }

    private void scheduleRotationTask() {
        if (rotationTask != null) {
            rotationTask.cancel();
        }
        rotationTask = Bukkit.getScheduler().runTaskTimer(plugin, this::startNextGame,
                rotationIntervalTicks, rotationIntervalTicks);
    }

    /** Broadcast a formatted start banner to all players. */
    public void broadcastGameStart(ChatGame game, String... bodyLines) {
        List<String> lines = new ArrayList<>();
        lines.add(ChatColor.WHITE + "" + ChatColor.BOLD + "Chat Game: " + ChatColor.AQUA + game.getDisplayName());
        for (String body : bodyLines) {
            lines.add(body);
        }
        lines.add(ChatColor.GRAY + "First correct answer wins!");
        String[] payload = lines.toArray(String[]::new);
        for (Player player : Bukkit.getOnlinePlayers()) {
            ChatFormatter.sendBoxedCenteredMessages(player, "§b", payload);
        }
    }

    /** Handle async chat events. */
    public void handleChatAsync(Player player, String message) {
        ChatGame game = activeGame;
        if (game == null || !game.isRunning()) {
            return;
        }
        Optional<ChatGameResult> result = game.handleChat(player, message);
        result.ifPresent(res -> Bukkit.getScheduler().runTask(plugin, () -> concludeGame(game, res)));
    }

    private void concludeGame(ChatGame game, ChatGameResult result) {
        if (activeGame != game) {
            return;
        }
        game.stop(this);
        activeGame = null;
        long durationMillis = Math.max(0L, System.currentTimeMillis() - activeGameStartMillis);
        activeGameStartMillis = 0L;
        awardReward(result);
        announceWinner(game, result, durationMillis);
    }

    private void awardReward(ChatGameResult result) {
        ChatGameReward reward = result.reward();
        if (reward == null || reward.isEmpty()) {
            return;
        }
        UUID winnerId = result.winnerId();
        if (reward.coins() > 0 && economyManager != null) {
            economyManager.addCoins(winnerId, reward.coins());
        }
        if (reward.experience() > 0 && levelManager != null) {
            levelManager.addXP(winnerId, reward.experience());
        }
        if (reward.intellect() > 0) {
            statsManager.addBaseStat(winnerId, StatType.INT, reward.intellect());
        }
    }

    private void announceWinner(ChatGame game, ChatGameResult result, long durationMillis) {
        String winnerName = result.winnerName();
        String header = ChatColor.WHITE + "" + ChatColor.BOLD + "Chat Game: " + ChatColor.AQUA + game.getDisplayName();
        double seconds = durationMillis / 1000.0;
        String formattedSeconds = String.format(Locale.US, "%.2f", seconds);
        ChatGameReward reward = result.reward();
        String rewardSummary = reward != null ? formatReward(reward) : "";
        String summary = ChatColor.LIGHT_PURPLE + winnerName + ChatColor.GRAY + " answered in "
                + ChatColor.LIGHT_PURPLE + formattedSeconds;
        if (!rewardSummary.isEmpty()) {
            summary += ChatColor.GRAY + " and got " + rewardSummary + ChatColor.GRAY + "!";
        } else {
            summary += ChatColor.GRAY + "!";
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            ChatFormatter.sendCenteredMessage(player, header);
            ChatFormatter.sendCenteredMessage(player, " ");
            ChatFormatter.sendCenteredMessage(player, summary);
        }
        if (result.solution() != null) {
            ChatMessageUtil.broadcast(MessageType.INFO,
                    ChatColor.GRAY + "Answer: " + ChatColor.AQUA + result.solution());
        }
    }

    private String formatReward(ChatGameReward reward) {
        List<String> parts = new ArrayList<>();
        if (reward.coins() > 0) {
            parts.add(ChatColor.GOLD + String.valueOf(reward.coins()) + ChatColor.GRAY + " coins");
        }
        if (reward.experience() > 0) {
            String xpColor = ChatFormatter.experienceColor();
            parts.add(xpColor + reward.experience() + ChatColor.GRAY + " XP");
        }
        if (reward.intellect() > 0) {
            parts.add(ChatColor.AQUA + "+" + reward.intellect() + " Intelligence");
        }
        return String.join(ChatColor.GRAY + ", ", parts);
    }

    /** Toggle a game's enabled state via debug command. */
    public boolean setGameEnabled(String id, boolean enabled) {
        if (id == null) return false;
        ChatGame game = gamesById.get(id.toLowerCase(Locale.ROOT));
        if (game == null) {
            return false;
        }
        game.setEnabled(enabled);
        if (!enabled && activeGame == game) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (activeGame != game) {
                    return;
                }
                game.stop(this);
                activeGame = null;
                activeGameStartMillis = 0L;
                ChatMessageUtil.broadcast(MessageType.WARNING,
                        ChatColor.YELLOW + game.getDisplayName() + ChatColor.GRAY + " has been disabled.");
                startNextGame();
            });
        }
        return true;
    }

    /** Snapshot of all registered games for debugging UI. */
    public List<ChatGameStatus> getStatuses() {
        return gamesById.values().stream()
                .map(game -> new ChatGameStatus(game.getId(), game.getDisplayName(), game.isEnabled(), game.canPlay()))
                .sorted(Comparator.comparing(ChatGameStatus::id))
                .toList();
    }

    public ChatGame getActiveGame() {
        return activeGame;
    }
}
