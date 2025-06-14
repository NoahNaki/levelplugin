package me.nakilex.levelplugin.guild;

import java.util.*;

public class GuildManager {
    private static final GuildManager instance = new GuildManager();
    public static GuildManager getInstance() { return instance; }

    private final Map<String, Guild> guilds = new HashMap<>();
    private final Map<UUID, String> playerGuild = new HashMap<>();
    private final Map<UUID, String> pendingInvites = new HashMap<>();
    private final Map<String, Set<String>> pendingAlliance = new HashMap<>(); // target -> requesting guilds
    private final Map<String, Set<String>> pendingNeutral = new HashMap<>();  // target -> requesting guilds

    public Guild createGuild(String name, UUID leader) {
        if (guilds.containsKey(name) || playerGuild.containsKey(leader)) return null;
        Guild g = new Guild(name, leader);
        guilds.put(name, g);
        playerGuild.put(leader, name);
        return g;
    }

    public boolean disbandGuild(String name) {
        Guild g = guilds.remove(name);
        if (g == null) return false;
        for (UUID m : g.getMembers()) {
            playerGuild.remove(m);
        }
        return true;
    }

    public Guild getGuild(String name) { return guilds.get(name); }
    public Guild getGuild(UUID player) {
        String g = playerGuild.get(player);
        return g != null ? guilds.get(g) : null;
    }

    public Collection<Guild> getGuilds() { return guilds.values(); }

    public boolean invite(UUID leader, UUID target) {
        Guild g = getGuild(leader);
        if (g == null || !g.getLeader().equals(leader)) return false;
        if (playerGuild.containsKey(target)) return false;
        pendingInvites.put(target, g.getName());
        return true;
    }

    public boolean accept(UUID player) {
        String name = pendingInvites.remove(player);
        if (name == null) return false;
        Guild g = guilds.get(name);
        if (g == null) return false;
        g.addMember(player);
        playerGuild.put(player, name);
        return true;
    }

    public boolean removeMember(UUID leader, UUID target) {
        Guild g = getGuild(leader);
        if (g == null || !g.getLeader().equals(leader)) return false;
        if (leader.equals(target)) return false; // cannot kick yourself
        if (!g.removeMember(target)) return false;
        playerGuild.remove(target);
        if (g.getMembers().isEmpty()) {
            guilds.remove(g.getName());
        }
        return true;
    }

    public boolean promote(UUID leader, UUID target) {
        Guild g = getGuild(leader);
        if (g == null || !g.getLeader().equals(leader)) return false;
        if (!g.getMembers().contains(target)) return false;
        g.promote(target);
        return true;
    }

    // ----- Alliance / Neutrality Requests -----
    public boolean requestAlliance(String from, String to) {
        Guild gA = guilds.get(from);
        Guild gB = guilds.get(to);
        if (gA == null || gB == null) return false;
        if (!canAlly(gA, gB)) return false;
        pendingAlliance.computeIfAbsent(to, k -> new HashSet<>()).add(from);
        return true;
    }

    public boolean acceptAlliance(String receiver, String requester) {
        Set<String> set = pendingAlliance.get(receiver);
        if (set == null || !set.remove(requester)) return false;
        if (set.isEmpty()) pendingAlliance.remove(receiver);
        return setAlliance(receiver, requester);
    }

    public boolean denyAlliance(String receiver, String requester) {
        Set<String> set = pendingAlliance.get(receiver);
        if (set == null || !set.remove(requester)) return false;
        if (set.isEmpty()) pendingAlliance.remove(receiver);
        return true;
    }

    public boolean requestNeutral(String from, String to) {
        Guild gA = guilds.get(from);
        Guild gB = guilds.get(to);
        if (gA == null || gB == null) return false;
        pendingNeutral.computeIfAbsent(to, k -> new HashSet<>()).add(from);
        return true;
    }

    public boolean acceptNeutral(String receiver, String requester) {
        Set<String> set = pendingNeutral.get(receiver);
        if (set == null || !set.remove(requester)) return false;
        if (set.isEmpty()) pendingNeutral.remove(receiver);
        return setNeutral(receiver, requester);
    }

    public boolean denyNeutral(String receiver, String requester) {
        Set<String> set = pendingNeutral.get(receiver);
        if (set == null || !set.remove(requester)) return false;
        if (set.isEmpty()) pendingNeutral.remove(receiver);
        return true;
    }

    private void propagateHostile(Guild source, String targetName) {
        source.getHostiles().add(targetName);
        for (String allyName : source.getAllies()) {
            Guild ally = guilds.get(allyName);
            if (ally != null) ally.getHostiles().add(targetName);
        }
    }

    public boolean setHostile(String a, String b) {
        Guild gA = guilds.get(a);
        Guild gB = guilds.get(b);
        if (gA == null || gB == null) return false;
        gA.getAllies().remove(b);
        gB.getAllies().remove(a);
        propagateHostile(gA, b);
        propagateHostile(gB, a);
        return true;
    }

    private boolean canAlly(Guild a, Guild b) {
        if (a.getHostiles().contains(b.getName()) || b.getHostiles().contains(a.getName())) return false;
        for (String allyName : a.getAllies()) {
            Guild ally = guilds.get(allyName);
            if (ally != null && ally.getHostiles().contains(b.getName())) return false;
        }
        for (String allyName : b.getAllies()) {
            Guild ally = guilds.get(allyName);
            if (ally != null && ally.getHostiles().contains(a.getName())) return false;
        }
        return true;
    }

    public boolean setAlliance(String a, String b) {
        Guild gA = guilds.get(a);
        Guild gB = guilds.get(b);
        if (gA == null || gB == null) return false;
        if (!canAlly(gA, gB)) return false;
        gA.getAllies().add(b);
        gB.getAllies().add(a);
        for (String hostile : gA.getHostiles()) propagateHostile(gB, hostile);
        for (String hostile : gB.getHostiles()) propagateHostile(gA, hostile);
        return true;
    }

    public boolean setNeutral(String a, String b) {
        Guild gA = guilds.get(a);
        Guild gB = guilds.get(b);
        if (gA == null || gB == null) return false;
        gA.getAllies().remove(b);
        gB.getAllies().remove(a);
        gA.getHostiles().remove(b);
        gB.getHostiles().remove(a);
        for (Guild ally : guilds.values()) {
            if (ally.getAllies().contains(a)) ally.getHostiles().remove(b);
            if (ally.getAllies().contains(b)) ally.getHostiles().remove(a);
        }
        return true;
    }

    public boolean areHostile(UUID p1, UUID p2) {
        Guild g1 = getGuild(p1);
        Guild g2 = getGuild(p2);
        if (g1 == null || g2 == null) return false;
        return g1.getHostiles().contains(g2.getName()) || g2.getHostiles().contains(g1.getName());
    }
}
