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
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/** Coordinates chat mini-games, handles rotation, and awards prizes. */
public class ChatGameManager {

    private static final long ROTATION_TICKS = 15L * 60L * 20L; // 15 minutes

    private final Main plugin;
    private final EconomyManager economyManager;
    private final LevelManager levelManager;
    private final StatsManager statsManager;
    private final ChatGamesConfig config;

    private final List<ChatGame> rotation = new ArrayList<>();
    private final Map<String, ChatGame> gamesById = new HashMap<>();

    private BukkitTask rotationTask;
    private int index = -1;
    private volatile ChatGame activeGame;

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
        startNextGame();
        rotationTask = Bukkit.getScheduler().runTaskTimer(plugin, this::startNextGame, ROTATION_TICKS, ROTATION_TICKS);
    }

    /** Cancel rotation and clear state. */
    public void stop() {
        if (rotationTask != null) {
            rotationTask.cancel();
            rotationTask = null;
        }
        ChatGame current = activeGame;
        activeGame = null;
        if (current != null) {
            current.stop(this);
        }
    }

    private void startNextGame() {
        ChatGame current = activeGame;
        if (current != null) {
            current.stop(this);
            activeGame = null;
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
                plugin.getLogger().info(() -> "Started chat game: " + candidate.getDisplayName());
                return;
            }
            activeGame = null;
        }
        plugin.getLogger().warning("All chat games are disabled or unavailable. Rotation paused.");
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
        awardReward(result);
        announceWinner(game, result);
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
        Player winner = Bukkit.getPlayer(winnerId);
        if (winner != null) {
            ChatMessageUtil.send(winner, MessageType.REWARD,
                    ChatColor.GRAY + "Rewards: " + formatReward(reward));
        }
    }

    private void announceWinner(ChatGame game, ChatGameResult result) {
        String winnerName = result.winnerName();
        ChatMessageUtil.broadcast(MessageType.SUCCESS,
                winnerName + ChatColor.GRAY + " solved " + ChatColor.AQUA + game.getDisplayName() + ChatColor.GRAY + "!" );
        if (result.solution() != null) {
            ChatMessageUtil.broadcast(MessageType.INFO,
                    ChatColor.GRAY + "Answer: " + ChatColor.AQUA + result.solution());
        }
        ChatGameReward reward = result.reward();
        if (reward != null && !reward.isEmpty()) {
            ChatMessageUtil.broadcast(MessageType.REWARD,
                    winnerName + ChatColor.GRAY + " earned " + formatReward(reward));
        }
        ChatMessageUtil.broadcast(MessageType.INFO, ChatColor.GRAY + "Next chat game begins in 15 minutes.");
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
