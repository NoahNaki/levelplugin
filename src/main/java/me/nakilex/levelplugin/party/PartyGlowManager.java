package me.nakilex.levelplugin.party;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher.Registry;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import com.comphenix.protocol.reflect.StructureModifier;
import me.nakilex.levelplugin.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles per-player glowing of party members.
 */
public class PartyGlowManager implements Listener {
    private final Main plugin;
    private final PartyManager partyManager;
    private final PlayerScoreboardManagerAccessor scoreboardAccessor;
    private final ProtocolManager protocol;
    /**
     * Tracks players that have the party glow feature disabled. Absent = enabled
     * because the glow should be on by default.
     */
    private final Set<UUID> disabled = ConcurrentHashMap.newKeySet();
    /** Tracks which players are currently glowing for each viewer. */
    private final Map<UUID, Set<UUID>> glowing = new ConcurrentHashMap<>();

    public PartyGlowManager(Main plugin, PartyManager partyManager, PlayerScoreboardManagerAccessor accessor) {
        this.plugin = plugin;
        this.partyManager = partyManager;
        this.scoreboardAccessor = accessor;
        this.protocol = ProtocolLibrary.getProtocolManager();

        protocol.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL,
                com.comphenix.protocol.PacketType.Play.Server.ENTITY_METADATA) {
            @Override
            public void onPacketSending(PacketEvent event) {
                handleMetadata(event);
            }
        });
    }

    private void handleMetadata(PacketEvent event) {
        Player viewer = event.getPlayer();
        if (!isEnabled(viewer)) return;

        Entity entity = event.getPacket().getEntityModifier(viewer.getWorld()).read(0);
        if (!(entity instanceof Player target)) return;

        if (!areInSameParty(viewer.getUniqueId(), target.getUniqueId())) return;

        try {
            // ProtocolLib 5.x and above
            StructureModifier<List<WrappedDataValue>> mod =
                    (StructureModifier<List<WrappedDataValue>>) event.getPacket()
                            .getClass()
                            .getMethod("getDataValueCollectionModifier")
                            .invoke(event.getPacket());
            List<WrappedDataValue> values = new ArrayList<>(mod.read(0));
            boolean found = false;
            for (int i = 0; i < values.size(); i++) {
                WrappedDataValue val = values.get(i);
                if (val.getIndex() == 0 && val.getValue() instanceof Byte) {
                    byte flags = (byte) val.getValue();
                    flags |= 0x40; // glowing bit
                    values.set(i, new WrappedDataValue(0, val.getSerializer(), flags));
                    found = true;
                    break;
                }
            }
            if (!found) {
                WrappedDataWatcher.Serializer ser = Registry.get(Byte.class);
                values.add(new WrappedDataValue(0, ser, (byte) 0x40));
            }
            mod.write(0, values);
            return;
        } catch (Exception ignore) {
            // fall back to older API
        }

        List<WrappedWatchableObject> watchables = new ArrayList<>(event.getPacket().getWatchableCollectionModifier().read(0));
        boolean found = false;
        for (int i = 0; i < watchables.size(); i++) {
            WrappedWatchableObject val = watchables.get(i);
            if (val.getIndex() == 0 && val.getValue() instanceof Byte) {
                byte flags = (byte) val.getValue();
                flags |= 0x40;
                watchables.set(i, new WrappedWatchableObject(0, flags));
                found = true;
                break;
            }
        }
        if (!found) {
            watchables.add(new WrappedWatchableObject(0, (byte) 0x40));
        }
        event.getPacket().getWatchableCollectionModifier().write(0, watchables);
    }

    private boolean areInSameParty(UUID a, UUID b) {
        Party party = partyManager.getParty(a);
        return party != null && party.getMembers().contains(b);
    }

    /** Toggle glow for a player. */
    public boolean toggle(Player player) {
        UUID id = player.getUniqueId();
        boolean nowEnabled;
        if (disabled.contains(id)) {
            disabled.remove(id);
            nowEnabled = true;
        } else {
            disabled.add(id);
            nowEnabled = false;
        }
        applyGlowScoreboard(player);
        return nowEnabled;
    }

    public boolean isEnabled(Player player) {
        return !disabled.contains(player.getUniqueId());
    }

    /**
     * Update the scoreboard team for this player's glow settings.
     */
    public void applyGlowScoreboard(Player viewer) {
        Scoreboard board = scoreboardAccessor.getBoard(viewer);
        if (board == null) return;
        Team team = board.getTeam("partyglow");
        if (team == null) {
            team = board.registerNewTeam("partyglow");
            team.setColor(ChatColor.YELLOW);
            team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        }
        team.getEntries().forEach(team::removeEntry);

        Party party = partyManager.getParty(viewer.getUniqueId());
        Set<UUID> current = new HashSet<>();
        if (party != null) {
            current.addAll(party.getMembers());
            current.remove(viewer.getUniqueId());
        }

        Set<UUID> prev = glowing.computeIfAbsent(viewer.getUniqueId(), k -> new HashSet<>());
        boolean glow = isEnabled(viewer);

        // Disable glow for players no longer party members
        for (UUID id : new HashSet<>(prev)) {
            if (!current.contains(id)) {
                Player p = Bukkit.getPlayer(id);
                if (p != null && p.isOnline()) {
                    sendGlow(viewer, p, false);
                }
                prev.remove(id);
            }
        }

        for (UUID memberId : current) {
            Player member = Bukkit.getPlayer(memberId);
            String entry = member != null ? member.getName() : Bukkit.getOfflinePlayer(memberId).getName();
            if (entry == null) entry = memberId.toString();
            team.addEntry(entry);
            if (member != null && member.isOnline()) {
                sendGlow(viewer, member, glow);
            }
            prev.add(memberId);
        }
    }

    private void sendGlow(Player viewer, Player target, boolean glowing) {
        PacketContainer packet = protocol.createPacket(PacketType.Play.Server.ENTITY_METADATA);
        packet.getIntegers().write(0, target.getEntityId());

        try {
            StructureModifier<List<WrappedDataValue>> mod =
                    (StructureModifier<List<WrappedDataValue>>) packet.getClass()
                            .getMethod("getDataValueCollectionModifier")
                            .invoke(packet);
            WrappedDataWatcher.Serializer ser = Registry.get(Byte.class);
            byte flags = (byte) (glowing ? 0x40 : 0);
            mod.write(0, Collections.singletonList(new WrappedDataValue(0, ser, flags)));
        } catch (Exception ex) {
            WrappedDataWatcher watcher = new WrappedDataWatcher();
            WrappedDataWatcher.Serializer ser = WrappedDataWatcher.Registry.get(Byte.class);
            watcher.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(0, ser), (byte) (glowing ? 0x40 : 0));
            packet.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());
        }

        try {
            protocol.sendServerPacket(viewer, packet);
        } catch (Exception e) {
            // ignore
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        disabled.remove(event.getPlayer().getUniqueId());
        glowing.remove(event.getPlayer().getUniqueId());
    }

    /** Interface to access PlayerScoreboardManager boards without exposing the class. */
    public interface PlayerScoreboardManagerAccessor {
        Scoreboard getBoard(Player player);
    }
}
