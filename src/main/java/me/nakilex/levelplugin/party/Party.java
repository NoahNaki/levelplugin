package me.nakilex.levelplugin.party;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Party {

    // Fields
    private UUID leader; // Party leader's UUID
    private List<UUID> members; // List of members in the party
    private boolean chatEnabled; // Party chat toggle

    // Constructor
    public Party(UUID leader) {
        this.leader = leader;
        this.members = new ArrayList<>();
        this.members.add(leader); // Leader is added as the first member
        this.chatEnabled = false;
    }

    // Getters
    public UUID getLeader() {
        return leader;
    }

    public List<UUID> getMembers() {
        return members;
    }

    public boolean isChatEnabled() {
        return chatEnabled;
    }

    // Setters
    public void setLeader(UUID newLeader) {
        this.leader = newLeader;
    }

    public void setChatEnabled(boolean enabled) {
        this.chatEnabled = enabled;
    }

    // Methods

    // Add a member to the party
    public boolean addMember(UUID player) {
        if (!members.contains(player)) {
            members.add(player);
            return true;
        }
        return false; // Already in party
    }

    // Remove a member from the party
    public boolean removeMember(UUID player) {
        if (members.contains(player)) {
            members.remove(player);
            return true;
        }
        return false; // Not in party
    }

    // Promote a new leader
    public boolean promoteLeader(UUID newLeader) {
        if (members.contains(newLeader)) {
            setLeader(newLeader);
            return true;
        }
        return false; // Player is not in party
    }

    // Check if player is the leader
    public boolean isLeader(UUID player) {
        return leader.equals(player);
    }

    // Toggle party chat
    public void toggleChat() {
        chatEnabled = !chatEnabled;
    }

    // Check if a player is in the party
    public boolean isMember(UUID player) {
        return members.contains(player);
    }

    // Get size of the party
    public int getSize() {
        return members.size();
    }
}
