package me.nakilex.levelplugin.party;

import me.nakilex.levelplugin.party.synergy.PartySynergyProfile;
import me.nakilex.levelplugin.party.synergy.PartySynergyUtil;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import java.util.*;

public class PartyManager {

    // Map to store parties by leader UUID
    private Map<UUID, Party> parties;
    private Map<UUID, UUID> playerToParty; // Maps player UUIDs to their party leader UUID
    private final java.util.List<PartyMembershipListener> membershipListeners = new java.util.ArrayList<>();

    // Constructor
    public PartyManager() {
        this.parties = new HashMap<>();
        this.playerToParty = new HashMap<>();
    }

    public void addMembershipListener(PartyMembershipListener listener) {
        if (listener != null) {
            membershipListeners.add(listener);
        }
    }

    private void notifyMembersChanged(Party party) {
        if (party == null) return;
        broadcastSynergyStatus(party);
        for (PartyMembershipListener listener : membershipListeners) {
            listener.onPartyMembersChanged(party);
        }
    }

    private void broadcastSynergyStatus(Party party) {
        java.util.List<Player> online = new java.util.ArrayList<>();
        for (UUID id : party.getMembers()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                online.add(p);
            }
        }
        PartySynergyProfile profile = PartySynergyUtil.profile(online);
        if (profile.multiplier() <= 1.0) {
            return;
        }
        PartyUtils.broadcastMessage(party, ChatColor.DARK_GRAY + "Party Synergy: " + ChatColor.GREEN + profile.summary());
    }

    private void notifyDisbanded(java.util.List<UUID> formerMembers) {
        if (formerMembers == null || formerMembers.isEmpty()) return;
        for (PartyMembershipListener listener : membershipListeners) {
            listener.onPartyDisbanded(java.util.List.copyOf(formerMembers));
        }
    }

    private String playerName(UUID playerId) {
        if (playerId == null) return "Unknown";
        String name = Bukkit.getOfflinePlayer(playerId).getName();
        return name == null ? playerId.toString() : name;
    }

    private void broadcastPartyEvent(Party party, String message) {
        if (party == null || message == null || message.isBlank()) {
            return;
        }
        PartyUtils.broadcastMessage(party, ChatColor.GRAY + message);
    }

    // Create a new party
    public boolean createParty(UUID leader) {
        if (playerToParty.containsKey(leader)) {
            return false; // Player already in a party
        }
        Party party = new Party(leader);
        parties.put(leader, party);
        playerToParty.put(leader, leader);
        notifyMembersChanged(party);
        return true;
    }

    // Disband a party
    public boolean disbandParty(UUID leader) {
        if (!parties.containsKey(leader)) {
            return false; // No such party
        }
        Party party = parties.remove(leader);
        java.util.List<UUID> former = new java.util.ArrayList<>(party.getMembers());
        for (UUID member : party.getMembers()) {
            playerToParty.remove(member);
        }
        broadcastPartyEvent(party, ChatColor.RED + "The party has been disbanded.");
        notifyDisbanded(former);
        return true;
    }

    // Add a member to a party
    // PartyManager.java

    public boolean addMember(UUID leader, UUID member) {
        if (!parties.containsKey(leader) || playerToParty.containsKey(member)) {
            return false; // Party doesn't exist or player is already in a party
        }
        Party party = parties.get(leader);

        // Check if party is already at or above max size
        if (party.getSize() >= 4) {
            // You could optionally send a message here indicating the party is full.
            return false;
        }

        // Proceed if there's room
        if (party.addMember(member)) {
            playerToParty.put(member, leader);
            broadcastPartyEvent(party, ChatColor.YELLOW + playerName(member) + ChatColor.GRAY + " joined the party.");
            notifyMembersChanged(party);
            return true;
        }

        return false;
    }


    // Remove a member from a party
    public boolean removeMember(UUID leader, UUID member) {
        if (!parties.containsKey(leader)) {
            return false;
        }
        Party party = parties.get(leader);
        if (party.removeMember(member)) {
            playerToParty.remove(member);
            // If leader leaves, promote a new leader or disband
            if (member.equals(leader)) {
                if (party.getSize() > 0) {
                    UUID newLeader = party.getMembers().get(0);
                    party.promoteLeader(newLeader);
                    parties.put(newLeader, party);
                    parties.remove(leader);
                    for (UUID m : party.getMembers()) {
                        playerToParty.put(m, newLeader);
                    }
                    broadcastPartyEvent(party, ChatColor.YELLOW + playerName(member) + ChatColor.GRAY
                            + " left. " + ChatColor.GOLD + playerName(newLeader)
                            + ChatColor.GRAY + " is now leader.");
                    notifyMembersChanged(party);
                } else {
                    disbandParty(leader);
                }
            } else {
                broadcastPartyEvent(party, ChatColor.YELLOW + playerName(member) + ChatColor.GRAY + " left the party.");
                notifyMembersChanged(party);
            }
            return true;
        }
        return false;
    }

    // Promote a new leader
    public boolean promoteLeader(UUID leader, UUID newLeader) {
        if (!parties.containsKey(leader)) {
            return false;
        }
        Party party = parties.get(leader);
        if (party.promoteLeader(newLeader)) {
            parties.put(newLeader, party);
            parties.remove(leader);
            for (UUID member : party.getMembers()) {
                playerToParty.put(member, newLeader);
            }
            broadcastPartyEvent(party, ChatColor.GOLD + playerName(newLeader) + ChatColor.GRAY + " is now party leader.");
            notifyMembersChanged(party);
            return true;
        }
        return false;
    }

    public boolean isInParty(UUID playerId) {
        return playerToParty.containsKey(playerId); // Check if player is mapped to a party
    }


    /**
     * Convenience helper to remove a member from their current party,
     * regardless of whether they are the leader or not.
     */
    public boolean leaveParty(UUID member) {
        UUID leader = playerToParty.get(member);
        if (leader == null) {
            return false;
        }
        return removeMember(leader, member);
    }


    // Get a party by player UUID
    public Party getParty(UUID player) {
        UUID leader = playerToParty.get(player);
        return leader != null ? parties.get(leader) : null;
    }
}
