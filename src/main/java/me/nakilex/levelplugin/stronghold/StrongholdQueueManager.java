package me.nakilex.levelplugin.stronghold;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.debug.StrongholdDebugGenerator;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.party.Party;
import me.nakilex.levelplugin.party.PartyManager;
import me.nakilex.levelplugin.party.PartyMembershipListener;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class StrongholdQueueManager implements Listener, PartyMembershipListener {
    private static final int BASE_GEAR_BAND = 250;
    private final Main plugin;
    private final PartyManager partyManager;
    private final Map<StrongholdQueueMode, LinkedHashMap<UUID, QueueEntry>> queues = new EnumMap<>(StrongholdQueueMode.class);
    private final Map<UUID, QueueEntry> entriesById = new HashMap<>();
    private final Map<UUID, UUID> playerToEntry = new HashMap<>();
    private Runnable queueListener;

    public StrongholdQueueManager(Main plugin, PartyManager partyManager) {
        this.plugin = plugin;
        this.partyManager = partyManager;
        for (StrongholdQueueMode mode : StrongholdQueueMode.values()) {
            queues.put(mode, new LinkedHashMap<>());
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        partyManager.addMembershipListener(this);
    }

    public QueueJoinResult join(Player player, StrongholdQueueMode mode) {
        if (player == null || mode == null) {
            return QueueJoinResult.INVALID;
        }
        if (isQueued(player.getUniqueId())) {
            return QueueJoinResult.ALREADY_QUEUED;
        }
        if (plugin.getStrongholdSurvivalManager() != null && plugin.getStrongholdSurvivalManager().isActive(player.getUniqueId())) {
            return QueueJoinResult.IN_RUN;
        }

        List<UUID> members = validateMembers(player, mode);
        if (members == null) {
            return QueueJoinResult.PARTY_INVALID;
        }

        QueueEntry entry = createEntry(mode, members);
        insertEntry(entry);
        broadcast(entry.members(), ChatMessageUtil.MessageType.SUCCESS,
                ChatColor.GRAY + "Joined " + ChatColor.GOLD + mode.displayName() + ChatColor.GRAY + " stronghold queue.");
        notifyQueueChange();
        attemptMatchmaking(mode);
        return QueueJoinResult.JOINED;
    }

    public boolean leave(UUID playerId) {
        UUID entryId = playerToEntry.remove(playerId);
        if (entryId == null) return false;
        QueueEntry entry = entriesById.remove(entryId);
        if (entry == null) return false;
        queues.get(entry.mode()).remove(entry.entryId());
        for (UUID member : entry.members()) {
            playerToEntry.remove(member);
        }
        broadcast(entry.members(), ChatMessageUtil.MessageType.WARNING,
                ChatColor.GRAY + "Left stronghold queue.");
        notifyQueueChange();
        return true;
    }

    public Optional<StrongholdQueueMode> getMode(UUID playerId) {
        QueueEntry entry = getEntry(playerId);
        return entry == null ? Optional.empty() : Optional.of(entry.mode());
    }

    public int getQueuePopulation(StrongholdQueueMode mode) {
        int total = 0;
        for (QueueEntry entry : queues.get(mode).values()) {
            total += entry.members().size();
        }
        return total;
    }

    public boolean isQueued(UUID playerId) {
        return playerToEntry.containsKey(playerId);
    }

    public void setQueueUpdateListener(Runnable listener) {
        this.queueListener = listener;
    }

    public void clear() {
        entriesById.clear();
        playerToEntry.clear();
        for (LinkedHashMap<UUID, QueueEntry> queue : queues.values()) {
            queue.clear();
        }
        notifyQueueChange();
    }

    private QueueEntry createEntry(StrongholdQueueMode mode, List<UUID> members) {
        Map<UUID, Integer> gearByPlayer = new HashMap<>();
        int total = 0;
        for (UUID member : members) {
            Player player = Bukkit.getPlayer(member);
            int gear = player != null ? Math.max(50, ItemUtil.calculateTotalGearScore(player)) : 50;
            gearByPlayer.put(member, gear);
            total += gear;
        }
        int avg = members.isEmpty() ? 50 : Math.max(50, total / members.size());
        return new QueueEntry(UUID.randomUUID(), mode, List.copyOf(members), gearByPlayer, avg, System.currentTimeMillis());
    }

    private void insertEntry(QueueEntry entry) {
        entriesById.put(entry.entryId(), entry);
        queues.get(entry.mode()).put(entry.entryId(), entry);
        for (UUID member : entry.members()) {
            playerToEntry.put(member, entry.entryId());
        }
    }

    private QueueEntry getEntry(UUID playerId) {
        UUID entryId = playerToEntry.get(playerId);
        if (entryId == null) return null;
        return entriesById.get(entryId);
    }

    private List<UUID> validateMembers(Player initiator, StrongholdQueueMode mode) {
        if (mode == StrongholdQueueMode.SOLO) {
            return List.of(initiator.getUniqueId());
        }
        Party party = partyManager.getParty(initiator.getUniqueId());
        if (party == null || !party.isLeader(initiator.getUniqueId())) {
            ChatMessageUtil.send(initiator, ChatMessageUtil.MessageType.ERROR,
                    "You must be the party leader to queue for this mode.");
            return null;
        }
        List<UUID> members = new ArrayList<>(party.getMembers());
        if (members.size() > mode.teamSize()) {
            ChatMessageUtil.send(initiator, ChatMessageUtil.MessageType.ERROR,
                    "Your party is larger than " + mode.teamSize() + " for this queue.");
            return null;
        }
        for (UUID member : members) {
            Player p = Bukkit.getPlayer(member);
            if (p == null || !p.isOnline()) {
                ChatMessageUtil.send(initiator, ChatMessageUtil.MessageType.ERROR,
                        "All party members must be online.");
                return null;
            }
            if (isQueued(member)) {
                ChatMessageUtil.send(initiator, ChatMessageUtil.MessageType.ERROR,
                        "A party member is already queued.");
                return null;
            }
        }
        return members;
    }

    private void attemptMatchmaking(StrongholdQueueMode mode) {
        if (mode == StrongholdQueueMode.SOLO || mode == StrongholdQueueMode.DUO) {
            return;
        }
        LinkedHashMap<UUID, QueueEntry> queue = queues.get(mode);
        if (queue.isEmpty()) {
            return;
        }
        List<QueueEntry> ordered = new ArrayList<>(queue.values());
        ordered.sort(Comparator.comparingLong(QueueEntry::createdAtMs));

        int required = mode.teamSize();
        List<QueueEntry> selection = new ArrayList<>();
        int count = 0;
        int weightedGear = 0;
        for (QueueEntry candidate : ordered) {
            if (!selection.isEmpty()) {
                int avg = weightedGear / Math.max(1, count);
                if (Math.abs(candidate.averageGearScore() - avg) > gearBandFor(selection.getFirst())) {
                    continue;
                }
            }
            selection.add(candidate);
            count += candidate.members().size();
            weightedGear += candidate.averageGearScore() * candidate.members().size();
            if (count >= required) {
                break;
            }
        }

        if (count < required) {
            return;
        }

        List<UUID> mergedMembers = new ArrayList<>();
        for (QueueEntry entry : selection) {
            for (UUID member : entry.members()) {
                if (mergedMembers.size() >= required) {
                    break;
                }
                mergedMembers.add(member);
            }
        }
        int avgGear = weightedGear / Math.max(1, count);

        for (QueueEntry entry : selection) {
            removeEntry(entry.entryId());
        }
        notifyQueueChange();
        launchRun(mergedMembers, avgGear, mode);
    }

    private int gearBandFor(QueueEntry entry) {
        long waitedSeconds = Math.max(0L, (System.currentTimeMillis() - entry.createdAtMs()) / 1000L);
        int expansion = (int) Math.min(350, waitedSeconds / 10 * 50);
        return BASE_GEAR_BAND + expansion;
    }

    private void removeEntry(UUID entryId) {
        QueueEntry entry = entriesById.remove(entryId);
        if (entry == null) return;
        queues.get(entry.mode()).remove(entryId);
        for (UUID member : entry.members()) {
            playerToEntry.remove(member);
        }
    }

    private void launchRun(List<UUID> memberIds, int avgGear, StrongholdQueueMode mode) {
        List<Player> online = memberIds.stream()
                .map(Bukkit::getPlayer)
                .filter(p -> p != null && p.isOnline())
                .toList();
        if (online.isEmpty()) {
            return;
        }
        Player leader = online.get(ThreadLocalRandom.current().nextInt(online.size()));
        broadcast(memberIds, ChatMessageUtil.MessageType.INFO,
                ChatColor.GRAY + "Stronghold found! Leader " + ChatColor.GOLD + leader.getName()
                        + ChatColor.GRAY + " is creating a " + mode.displayName() + " run.");
        boolean ok = StrongholdDebugGenerator.generateTest(leader);
        if (!ok) {
            broadcast(memberIds, ChatMessageUtil.MessageType.ERROR, "Failed to create stronghold run world.");
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            World world = leader.getWorld();
            if (world == null) {
                return;
            }
            for (Player member : online) {
                if (!member.getUniqueId().equals(leader.getUniqueId())) {
                    member.teleportAsync(leader.getLocation());
                }
            }
            if (plugin.getStrongholdSurvivalManager() != null) {
                plugin.getStrongholdSurvivalManager().startRun(online, avgGear);
            }
        }, 50L);
    }

    private void notifyQueueChange() {
        if (queueListener != null) {
            queueListener.run();
        }
    }

    private void broadcast(List<UUID> members, ChatMessageUtil.MessageType type, String message) {
        for (UUID member : members) {
            Player player = Bukkit.getPlayer(member);
            if (player != null && player.isOnline()) {
                ChatMessageUtil.send(player, type, message);
            }
        }
    }

    @Override
    public void onPartyMembersChanged(Party party) {
        if (party == null) return;
        Set<UUID> members = Set.copyOf(party.getMembers());
        for (UUID member : members) {
            QueueEntry entry = getEntry(member);
            if (entry != null && entry.mode() != StrongholdQueueMode.SOLO) {
                leave(member);
                break;
            }
        }
    }

    @Override
    public void onPartyDisbanded(List<UUID> formerMembers) {
        if (formerMembers == null) return;
        for (UUID member : formerMembers) {
            QueueEntry entry = getEntry(member);
            if (entry != null && entry.mode() != StrongholdQueueMode.SOLO) {
                leave(member);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        leave(event.getPlayer().getUniqueId());
    }

    public void tick() {
        attemptMatchmaking(StrongholdQueueMode.SQUAD);
        LinkedHashMap<UUID, QueueEntry> queue = queues.get(StrongholdQueueMode.DUO);
        Iterator<QueueEntry> iterator = new ArrayList<>(queue.values()).iterator();
        while (iterator.hasNext()) {
            QueueEntry entry = iterator.next();
            if (entry.members().size() == StrongholdQueueMode.DUO.teamSize()) {
                removeEntry(entry.entryId());
                notifyQueueChange();
                launchRun(entry.members(), entry.averageGearScore(), StrongholdQueueMode.DUO);
                break;
            }
        }
    }

    private record QueueEntry(UUID entryId,
                              StrongholdQueueMode mode,
                              List<UUID> members,
                              Map<UUID, Integer> gearByPlayer,
                              int averageGearScore,
                              long createdAtMs) {
    }

    public enum QueueJoinResult {
        JOINED,
        ALREADY_QUEUED,
        PARTY_INVALID,
        IN_RUN,
        INVALID
    }
}
