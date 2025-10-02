package me.nakilex.levelplugin.arena.match;

import me.nakilex.levelplugin.utils.ChatFormatter;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;

/** Utility to broadcast post-match combat summaries using shared formatting. */
public final class ArenaCombatSummaryBroadcaster {
    private ArenaCombatSummaryBroadcaster() {
    }

    public static void broadcast(ArenaCombatTracker tracker,
                                 List<UUID> orderedParticipants,
                                 Set<UUID> winners) {
        Map<UUID, ArenaCombatTracker.CombatStats> snapshot = tracker.snapshot(orderedParticipants);
        Set<UUID> winnerSet = winners == null ? Set.of() : new LinkedHashSet<>(winners);
        boolean draw = winnerSet.isEmpty();

        for (UUID viewerId : orderedParticipants) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer == null) {
                continue;
            }
            ChatMessageUtil.send(viewer, MessageType.INFO, ChatColor.GRAY + "Match summary:");

            List<String> lines = new ArrayList<>();
            lines.add(resultLine(draw, winnerSet.contains(viewerId)));
            lines.add("");
            for (UUID participantId : orderedParticipants) {
                ArenaCombatTracker.CombatStats stats = snapshot.get(participantId);
                lines.add(statLine(participantId, stats, winnerSet, draw));
            }
            ChatFormatter.sendBoxedCenteredMessages(viewer, ChatColor.DARK_GRAY.toString(), lines.toArray(new String[0]));
        }

        tracker.clear(orderedParticipants);
    }

    private static String resultLine(boolean draw, boolean winner) {
        if (draw) {
            return ChatColor.GOLD + "Draw" + ChatColor.DARK_GRAY + " — " + ChatColor.GRAY + "No ELO was exchanged.";
        }
        return (winner ? ChatColor.GREEN + "Victory" : ChatColor.RED + "Defeat")
                + ChatColor.DARK_GRAY + " — " + ChatColor.GRAY + "Thanks for playing!";
    }

    private static String statLine(UUID playerId,
                                   ArenaCombatTracker.CombatStats stats,
                                   Set<UUID> winners,
                                   boolean draw) {
        ChatColor nameColor = draw ? ChatColor.YELLOW : (winners.contains(playerId) ? ChatColor.GREEN : ChatColor.RED);
        String name = resolveName(playerId);
        String damage = formatNumber(stats == null ? 0 : stats.damageDealt());
        String healing = formatNumber(stats == null ? 0 : stats.healingDone());
        return nameColor + name + ChatColor.GRAY + " — " + ChatColor.RED + damage + ChatColor.GRAY + " dmg "
                + ChatColor.DARK_GRAY + "| " + ChatColor.GREEN + healing + ChatColor.GRAY + " heal";
    }

    private static String resolveName(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            return player.getName();
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(playerId);
        String name = offline.getName();
        return name != null ? name : "Unknown";
    }

    private static String formatNumber(double value) {
        double rounded = Math.round(value * 10.0) / 10.0;
        if (Math.abs(rounded - Math.rint(rounded)) < 1e-9) {
            return String.format(Locale.US, "%.0f", rounded);
        }
        return String.format(Locale.US, "%.1f", rounded);
    }
}
