package me.nakilex.levelplugin.friend;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.ListenerPriority;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.GlowPacketUtil;
import me.nakilex.levelplugin.npc.system.NpcTagUtil;
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
 * Handles per-player glowing of friends in green.
 */
public class FriendGlowManager implements Listener {
    private final Main plugin;
    private final FriendManager friendManager;
    private final PlayerScoreboardManagerAccessor scoreboardAccessor;
    private final ProtocolManager protocol;
    private final Set<UUID> disabled = ConcurrentHashMap.newKeySet();
    /** Tracks which players are currently glowing for each viewer. */
    private final Map<UUID, Set<UUID>> glowing = new ConcurrentHashMap<>();

    public FriendGlowManager(Main plugin, FriendManager manager, PlayerScoreboardManagerAccessor accessor) {
        this.plugin = plugin;
        this.friendManager = manager;
        this.scoreboardAccessor = accessor;
        this.protocol = ProtocolLibrary.getProtocolManager();

        protocol.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL,
                PacketType.Play.Server.ENTITY_METADATA) {
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
        if (!areFriends(viewer.getUniqueId(), target.getUniqueId())) return;

        GlowPacketUtil.applyGlowing(event.getPacket(), true);
    }

    private boolean areFriends(UUID a, UUID b) {
        return friendManager.areFriends(a, b);
    }

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

    public void applyGlowScoreboard(Player viewer) {
        Scoreboard board = scoreboardAccessor.getBoard(viewer);
        if (board == null) return;
        Team team = board.getTeam("friendglow");
        if (team == null) {
            team = board.registerNewTeam("friendglow");
            team.setColor(ChatColor.GREEN);
            team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        }
        team.getEntries().forEach(team::removeEntry);

        Set<UUID> current = new HashSet<>(friendManager.getFriends(viewer.getUniqueId()));
        Set<UUID> prev = glowing.computeIfAbsent(viewer.getUniqueId(), k -> new HashSet<>());

        boolean glow = isEnabled(viewer);

        // Disable glow for players no longer friends
        for (UUID id : new HashSet<>(prev)) {
            if (!current.contains(id)) {
                Player p = Bukkit.getPlayer(id);
                if (p != null && p.isOnline()) {
                    sendGlow(viewer, p, false);
                }
                prev.remove(id);
            }
        }

        // Apply glow for current friends
        for (UUID id : current) {
            Player friend = Bukkit.getPlayer(id);
            String entry = friend != null ? friend.getName() : Bukkit.getOfflinePlayer(id).getName();
            if (entry == null) entry = id.toString();
            team.addEntry(entry);
            if (friend != null && friend.isOnline()) {
                sendGlow(viewer, friend, glow);
            }
            prev.add(id);
        }
    }

    private void sendGlow(Player viewer, Player target, boolean glowing) {
        PacketContainer packet = protocol.createPacket(PacketType.Play.Server.ENTITY_METADATA);
        packet.getIntegers().write(0, target.getEntityId());

        GlowPacketUtil.applyGlowing(packet, glowing);

        try {
            protocol.sendServerPacket(viewer, packet);
        } catch (Exception ignore) {
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (NpcTagUtil.isNpc(event.getPlayer())) {
            return;
        }
        disabled.remove(event.getPlayer().getUniqueId());
        glowing.remove(event.getPlayer().getUniqueId());
    }

    public interface PlayerScoreboardManagerAccessor {
        Scoreboard getBoard(Player player);
    }
}
