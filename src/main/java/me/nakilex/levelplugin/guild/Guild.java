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
    /** Pending applicants mapped to timestamp applied. */
    private final Map<UUID, Long> applicants = new LinkedHashMap<>();
    /** Optional guild message of the day. */
    private String motd = "";

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

    /** Map of applicants and when they applied. */
    public Map<UUID, Long> getApplicants() {
        return applicants;
    }

    /** Current guild message of the day. */
    public String getMotd() {
        return motd;
    }

    public void setMotd(String motd) {
        this.motd = motd == null ? "" : motd;
    }

    public boolean addMember(UUID id) {
        return members.add(id);
    }

    /** Add an applicant with the current timestamp. */
    public boolean addApplicant(UUID id) {
        if (members.contains(id)) return false;
        return applicants.putIfAbsent(id, System.currentTimeMillis()) == null;
    }

    /** Remove applicant. */
    public boolean removeApplicant(UUID id) {
        return applicants.remove(id) != null;
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
