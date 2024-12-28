package me.nakilex.levelplugin.party;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PartyUtils {

    private static final Map<UUID, UUID> pendingInvites = new HashMap<>(); // Maps invitee -> inviter

    // Send a formatted message to all members of the party
    public static void broadcastMessage(Party party, String message) {
        for (UUID memberId : party.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline()) {
                member.sendMessage(ChatColor.GREEN + "[Party] " + message);
            }
        }
    }

    // Format a party message
    public static String formatMessage(String sender, String message) {
        return ChatColor.GREEN + "[Party] " + sender + ": " + message;
    }

    // Check if a player is the leader of a party
    public static boolean isLeader(PartyManager partyManager, UUID playerId) {
        Party party = partyManager.getParty(playerId);
        return party != null && party.isLeader(playerId);
    }

    // Get a player by name safely
    public static Player getPlayerByName(String name) {
        return Bukkit.getPlayer(name);
    }

    // Check if a player is in a party
    public static boolean isInParty(PartyManager partyManager, UUID playerId) {
        return partyManager.getParty(playerId) != null;
    }

    // Toggle party chat mode for a player
    public static boolean togglePartyChat(PartyManager partyManager, UUID playerId) {
        Party party = partyManager.getParty(playerId);
        if (party != null) {
            party.toggleChat();
            return true;
        }
        return false;
    }

    public static UUID getInviter(UUID invitee) {
        return pendingInvites.get(invitee);
    }

    // Handle party invites
    public static void sendInvite(UUID inviter, UUID invitee) {
        pendingInvites.put(invitee, inviter);
        Player inviteePlayer = Bukkit.getPlayer(invitee);
        Player inviterPlayer = Bukkit.getPlayer(inviter);
        if (inviteePlayer != null && inviterPlayer != null) {
            inviteePlayer.sendMessage(ChatColor.GREEN + inviterPlayer.getName() + " has invited you to join their party.");
            inviteePlayer.sendMessage(ChatColor.YELLOW + "Type /party accept to join or /party deny to reject.");
        }
    }

    public static boolean acceptInvite(UUID invitee, PartyManager partyManager) {
        if (pendingInvites.containsKey(invitee)) {
            UUID inviter = pendingInvites.remove(invitee);
            return partyManager.addMember(inviter, invitee);
        }
        return false;
    }

    public static boolean denyInvite(UUID invitee) {
        return pendingInvites.remove(invitee) != null;
    }
}
