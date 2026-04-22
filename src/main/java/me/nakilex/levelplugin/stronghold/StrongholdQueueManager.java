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
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

public class StrongholdQueueManager implements PartyMembershipListener {
    private static final int MATCH_BAND = 180;

    private final Main plugin;
    private final PartyManager partyManager;
    private final StrongholdSurvivalManager survivalManager;

    private final Map<StrongholdQueueMode, LinkedHashMap<UUID, QueueEntry>> queues = new EnumMap<>(StrongholdQueueMode.class);
    private final Map<UUID, UUID> playerToEntry = new HashMap<>();

    public StrongholdQueueManager(Main plugin, PartyManager partyManager, StrongholdSurvivalManager survivalManager) {
        this.plugin = plugin;
        this.partyManager = partyManager;
        this.survivalManager = survivalManager;
        for (StrongholdQueueMode mode : StrongholdQueueMode.values()) {
            queues.put(mode, new LinkedHashMap<>());
        }
        partyManager.addMembershipListener(this);
    }

    public boolean isQueued(UUID playerId) {
        return playerToEntry.containsKey(playerId);
    }

    public Optional<StrongholdQueueMode> getMode(UUID playerId) {
        UUID entryId = playerToEntry.get(playerId);
        if (entryId == null) return Optional.empty();
        for (Map.Entry<StrongholdQueueMode, LinkedHashMap<UUID, QueueEntry>> e : queues.entrySet()) {
            if (e.getValue().containsKey(entryId)) {
                return Optional.of(e.getKey());
            }
        }
        return Optional.empty();
    }

    public int queuePopulation(StrongholdQueueMode mode) {
        LinkedHashMap<UUID, QueueEntry> queue = queues.get(mode);
        if (queue == null) return 0;
        int total = 0;
        for (QueueEntry entry : queue.values()) {
            total += entry.members.size();
        }
        return total;
    }

    public String join(Player player, StrongholdQueueMode mode) {
        if (playerToEntry.containsKey(player.getUniqueId())) {
            return ChatColor.RED + "You are already in a stronghold queue.";
        }
        List<UUID> members;
        if (mode == StrongholdQueueMode.SOLO) {
            members = List.of(player.getUniqueId());
        } else {
            Party party = partyManager.getParty(player.getUniqueId());
            if (party == null) {
                return ChatColor.RED + "You need a party to join " + mode.displayName() + ".";
            }
            if (!party.isLeader(player.getUniqueId())) {
                return ChatColor.RED + "Only party leaders can queue " + mode.displayName() + ".";
            }
            if (party.getSize() != mode.teamSize()) {
                return ChatColor.RED + "Party must have exactly " + mode.teamSize() + " members.";
            }
            members = new ArrayList<>(party.getMembers());
        }

        for (UUID id : members) {
            Player online = Bukkit.getPlayer(id);
            if (online == null || !online.isOnline()) {
                return ChatColor.RED + "All party members must be online.";
            }
            if (playerToEntry.containsKey(id)) {
                return ChatColor.RED + "A teammate is already queued.";
            }
        }

        int avgGear = averageGear(members);
        QueueEntry entry = new QueueEntry(UUID.randomUUID(), mode, List.copyOf(members), avgGear, System.currentTimeMillis());
        queues.get(mode).put(entry.entryId, entry);
        for (UUID id : members) {
            playerToEntry.put(id, entry.entryId);
            Player online = Bukkit.getPlayer(id);
            if (online != null) {
                ChatMessageUtil.send(online, ChatMessageUtil.MessageType.SUCCESS,
                        "Joined " + mode.displayName() + " Stronghold queue (avg gear " + ChatColor.LIGHT_PURPLE + avgGear + ChatColor.GRAY + ").");
            }
        }
        return null;
    }

    public boolean leave(UUID playerId) {
        UUID entryId = playerToEntry.get(playerId);
        if (entryId == null) return false;
        QueueEntry removed = null;
        for (LinkedHashMap<UUID, QueueEntry> queue : queues.values()) {
            removed = queue.remove(entryId);
            if (removed != null) break;
        }
        if (removed == null) return false;
        for (UUID id : removed.members) {
            playerToEntry.remove(id);
            Player online = Bukkit.getPlayer(id);
            if (online != null && online.isOnline()) {
                ChatMessageUtil.send(online, ChatMessageUtil.MessageType.WARNING,
                        "Left Stronghold queue.");
            }
        }
        return true;
    }

    public void tick() {
        for (StrongholdQueueMode mode : StrongholdQueueMode.values()) {
            tryMatch(mode);
        }
    }

    private void tryMatch(StrongholdQueueMode mode) {
        LinkedHashMap<UUID, QueueEntry> queue = queues.get(mode);
        if (queue == null || queue.size() < 2) {
            return;
        }
        List<QueueEntry> entries = new ArrayList<>(queue.values());
        entries.sort(Comparator.comparingLong(e -> e.queuedAtMs));
        QueueEntry first = entries.get(0);
        QueueEntry second = entries.get(1);
        if (Math.abs(first.averageGear - second.averageGear) > MATCH_BAND) {
            return;
        }

        queue.remove(first.entryId);
        queue.remove(second.entryId);
        launchRun(first, second);
    }

    private void launchRun(QueueEntry... teamEntries) {
        List<UUID> members = new ArrayList<>();
        for (QueueEntry entry : teamEntries) {
            for (UUID id : entry.members) {
                playerToEntry.remove(id);
                members.add(id);
            }
        }
        if (members.isEmpty()) {
            return;
        }
        Player anchor = Bukkit.getPlayer(members.get(0));
        if (anchor == null || !anchor.isOnline()) {
            return;
        }

        StrongholdDebugGenerator.generateTest(anchor);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!anchor.isOnline()) {
                return;
            }
            Location center = anchor.getLocation().clone();
            for (UUID id : members) {
                Player teammate = Bukkit.getPlayer(id);
                if (teammate != null && teammate.isOnline()) {
                    teammate.teleport(center);
                }
            }
            survivalManager.startRun(members, center.getWorld(), center);
        }, 60L);
    }

    private int averageGear(List<UUID> members) {
        int total = 0;
        int count = 0;
        for (UUID id : members) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;
            total += ItemUtil.calculateTotalGearScore(p);
            count++;
        }
        return count <= 0 ? 0 : total / count;
    }

    @Override
    public void onPartyMembersChanged(Party party) {
        if (party == null) return;
        for (UUID member : List.copyOf(party.getMembers())) {
            UUID entryId = playerToEntry.get(member);
            if (entryId == null) {
                continue;
            }
            Optional<StrongholdQueueMode> mode = getMode(member);
            if (mode.isPresent() && mode.get() != StrongholdQueueMode.SOLO) {
                leave(member);
                break;
            }
        }
    }

    @Override
    public void onPartyDisbanded(List<UUID> formerMembers) {
        if (formerMembers == null) return;
        for (UUID member : formerMembers) {
            UUID entryId = playerToEntry.get(member);
            if (entryId == null) continue;
            Optional<StrongholdQueueMode> mode = getMode(member);
            if (mode.isPresent() && mode.get() != StrongholdQueueMode.SOLO) {
                leave(member);
            }
        }
    }

    private static final class QueueEntry {
        private final UUID entryId;
        private final StrongholdQueueMode mode;
        private final List<UUID> members;
        private final int averageGear;
        private final long queuedAtMs;

        private QueueEntry(UUID entryId, StrongholdQueueMode mode, List<UUID> members, int averageGear, long queuedAtMs) {
            this.entryId = entryId;
            this.mode = mode;
            this.members = members;
            this.averageGear = averageGear;
            this.queuedAtMs = queuedAtMs;
        }
    }
}
