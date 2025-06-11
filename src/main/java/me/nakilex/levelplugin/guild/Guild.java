package me.nakilex.levelplugin.guild;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.*;

public class Guild {
    private final String name;
    private UUID leader;
    private final Set<UUID> members = new HashSet<>();
    private final Set<String> allies = new HashSet<>();
    private final Set<String> hostiles = new HashSet<>();

    public Guild(String name, UUID leader) {
        this.name = name;
        this.leader = leader;
        this.members.add(leader);
    }

    public String getName() {
        return name;
    }

    public UUID getLeader() {
        return leader;
    }

    public Set<UUID> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    public Set<String> getAllies() {
        return allies;
    }

    public Set<String> getHostiles() {
        return hostiles;
    }

    public boolean addMember(UUID id) {
        return members.add(id);
    }

    public boolean removeMember(UUID id) {
        if (members.remove(id)) {
            if (id.equals(leader)) {
                if (!members.isEmpty()) {
                    leader = members.iterator().next();
                }
            }
            return true;
        }
        return false;
    }

    public void promote(UUID id) {
        if (members.contains(id)) {
            leader = id;
        }
    }

    public String getLeaderName() {
        OfflinePlayer p = Bukkit.getOfflinePlayer(leader);
        return p.getName() == null ? "Unknown" : p.getName();
    }
}
