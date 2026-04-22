package me.nakilex.levelplugin.stronghold;

import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.party.Party;
import me.nakilex.levelplugin.party.PartyManager;
import me.nakilex.levelplugin.party.PartyMembershipListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import static me.nakilex.levelplugin.utils.ChatMessageUtil.send;

/**
 * Stronghold queue with solo/duo/squad party validation and gear-band matchmaking.
 */
public class StrongholdQueueManager implements PartyMembershipListener {
    private final PartyManager partyManager;
    private final Map<StrongholdQueueMode, LinkedHashMap<UUID, QueueEntry>> queues = new EnumMap<>(StrongholdQueueMode.class);
    private final Map<UUID, QueueEntry> entriesById = new HashMap<>();
    private final Map<UUID, UUID> playerToEntry = new HashMap<>();

    private MatchHandler matchHandler;
    private Runnable queueListener;

    public StrongholdQueueManager(PartyManager partyManager) {
        this.partyManager = partyManager;
        for (StrongholdQueueMode mode : StrongholdQueueMode.values()) {
            queues.put(mode, new LinkedHashMap<>());
        }
        partyManager.addMembershipListener(this);
    }

    public QueueJoinOutcome join(Player player, StrongholdQueueMode mode) {
        if (player == null || mode == null) {
            return QueueJoinOutcome.of(QueueJoinResult.INVALID_REQUEST);
        }
        if (mode == StrongholdQueueMode.SOLO) {
            return joinMembers(mode, List.of(player.getUniqueId()));
        }

        Party party = partyManager.getParty(player.getUniqueId());
        if (party == null) {
            return QueueJoinOutcome.of(QueueJoinResult.PARTY_REQUIRED,
                    ChatColor.RED + "You need a party to queue for " + mode.displayName() + ".");
        }
        if (!party.isLeader(player.getUniqueId())) {
            return QueueJoinOutcome.of(QueueJoinResult.LEADER_REQUIRED,
                    ChatColor.RED + "Only the party leader can queue for " + mode.displayName() + ".");
        }

        List<UUID> members = List.copyOf(party.getMembers());
        if (members.size() != mode.teamSize()) {
            return QueueJoinOutcome.of(QueueJoinResult.PARTY_SIZE_INVALID,
                    ChatColor.RED + mode.displayName() + " requires exactly " + mode.teamSize() + " players.");
        }
        return joinMembers(mode, members);
    }

    private QueueJoinOutcome joinMembers(StrongholdQueueMode mode, List<UUID> members) {
        for (UUID member : members) {
            Player online = Bukkit.getPlayer(member);
            if (online == null || !online.isOnline()) {
                return QueueJoinOutcome.of(QueueJoinResult.PARTY_MEMBER_OFFLINE,
                        ChatColor.RED + playerName(member) + ChatColor.GRAY + " must be online to queue.");
            }
            if (playerToEntry.containsKey(member)) {
                return QueueJoinOutcome.of(QueueJoinResult.ALREADY_QUEUED,
                        ChatColor.RED + playerName(member) + ChatColor.GRAY + " is already queued.");
            }
        }

        int avgGear = averageGear(members);
        StrongholdGearBand gearBand = StrongholdGearBand.fromAverageGear(avgGear);
        QueueEntry entry = createEntry(mode, members, avgGear, gearBand);
        insertEntry(entry);
        notifyQueueChange();
        return QueueJoinOutcome.of(QueueJoinResult.JOINED,
                ChatColor.GREEN + "Queued " + mode.color() + mode.displayName() + ChatColor.GRAY
                        + " in " + gearBand.display() + ChatColor.GRAY + " band.");
    }

    public boolean leave(UUID playerId) {
        return leave(playerId, LeaveReason.PLAYER_REQUEST);
    }

    public boolean leave(UUID playerId, LeaveReason reason) {
        UUID entryId = playerToEntry.get(playerId);
        if (entryId == null) {
            return false;
        }
        QueueEntry entry = removeEntryInternal(entryId);
        if (entry == null) {
            return false;
        }
        if (reason != LeaveReason.MATCH_FOUND) {
            sendMessage(entry.members(), reasonMessage(reason), playerId);
        }
        notifyQueueChange();
        return true;
    }

    public void tick() {
        for (StrongholdQueueMode mode : StrongholdQueueMode.values()) {
            attemptMatchmaking(mode);
        }
    }

    private void attemptMatchmaking(StrongholdQueueMode mode) {
        LinkedHashMap<UUID, QueueEntry> queue = queues.get(mode);
        if (queue == null || queue.size() < 2) {
            return;
        }

        List<QueueEntry> entries = new ArrayList<>(queue.values());
        for (int i = 0; i < entries.size(); i++) {
            QueueEntry first = entries.get(i);
            if (!entriesById.containsKey(first.entryId())) {
                continue;
            }
            for (int j = i + 1; j < entries.size(); j++) {
                QueueEntry second = entries.get(j);
                if (!entriesById.containsKey(second.entryId())) {
                    continue;
                }
                if (first.gearBand() != second.gearBand()) {
                    continue;
                }
                removeEntryInternal(first.entryId());
                removeEntryInternal(second.entryId());
                notifyQueueChange();
                if (matchHandler != null) {
                    matchHandler.onMatchFound(new QueueMatch(mode, first, second, first.gearBand()));
                }
                sendMatchFound(first, second, mode);
                return;
            }
        }
    }

    private void sendMatchFound(QueueEntry first, QueueEntry second, StrongholdQueueMode mode) {
        String message = ChatColor.GOLD + "Stronghold match found for " + mode.color() + mode.displayName()
                + ChatColor.GOLD + " " + ChatColor.GRAY + "(" + first.gearBand().display() + ChatColor.GRAY + ").";
        sendMessage(first.members(), message, null);
        sendMessage(second.members(), message, null);
    }

    private QueueEntry createEntry(StrongholdQueueMode mode,
                                   List<UUID> members,
                                   int avgGear,
                                   StrongholdGearBand band) {
        UUID entryId = UUID.randomUUID();
        return new QueueEntry(entryId, mode, List.copyOf(members), avgGear, band, Instant.now());
    }

    private void insertEntry(QueueEntry entry) {
        entriesById.put(entry.entryId(), entry);
        queues.get(entry.mode()).put(entry.entryId(), entry);
        for (UUID member : entry.members()) {
            playerToEntry.put(member, entry.entryId());
        }
    }

    private QueueEntry removeEntryInternal(UUID entryId) {
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
        }
        return entry;
    }

    private void notifyQueueChange() {
        if (queueListener != null) {
            queueListener.run();
        }
    }

    private void sendMessage(List<UUID> members, String message, UUID exclude) {
        for (UUID member : members) {
            if (exclude != null && exclude.equals(member)) {
                continue;
            }
            Player player = Bukkit.getPlayer(member);
            if (player != null && player.isOnline()) {
                send(player, MessageType.INFO, message);
            }
        }
    }

    public boolean isQueued(UUID playerId) {
        return playerToEntry.containsKey(playerId);
    }

    public Optional<StrongholdQueueMode> getMode(UUID playerId) {
        return getEntry(playerId).map(QueueEntry::mode);
    }

    public Optional<StrongholdGearBand> getGearBand(UUID playerId) {
        return getEntry(playerId).map(QueueEntry::gearBand);
    }

    public OptionalInt getAverageGear(UUID playerId) {
        QueueEntry entry = getEntry(playerId).orElse(null);
        return entry == null ? OptionalInt.empty() : OptionalInt.of(entry.averageGear());
    }

    public int getQueuePopulation(StrongholdQueueMode mode) {
        LinkedHashMap<UUID, QueueEntry> queue = queues.get(mode);
        if (queue == null) {
            return 0;
        }
        int total = 0;
        for (QueueEntry entry : queue.values()) {
            total += entry.members().size();
        }
        return total;
    }

    public Duration getWaitDuration(UUID playerId) {
        QueueEntry entry = getEntry(playerId).orElse(null);
        return entry == null ? Duration.ZERO : Duration.between(entry.queuedAt(), Instant.now());
    }

    public void clear() {
        for (QueueEntry entry : new ArrayList<>(entriesById.values())) {
            removeEntryInternal(entry.entryId());
        }
        notifyQueueChange();
    }

    public void setQueueUpdateListener(Runnable queueListener) {
        this.queueListener = queueListener;
    }

    public void setMatchHandler(MatchHandler matchHandler) {
        this.matchHandler = matchHandler;
    }

    private Optional<QueueEntry> getEntry(UUID playerId) {
        UUID entryId = playerToEntry.get(playerId);
        if (entryId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(entriesById.get(entryId));
    }

    private int averageGear(List<UUID> members) {
        int total = 0;
        int count = 0;
        for (UUID member : members) {
            Player player = Bukkit.getPlayer(member);
            if (player == null || !player.isOnline()) {
                continue;
            }
            total += Math.max(0, ItemUtil.calculateTotalGearScore(player));
            count++;
        }
        return count <= 0 ? 0 : (int) Math.round((double) total / count);
    }

    private String reasonMessage(LeaveReason reason) {
        return switch (reason) {
            case DISCONNECT -> ChatColor.RED + "Stronghold queue cancelled because a member disconnected.";
            case PARTY_CHANGED -> ChatColor.RED + "Stronghold queue cancelled because party composition changed.";
            case PARTY_DISBANDED -> ChatColor.RED + "Stronghold queue cancelled because the party disbanded.";
            case PLAYER_REQUEST -> ChatColor.RED + "You left the Stronghold queue.";
            case MATCH_FOUND -> "";
        };
    }

    private String playerName(UUID id) {
        String name = Bukkit.getOfflinePlayer(id).getName();
        return name == null ? "Unknown" : name;
    }

    @Override
    public void onPartyMembersChanged(Party party) {
        if (party == null) {
            return;
        }
        for (QueueEntry entry : new ArrayList<>(entriesById.values())) {
            if (entry.mode() == StrongholdQueueMode.SOLO) {
                continue;
            }
            boolean touchesParty = false;
            for (UUID member : entry.members()) {
                if (party.getMembers().contains(member)) {
                    touchesParty = true;
                    break;
                }
            }
            if (touchesParty && !isPartyStillValid(party, entry)) {
                leave(entry.members().get(0), LeaveReason.PARTY_CHANGED);
            }
        }
    }

    @Override
    public void onPartyDisbanded(List<UUID> formerMembers) {
        if (formerMembers == null) {
            return;
        }
        for (UUID member : formerMembers) {
            if (isQueued(member)) {
                leave(member, LeaveReason.PARTY_DISBANDED);
            }
        }
    }

    private boolean isPartyStillValid(Party party, QueueEntry entry) {
        if (party.getMembers().size() != entry.mode().teamSize()) {
            return false;
        }
        List<UUID> members = new ArrayList<>(party.getMembers());
        members.sort(UUID::compareTo);
        List<UUID> queuedMembers = new ArrayList<>(entry.members());
        queuedMembers.sort(UUID::compareTo);
        return members.equals(queuedMembers);
    }

    public enum LeaveReason {
        PLAYER_REQUEST,
        DISCONNECT,
        PARTY_CHANGED,
        PARTY_DISBANDED,
        MATCH_FOUND
    }

    public enum QueueJoinResult {
        JOINED,
        ALREADY_QUEUED,
        PARTY_REQUIRED,
        LEADER_REQUIRED,
        PARTY_SIZE_INVALID,
        PARTY_MEMBER_OFFLINE,
        INVALID_REQUEST
    }

    public record QueueJoinOutcome(QueueJoinResult result, String message) {
        public static QueueJoinOutcome of(QueueJoinResult result) {
            return new QueueJoinOutcome(result, null);
        }

        public static QueueJoinOutcome of(QueueJoinResult result, String message) {
            return new QueueJoinOutcome(result, message);
        }
    }

    public record QueueEntry(UUID entryId,
                             StrongholdQueueMode mode,
                             List<UUID> members,
                             int averageGear,
                             StrongholdGearBand gearBand,
                             Instant queuedAt) {
    }

    public record QueueMatch(StrongholdQueueMode mode,
                             QueueEntry first,
                             QueueEntry second,
                             StrongholdGearBand gearBand) {
    }

    public interface MatchHandler {
        void onMatchFound(QueueMatch match);
    }

}
