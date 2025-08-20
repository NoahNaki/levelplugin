package me.nakilex.levelplugin.guild;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import me.nakilex.levelplugin.guild.quests.GuildQuest;

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
    /** Current guild level (starts at 1) */
    private int level = 1;
    /** Accumulated guild experience toward next level */
    private int exp = 0;
    /** Purchased town perks retained by the guild. */
    private final java.util.EnumSet<TownPerk> townPerks = java.util.EnumSet.noneOf(TownPerk.class);
    /** Active guild quests keyed by slot index. */
    private final java.util.Map<String, GuildQuest> quests = new java.util.LinkedHashMap<>();

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

    /** Add coins to the guild vault respecting capacity. */
    public void addCoins(int amount) {
        if (amount <= 0) return;
        coins = Math.min(coins + amount, getCoinCapacity());
    }

    /** Remove coins if available. @return true if enough coins were present */
    public boolean removeCoins(int amount) {
        if (amount <= 0 || amount > coins) return false;
        coins -= amount;
        return true;
    }

    /** Set coin balance directly (for loading). */
    public void setCoins(int coins) { this.coins = Math.max(0, Math.min(coins, getCoinCapacity())); }

    public int getLevel() { return level; }

    public void setLevel(int level) { this.level = Math.max(1, Math.min(10, level)); }

    public int getExp() { return exp; }

    public void setExp(int exp) { this.exp = Math.max(0, exp); }

    /** Experience required to reach the next guild level. */
    public int getExpNeeded() {
        if (level >= 10) return 0;
        return xpForLevel(level);
    }

    /** Access the guild's purchased town perks. */
    public java.util.EnumSet<TownPerk> getTownPerks() {
        return townPerks;
    }

    /** Access guild quests for display or progress tracking. */
    public java.util.Map<String, GuildQuest> getQuests() {
        return quests;
    }

    public boolean hasPerk(TownPerk perk) {
        return townPerks.contains(perk);
    }

    public void addPerk(TownPerk perk) {
        townPerks.add(perk);
    }

    /** Add guild experience and handle level ups. */
    public void addExp(int amount) {
        if (amount <= 0 || level >= 10) return;
        exp += amount;
        while (level < 10) {
            int need = xpForLevel(level);
            if (exp < need) break;
            exp -= need;
            level++;
            // Broadcast level up to online members and refresh their holograms
            for (UUID id : members) {
                org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(id);
                if (p != null) {
                    me.nakilex.levelplugin.utils.ChatFormatter.sendCenteredMessage(p,
                        org.bukkit.ChatColor.GOLD + "Your guild reached level " + level + "!");
                    me.nakilex.levelplugin.Main.getInstance().getEnvironmentManager().refreshAllBuildingHolograms(p);
                }
            }
        }
    }

    private static int xpForLevel(int lvl) {
        return (int) Math.round(1000 * Math.pow(1.5, lvl - 1));
    }

    /** Maximum guild members allowed at the current level. */
    public int getMaxMembers() {
        return 20 + (level - 1) * 5;
    }

    /** Cost reduction percentage for building upgrades. */
    public double getUpgradeDiscount() {
        return (level - 1) * 0.10;
    }

    /** Maximum coin storage based on guild level. */
    public int getCoinCapacity() {
        if (level <= 4) {
            return 25000 * level;
        }
        return 50000 * (level - 2);
    }

    /** Maximum storage pages available. */
    public int getMaxPages() {
        return Math.max(1, level - 1);
    }

    public boolean addMember(UUID id) {
        if (members.size() >= getMaxMembers()) return false;
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
