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
    private final Map<UUID, GuildRole> roles = new HashMap<>();
    private final java.util.EnumMap<GuildRole, RolePermissions> permissions = new java.util.EnumMap<>(GuildRole.class);
    /** Pending applicants mapped to timestamp applied. */
    private final Map<UUID, Long> applicants = new LinkedHashMap<>();
    /** Optional guild message of the day. */
    private String motd = "";
    /** Stored guild coin balance. */
    private int coins = 0;

    public Guild(String name, UUID leader) {
        this.name = name;
        this.leader = leader;
        this.members.add(leader);
        roles.put(leader, GuildRole.LEADER);
        permissions.put(GuildRole.LEADER, new RolePermissions(true,true,true,true,true));
        permissions.put(GuildRole.ADVISOR, new RolePermissions(true,true,true,true,true));
        permissions.put(GuildRole.VETERAN, new RolePermissions(true,true,true,false,false));
        permissions.put(GuildRole.MEMBER, new RolePermissions(false,false,false,false,false));
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

    /** Current guild coin balance. */
    public int getCoins() { return coins; }

    /** Add coins to the guild vault. */
    public void addCoins(int amount) { if (amount > 0) coins += amount; }

    /** Remove coins if available. @return true if enough coins were present */
    public boolean removeCoins(int amount) {
        if (amount <= 0 || amount > coins) return false;
        coins -= amount;
        return true;
    }

    /** Set coin balance directly (for loading). */
    public void setCoins(int coins) { this.coins = Math.max(0, coins); }

    public boolean addMember(UUID id) {
        roles.put(id, GuildRole.MEMBER);
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
            roles.remove(id);
            if (id.equals(leader)) {
                if (!members.isEmpty()) {
                    leader = members.iterator().next();
                    roles.put(leader, GuildRole.LEADER);
                }
            }
            return true;
        }
        return false;
    }

    public void setRole(UUID id, GuildRole role) {
        roles.put(id, role);
        if (role == GuildRole.LEADER) {
            leader = id;
        }
    }

    public GuildRole getRole(UUID id) {
        if (id.equals(leader)) return GuildRole.LEADER;
        return roles.getOrDefault(id, GuildRole.MEMBER);
    }

    public RolePermissions getPermissions(GuildRole role) {
        return permissions.get(role);
    }

    public void setPermission(GuildRole role, GuildPermission perm, boolean value) {
        permissions.get(role).set(perm, value);
    }

    public String getLeaderName() {
        OfflinePlayer p = Bukkit.getOfflinePlayer(leader);
        return p.getName() == null ? "Unknown" : p.getName();
    }
}
