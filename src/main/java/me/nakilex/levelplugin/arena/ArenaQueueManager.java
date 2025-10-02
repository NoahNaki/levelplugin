package me.nakilex.levelplugin.arena;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.arena.rating.ArenaRatingManager;
import me.nakilex.levelplugin.party.Party;
import me.nakilex.levelplugin.party.PartyManager;
import me.nakilex.levelplugin.party.PartyMembershipListener;
import me.nakilex.levelplugin.scoreboard.PlayerScoreboardManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;

/**
 * Tracks players and parties waiting in the arena queues. Supports multiple
 * modes (1v1, 2v2) and coordinates matchmaking callbacks when compatible
 * opponents are found.
 */
public class ArenaQueueManager implements PartyMembershipListener {
    private final ArenaRatingManager ratingManager;
    private final PartyManager partyManager;
    private final Map<ArenaMode, LinkedHashMap<UUID, QueueEntry>> queues = new EnumMap<>(ArenaMode.class);
    private final Map<UUID, QueueEntry> entriesById = new HashMap<>();
    private final Map<UUID, UUID> playerToEntry = new HashMap<>();
    private final Map<ArenaMode, MatchHandler> matchHandlers = new EnumMap<>(ArenaMode.class);
    private final List<Predicate<UUID>> matchChecks = new ArrayList<>();

    private PlayerScoreboardManager scoreboardManager;
    private Runnable queueListener;

    public ArenaQueueManager(ArenaRatingManager ratingManager, PartyManager partyManager) {
        this.ratingManager = ratingManager;
        this.partyManager = partyManager;
        for (ArenaMode mode : ArenaMode.values()) {
            queues.put(mode, new LinkedHashMap<>());
        }
        partyManager.addMembershipListener(this);
    }

    /**
     * Join the default (1v1) queue.
     */
    public QueueJoinOutcome join(Player player) {
        return join(player, ArenaMode.ONE_VS_ONE);
    }

    /**
     * Join the queue for the specified mode. Handles party validation for
     * modes that require more than one player.
     */
    public QueueJoinOutcome join(Player player, ArenaMode mode) {
        if (mode == ArenaMode.TWO_VS_TWO) {
            return joinPartyQueue(player);
        }
        return joinSoloQueue(player.getUniqueId());
    }

    private QueueJoinOutcome joinSoloQueue(UUID playerId) {
        if (playerToEntry.containsKey(playerId)) {
            return QueueJoinOutcome.of(QueueJoinResult.ALREADY_QUEUED);
        }
        if (isInMatch(playerId)) {
            return QueueJoinOutcome.of(QueueJoinResult.IN_MATCH);
        }

        QueueEntry entry = createEntry(ArenaMode.ONE_VS_ONE, List.of(playerId));
        insertEntry(entry);
        notifyQueueChange();
        updateScoreboard(Bukkit.getPlayer(playerId));
        attemptMatchmaking();
        return QueueJoinOutcome.of(QueueJoinResult.JOINED);
    }

    private QueueJoinOutcome joinPartyQueue(Player initiator) {
        UUID playerId = initiator.getUniqueId();
        Party party = partyManager.getParty(playerId);
        if (party == null) {
            return new QueueJoinOutcome(QueueJoinResult.PARTY_REQUIRED,
                    ChatColor.RED + "You need a party of two to join the 2v2 queue.");
        }

        List<UUID> members = new ArrayList<>(party.getMembers());
        if (members.size() != ArenaMode.TWO_VS_TWO.teamSize()) {
            return new QueueJoinOutcome(QueueJoinResult.PARTY_SIZE_INVALID,
                    ChatColor.RED + "Your party must contain exactly 2 members to queue for 2v2.");
        }

        ArenaRatingManager.RatingCategory category = ArenaMode.TWO_VS_TWO.ratingCategory();
        Map<UUID, ArenaRatingManager.RatingSnapshot> ratingSnapshots = loadSnapshots(members, category);
        int minOrdinal = Integer.MAX_VALUE;
        int maxOrdinal = Integer.MIN_VALUE;
        ArenaRatingManager.RatingTier highestTier = null;
        ArenaRatingManager.RatingTier lowestTier = null;
        for (ArenaRatingManager.RatingSnapshot snapshot : ratingSnapshots.values()) {
            ArenaRatingManager.RatingTier tier = ratingManager.getTier(snapshot.rating());
            int ordinal = tier.ordinal();
            if (ordinal < minOrdinal) {
                minOrdinal = ordinal;
                highestTier = tier;
            }
            if (ordinal > maxOrdinal) {
                maxOrdinal = ordinal;
                lowestTier = tier;
            }
        }
        if (highestTier != null && lowestTier != null && maxOrdinal - minOrdinal >= 2) {
            String message = ChatColor.RED + "Party members must be within one arena tier ("
                    + highestTier.color() + highestTier.displayName() + ChatColor.RED + " vs "
                    + lowestTier.color() + lowestTier.displayName() + ChatColor.RED + ").";
            return new QueueJoinOutcome(QueueJoinResult.RANK_GAP_TOO_LARGE, message);
        }

        for (UUID member : members) {
            Player teammate = Bukkit.getPlayer(member);
            if (teammate == null || !teammate.isOnline()) {
                String name = teammate != null ? teammate.getName() : Bukkit.getOfflinePlayer(member).getName();
                if (name == null) name = ChatColor.DARK_GRAY + "Unknown";
                return new QueueJoinOutcome(QueueJoinResult.PARTY_MEMBER_OFFLINE,
                        ChatColor.RED + name + ChatColor.GRAY + " must be online to join the 2v2 queue.");
            }
        }

        for (UUID member : members) {
            if (playerToEntry.containsKey(member)) {
                String name = Bukkit.getOfflinePlayer(member).getName();
                if (name == null) name = "Unknown";
                return new QueueJoinOutcome(QueueJoinResult.TEAM_MEMBER_QUEUED,
                        ChatColor.RED + name + ChatColor.GRAY + " is already in an arena queue.");
            }
            if (isInMatch(member)) {
                String name = Bukkit.getOfflinePlayer(member).getName();
                if (name == null) name = "Unknown";
                return new QueueJoinOutcome(QueueJoinResult.TEAM_MEMBER_IN_MATCH,
                        ChatColor.RED + name + ChatColor.GRAY + " is currently fighting in the arena.");
            }
        }

        QueueEntry entry = createEntry(ArenaMode.TWO_VS_TWO, members, ratingSnapshots);
        insertEntry(entry);
        notifyQueueChange();
        for (UUID member : members) {
            updateScoreboard(Bukkit.getPlayer(member));
        }
        attemptMatchmaking();
        return QueueJoinOutcome.of(QueueJoinResult.JOINED);
    }

    /** Remove the player (and their team) from the queue. */
    public boolean leave(UUID playerId) {
        return leave(playerId, LeaveReason.PLAYER_REQUEST);
    }

    public boolean leave(UUID playerId, LeaveReason reason) {
        UUID entryId = playerToEntry.get(playerId);
        if (entryId == null) {
            return false;
        }
        QueueEntry entry = removeEntryInternal(entryId);
        if (entry != null) {
            notifyQueueChange();
            if (entry.mode() == ArenaMode.TWO_VS_TWO) {
                Player actor = Bukkit.getPlayer(playerId);
                String actorName = actor != null ? actor.getName() : Bukkit.getOfflinePlayer(playerId).getName();
                String message;
                if (reason == LeaveReason.DISCONNECT) {
                    message = ChatColor.RED + (actorName == null ? "A party member" : actorName)
                            + ChatColor.GRAY + " disconnected. 2v2 queue cancelled.";
                } else if (reason == LeaveReason.PLAYER_REQUEST) {
                    message = ChatColor.RED + "Your party left the 2v2 queue.";
                } else {
                    message = ChatColor.RED + "2v2 queue cancelled.";
                }
                sendMessage(entry, MessageType.WARNING, message, playerId);
            }
            return true;
        }
        return false;
    }

    /** Check whether the player is currently queued in any mode. */
    public boolean isQueued(UUID playerId) {
        return playerToEntry.containsKey(playerId);
    }

    /** Get the mode the player is queued for, if any. */
    public Optional<ArenaMode> getMode(UUID playerId) {
        QueueEntry entry = getEntry(playerId).orElse(null);
        return entry == null ? Optional.empty() : Optional.of(entry.mode());
    }

    /** Return the number of players queued in the default (1v1) mode. */
    public int getQueueSize() {
        return getQueuePopulation(ArenaMode.ONE_VS_ONE);
    }

    public int getQueuePopulation(ArenaMode mode) {
        LinkedHashMap<UUID, QueueEntry> queue = queues.get(mode);
        if (queue == null) return 0;
        int total = 0;
        for (QueueEntry entry : queue.values()) {
            total += entry.members().size();
        }
        return total;
    }

    public List<UUID> getQueueSnapshot() {
        LinkedHashMap<UUID, QueueEntry> queue = queues.get(ArenaMode.ONE_VS_ONE);
        if (queue == null) {
            return List.of();
        }
        List<UUID> snapshot = new ArrayList<>();
        for (QueueEntry entry : queue.values()) {
            snapshot.add(entry.members().get(0));
        }
        return Collections.unmodifiableList(snapshot);
    }

    /** Remove all queued players. */
    public void clear() {
        List<QueueEntry> removed = new ArrayList<>(entriesById.values());
        for (QueueEntry entry : removed) {
            removeEntryInternal(entry.entryId());
        }
        notifyQueueChange();
    }

    public void setScoreboardManager(PlayerScoreboardManager scoreboardManager) {
        this.scoreboardManager = scoreboardManager;
    }

    public void setQueueUpdateListener(Runnable listener) {
        this.queueListener = listener;
    }

    public void setMatchHandler(ArenaMode mode, MatchHandler handler) {
        if (mode != null && handler != null) {
            matchHandlers.put(mode, handler);
        }
    }

    public void setMatchCheck(Predicate<UUID> matchCheck) {
        matchChecks.clear();
        addMatchCheck(matchCheck);
    }

    public void addMatchCheck(Predicate<UUID> matchCheck) {
        if (matchCheck != null) {
            matchChecks.add(matchCheck);
        }
    }

    public void requeue(QueueEntry entry) {
        if (entry == null) {
            return;
        }
        QueueEntry refreshed = createEntry(entry.mode(), entry.members());
        insertEntry(refreshed);
        notifyQueueChange();
        for (UUID member : refreshed.members()) {
            updateScoreboard(Bukkit.getPlayer(member));
        }
        attemptMatchmaking();
    }

    public Duration getWaitDuration(UUID playerId) {
        QueueEntry entry = getEntry(playerId).orElse(null);
        return entry == null ? Duration.ZERO : entry.waitDuration();
    }

    public Optional<QueueEntry> getEntry(UUID playerId) {
        UUID entryId = playerToEntry.get(playerId);
        if (entryId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(entriesById.get(entryId));
    }

    private boolean isInMatch(UUID playerId) {
        for (Predicate<UUID> check : matchChecks) {
            if (check != null && check.test(playerId)) {
                return true;
            }
        }
        return false;
    }

    private QueueEntry createEntry(ArenaMode mode, List<UUID> members) {
        return createEntry(mode, members, null);
    }

    private QueueEntry createEntry(ArenaMode mode,
                                   List<UUID> members,
                                   Map<UUID, ArenaRatingManager.RatingSnapshot> preloadedSnapshots) {
        Map<UUID, ArenaRatingManager.RatingSnapshot> snapshots = preloadedSnapshots != null
                ? new HashMap<>(preloadedSnapshots)
                : loadSnapshots(members, mode.ratingCategory());
        return new QueueEntry(UUID.randomUUID(), mode, members, System.currentTimeMillis(), snapshots);
    }

    private Map<UUID, ArenaRatingManager.RatingSnapshot> loadSnapshots(List<UUID> members,
                                                                       ArenaRatingManager.RatingCategory category) {
        Map<UUID, ArenaRatingManager.RatingSnapshot> snapshots = new HashMap<>();
        for (UUID member : members) {
            snapshots.put(member, ratingManager.getSnapshot(member, category));
        }
        return snapshots;
    }

    private void insertEntry(QueueEntry entry) {
        LinkedHashMap<UUID, QueueEntry> queue = queues.get(entry.mode());
        if (queue == null) {
            return;
        }
        queue.remove(entry.entryId());
        LinkedHashMap<UUID, QueueEntry> ordered = new LinkedHashMap<>();
        boolean inserted = false;
        for (QueueEntry existing : queue.values()) {
            if (!inserted && existing.joinedAt() > entry.joinedAt()) {
                ordered.put(entry.entryId(), entry);
                inserted = true;
            }
            ordered.put(existing.entryId(), existing);
        }
        if (!inserted) {
            ordered.put(entry.entryId(), entry);
        }
        queue.clear();
        queue.putAll(ordered);
        entriesById.put(entry.entryId(), entry);
        for (UUID member : entry.members()) {
            playerToEntry.put(member, entry.entryId());
        }
    }

    private QueueEntry removeEntryInternal(UUID entryId) {
        if (entryId == null) {
            return null;
        }
        QueueEntry entry = entriesById.remove(entryId);
        if (entry == null) {
            return null;
        }
        LinkedHashMap<UUID, QueueEntry> queue = queues.get(entry.mode());
        if (queue != null) {
            queue.remove(entryId);
        }
        for (UUID member : entry.members()) {
            playerToEntry.remove(member);
            updateScoreboard(Bukkit.getPlayer(member));
        }
        return entry;
    }

    private void notifyQueueChange() {
        if (queueListener != null) {
            queueListener.run();
        }
        if (scoreboardManager != null) {
            for (UUID playerId : playerToEntry.keySet()) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null) {
                    updateScoreboard(player);
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

    private void sendMessage(QueueEntry entry, MessageType type, String message, UUID excluded) {
        for (UUID member : entry.members()) {
            if (excluded != null && excluded.equals(member)) {
                continue;
            }
            Player player = Bukkit.getPlayer(member);
            if (player != null) {
                ChatMessageUtil.send(player, type, message);
            }
        }
    }

    private void attemptMatchmaking() {
        for (ArenaMode mode : ArenaMode.values()) {
            attemptMatchmaking(mode);
        }
    }

    private void attemptMatchmaking(ArenaMode mode) {
        MatchHandler handler = matchHandlers.get(mode);
        LinkedHashMap<UUID, QueueEntry> queue = queues.get(mode);
        if (handler == null || queue == null || queue.size() < 2) {
            return;
        }
        boolean matched;
        do {
            matched = false;
            List<QueueEntry> entries = new ArrayList<>(queue.values());
            outer:
            for (int i = 0; i < entries.size(); i++) {
                QueueEntry first = entries.get(i);
                int windowFirst = first.matchWindow(first.waitDuration());
                QueueEntry best = null;
                int bestDiff = Integer.MAX_VALUE;
                for (int j = i + 1; j < entries.size(); j++) {
                    QueueEntry second = entries.get(j);
                    int diff = Math.abs(first.averageRating() - second.averageRating());
                    int allowed = Math.min(windowFirst, second.matchWindow(second.waitDuration()));
                    if (diff <= allowed && diff < bestDiff) {
                        best = second;
                        bestDiff = diff;
                    }
                }
                if (best != null) {
                    removeEntryInternal(first.entryId());
                    removeEntryInternal(best.entryId());
                    matched = true;
                    handler.handle(first, best);
                    break outer;
                }
            }
        } while (matched && queue.size() >= 2);
        notifyQueueChange();
    }

    @Override
    public void onPartyMembersChanged(Party party) {
        if (party == null) {
            return;
        }
        enforcePartyState(party.getMembers());
    }

    @Override
    public void onPartyDisbanded(List<UUID> formerMembers) {
        enforcePartyState(formerMembers);
    }

    private void enforcePartyState(List<UUID> members) {
        if (members == null) {
            return;
        }
        for (UUID member : members) {
            QueueEntry entry = getEntry(member).orElse(null);
            if (entry != null && entry.mode() == ArenaMode.TWO_VS_TWO) {
                QueueEntry removed = removeEntryInternal(entry.entryId());
                if (removed != null) {
                    notifyQueueChange();
                    sendMessage(removed, MessageType.WARNING,
                            ChatColor.RED + "2v2 queue cancelled because the party changed.", null);
                }
                break;
            }
        }
    }

    /** Result wrapper containing the outcome of a join attempt and optional message. */
    public record QueueJoinOutcome(QueueJoinResult result, String message) {
        public QueueJoinOutcome {
            Objects.requireNonNull(result, "result");
        }

        public static QueueJoinOutcome of(QueueJoinResult result) {
            return new QueueJoinOutcome(result, null);
        }
    }

    public enum QueueJoinResult {
        JOINED,
        ALREADY_QUEUED,
        IN_MATCH,
        PARTY_REQUIRED,
        PARTY_SIZE_INVALID,
        PARTY_MEMBER_OFFLINE,
        TEAM_MEMBER_QUEUED,
        TEAM_MEMBER_IN_MATCH,
        RANK_GAP_TOO_LARGE
    }

    public enum LeaveReason {
        PLAYER_REQUEST,
        DISCONNECT,
        SYSTEM
    }

    @FunctionalInterface
    public interface MatchHandler {
        void handle(QueueEntry first, QueueEntry second);
    }

    public record QueueEntry(UUID entryId,
                             ArenaMode mode,
                             List<UUID> members,
                             long joinedAt,
                             Map<UUID, ArenaRatingManager.RatingSnapshot> ratingSnapshots) {
        public QueueEntry {
            Objects.requireNonNull(entryId, "entryId");
            Objects.requireNonNull(mode, "mode");
            Objects.requireNonNull(members, "members");
            Objects.requireNonNull(ratingSnapshots, "ratingSnapshots");
            members = List.copyOf(members);
            ratingSnapshots = Map.copyOf(ratingSnapshots);
        }

        public Duration waitDuration() {
            long elapsed = Math.max(0L, System.currentTimeMillis() - joinedAt);
            return Duration.ofMillis(elapsed);
        }

        public int averageRating() {
            return (int) Math.round(members.stream()
                    .mapToInt(member -> {
                        ArenaRatingManager.RatingSnapshot snapshot = ratingSnapshots.get(member);
                        return snapshot != null ? snapshot.rating() : 0;
                    })
                    .average()
                    .orElse(0));
        }

        public int matchWindow(Duration waitDuration) {
            return (int) Math.round(members.stream()
                    .mapToInt(member -> {
                        ArenaRatingManager.RatingSnapshot snapshot = ratingSnapshots.get(member);
                        return snapshot == null ? 0 : snapshot.matchWindow(waitDuration);
                    })
                    .average()
                    .orElse(0));
        }

        public ArenaRatingManager.RatingSnapshot ratingSnapshot(UUID playerId) {
            return ratingSnapshots.get(playerId);
        }

        public boolean contains(UUID playerId) {
            return members.contains(playerId);
        }
    }
}

