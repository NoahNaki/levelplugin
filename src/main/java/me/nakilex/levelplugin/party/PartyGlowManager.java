package me.nakilex.levelplugin.party;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher.Registry;
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
    private final Set<UUID> enabled = ConcurrentHashMap.newKeySet();

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
        if (!enabled.contains(viewer.getUniqueId())) return;

        Entity entity = event.getPacket().getEntityModifier(viewer.getWorld()).read(0);
        if (!(entity instanceof Player target)) return;

        if (!areInSameParty(viewer.getUniqueId(), target.getUniqueId())) return;

        List<WrappedDataValue> values = new ArrayList<>(event.getPacket().getDataValueCollectionModifier().read(0));
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
        event.getPacket().getDataValueCollectionModifier().write(0, values);
    }

    private boolean areInSameParty(UUID a, UUID b) {
        Party party = partyManager.getParty(a);
        return party != null && party.getMembers().contains(b);
    }

    /** Toggle glow for a player. */
    public boolean toggle(Player player) {
        UUID id = player.getUniqueId();
        boolean now = !enabled.contains(id);
        if (now) enabled.add(id); else enabled.remove(id);
        player.sendMessage(ChatColor.GRAY + "Party glow: " + (now ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"));
        applyGlowScoreboard(player);
        return now;
    }

    public boolean isEnabled(Player player) {
        return enabled.contains(player.getUniqueId());
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
        if (!enabled.contains(viewer.getUniqueId())) return;
        Party party = partyManager.getParty(viewer.getUniqueId());
        if (party == null) return;
        for (UUID memberId : party.getMembers()) {
            if (memberId.equals(viewer.getUniqueId())) continue;
            Player member = Bukkit.getPlayer(memberId);
            String entry = member != null ? member.getName() : Bukkit.getOfflinePlayer(memberId).getName();
            if (entry == null) entry = memberId.toString();
            team.addEntry(entry);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        enabled.remove(event.getPlayer().getUniqueId());
    }

    /** Interface to access PlayerScoreboardManager boards without exposing the class. */
    public interface PlayerScoreboardManagerAccessor {
        Scoreboard getBoard(Player player);
    }
}
