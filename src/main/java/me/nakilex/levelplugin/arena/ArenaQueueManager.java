package me.nakilex.levelplugin.arena;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.arena.rating.ArenaRatingManager;
import me.nakilex.levelplugin.scoreboard.PlayerScoreboardManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Tracks players waiting for the arena queue. In addition to preserving join
 * order the manager captures rating snapshots so match making can be informed
 * by ELO and wait duration.
 */
public class ArenaQueueManager {
    private final ArenaRatingManager ratingManager;
    private final Map<UUID, QueueEntry> queue = new LinkedHashMap<>();

    private PlayerScoreboardManager scoreboardManager;
    private Predicate<UUID> matchCheck;
    private MatchHandler matchHandler;
    private Runnable queueListener;

    public ArenaQueueManager(ArenaRatingManager ratingManager) {
        this.ratingManager = ratingManager;
    }

    /** Add the player to the queue and trigger matchmaking when possible. */
    public QueueJoinResult join(Player player) {
        return join(player.getUniqueId());
    }

    /** Add a player by unique id. Useful for non-player contexts. */
    public QueueJoinResult join(UUID playerId) {
        if (queue.containsKey(playerId)) {
            return QueueJoinResult.ALREADY_QUEUED;
        }
        if (matchCheck != null && matchCheck.test(playerId)) {
            return QueueJoinResult.IN_MATCH;
        }
        QueueEntry entry = new QueueEntry(playerId,
                System.currentTimeMillis(),
                ratingManager.getSnapshot(playerId));
        insertEntry(entry);
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            updateScoreboard(player);
        }
        notifyQueueChange();
        attemptMatchmaking();
        return QueueJoinResult.JOINED;
    }

    /** Remove the player from the queue. */
    public boolean leave(UUID playerId) {
        QueueEntry removed = queue.remove(playerId);
        if (removed != null) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                updateScoreboard(player);
            }
            notifyQueueChange();
            return true;
        }
        return false;
    }

    /** Convenience overload accepting a {@link Player} instance. */
    public boolean leave(Player player) {
        return leave(player.getUniqueId());
    }

    /** Check whether the player is already queued. */
    public boolean isQueued(UUID playerId) {
        return queue.containsKey(playerId);
    }

    /** @return number of players currently queued. */
    public int getQueueSize() {
        return queue.size();
    }

    /**
     * Snapshot of the queue order. The returned list is immutable to prevent
     * callers from accidentally mutating the underlying map.
     */
    public List<UUID> getQueueSnapshot() {
        return Collections.unmodifiableList(new ArrayList<>(queue.keySet()));
    }

    /** Remove all queued players. Typically invoked on plugin shutdown. */
    public void clear() {
        List<UUID> previous = new ArrayList<>(queue.keySet());
        queue.clear();
        if (scoreboardManager != null) {
            for (UUID playerId : previous) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null) {
                    updateScoreboard(player);
                }
            }
        }
        notifyQueueChange();
    }

    /** Record the scoreboard manager so queue changes can trigger updates. */
    public void setScoreboardManager(PlayerScoreboardManager scoreboardManager) {
        this.scoreboardManager = scoreboardManager;
    }

    /** Allow callers to check if a player is locked in an active match. */
    public void setMatchCheck(Predicate<UUID> matchCheck) {
        this.matchCheck = matchCheck;
    }

    /** Register the matchmaking callback invoked when two players can fight. */
    public void setMatchHandler(MatchHandler matchHandler) {
        this.matchHandler = matchHandler;
    }

    /** Register a listener that fires whenever the queue contents change. */
    public void setQueueUpdateListener(Runnable listener) {
        this.queueListener = listener;
    }

    /** Requeue an existing entry (e.g. after a cancelled match setup). */
    public void requeue(QueueEntry entry) {
        if (entry == null) {
            return;
        }
        insertEntry(entry);
        Player player = Bukkit.getPlayer(entry.playerId());
        if (player != null) {
            updateScoreboard(player);
        }
        notifyQueueChange();
        attemptMatchmaking();
    }

    /** Return the elapsed time since the player joined the queue. */
    public Duration getWaitDuration(UUID playerId) {
        QueueEntry entry = queue.get(playerId);
        return entry == null ? Duration.ZERO : entry.waitDuration();
    }

    private void attemptMatchmaking() {
        if (matchHandler == null || queue.size() < 2) {
            return;
        }
        boolean matched;
        do {
            matched = false;
            List<QueueEntry> entries = new ArrayList<>(queue.values());
            outer:
            for (int i = 0; i < entries.size(); i++) {
                QueueEntry first = entries.get(i);
                int windowFirst = first.ratingSnapshot().matchWindow(first.waitDuration());
                QueueEntry best = null;
                int bestDiff = Integer.MAX_VALUE;
                for (int j = i + 1; j < entries.size(); j++) {
                    QueueEntry second = entries.get(j);
                    int diff = Math.abs(first.ratingSnapshot().rating() - second.ratingSnapshot().rating());
                    int allowed = Math.min(windowFirst, second.ratingSnapshot().matchWindow(second.waitDuration()));
                    if (diff <= allowed && diff < bestDiff) {
                        best = second;
                        bestDiff = diff;
                    }
                }
                if (best != null) {
                    queue.remove(first.playerId());
                    queue.remove(best.playerId());
                    Player p1 = Bukkit.getPlayer(first.playerId());
                    Player p2 = Bukkit.getPlayer(best.playerId());
                    if (p1 != null) updateScoreboard(p1);
                    if (p2 != null) updateScoreboard(p2);
                    notifyQueueChange();
                    matchHandler.handle(first, best);
                    matched = true;
                    break outer;
                }
            }
        } while (matched && queue.size() >= 2);
    }

    private void insertEntry(QueueEntry entry) {
        queue.remove(entry.playerId());
        LinkedHashMap<UUID, QueueEntry> ordered = new LinkedHashMap<>();
        boolean inserted = false;
        for (QueueEntry existing : queue.values()) {
            if (!inserted && existing.joinedAt() > entry.joinedAt()) {
                ordered.put(entry.playerId(), entry);
                inserted = true;
            }
            ordered.put(existing.playerId(), existing);
        }
        if (!inserted) {
            ordered.put(entry.playerId(), entry);
        }
        queue.clear();
        queue.putAll(ordered);
    }

    private void notifyQueueChange() {
        if (queueListener != null) {
            queueListener.run();
        }
        if (scoreboardManager != null) {
            for (UUID playerId : queue.keySet()) {
                Player queued = Bukkit.getPlayer(playerId);
                if (queued != null) {
                    updateScoreboard(queued);
                }
            }
        }
    }

    private void updateScoreboard(Player player) {
        if (player == null || scoreboardManager == null) {
            return;
        }
        Main plugin = Main.getInstance();
        if (plugin == null || !plugin.isEnabled()) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> scoreboardManager.updateBoard(player));
    }

    public enum QueueJoinResult {
        JOINED,
        ALREADY_QUEUED,
        IN_MATCH
    }

    @FunctionalInterface
    public interface MatchHandler {
        void handle(QueueEntry first, QueueEntry second);
    }

    public record QueueEntry(UUID playerId,
                             long joinedAt,
                             ArenaRatingManager.RatingSnapshot ratingSnapshot) {
        public QueueEntry {
            Objects.requireNonNull(playerId, "playerId");
            Objects.requireNonNull(ratingSnapshot, "ratingSnapshot");
        }

        public Duration waitDuration() {
            long elapsed = Math.max(0L, System.currentTimeMillis() - joinedAt);
            return Duration.ofMillis(elapsed);
        }
    }
}
