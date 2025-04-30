package me.nakilex.levelplugin.party;

import org.bukkit.entity.Player;
import java.util.*;

public class PartyManager {

    // Map to store parties by leader UUID
    private Map<UUID, Party> parties;
    private Map<UUID, UUID> playerToParty; // Maps player UUIDs to their party leader UUID

    // Constructor
    public PartyManager() {
        this.parties = new HashMap<>();
        this.playerToParty = new HashMap<>();
    }

    // Create a new party
    public boolean createParty(UUID leader) {
        if (playerToParty.containsKey(leader)) {
            return false; // Player already in a party
        }
        Party party = new Party(leader);
        parties.put(leader, party);
        playerToParty.put(leader, leader);
        return true;
    }

    // Disband a party
    public boolean disbandParty(UUID leader) {
        if (!parties.containsKey(leader)) {
            return false; // No such party
        }
        Party party = parties.remove(leader);
        for (UUID member : party.getMembers()) {
            playerToParty.remove(member);
        }
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
                } else {
                    disbandParty(leader);
                }
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
            return true;
        }
        return false;
    }

    public boolean isInParty(UUID playerId) {
        return playerToParty.containsKey(playerId); // Check if player is mapped to a party
    }


    // Get a party by player UUID
    public Party getParty(UUID player) {
        UUID leader = playerToParty.get(player);
        return leader != null ? parties.get(leader) : null;
    }
}
