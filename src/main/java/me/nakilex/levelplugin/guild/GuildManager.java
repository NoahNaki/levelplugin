package me.nakilex.levelplugin.guild;

import me.nakilex.levelplugin.guild.events.GuildMembershipEvent;
import me.nakilex.levelplugin.guild.GuildPermission;
import me.nakilex.levelplugin.guild.GuildRole;
import me.nakilex.levelplugin.guild.quests.GuildQuest;
import me.nakilex.levelplugin.guild.quests.GuildQuestManager;
import me.nakilex.levelplugin.quests.data.QuestObjective;
import me.nakilex.levelplugin.quests.data.QuestObjectiveType;
import me.nakilex.levelplugin.quests.data.QuestReward;
import me.nakilex.levelplugin.quests.data.QuestRewardCompat;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class GuildManager {
    private static final GuildManager instance = new GuildManager();
    public static GuildManager getInstance() { return instance; }

    private final Map<String, Guild> guilds = new HashMap<>();
    private final Map<UUID, String> playerGuild = new HashMap<>();
    private final Map<UUID, String> pendingInvites = new HashMap<>();
    private final Map<String, Set<String>> pendingAlliance = new HashMap<>(); // target -> requesting guilds
    private final Map<String, Set<String>> pendingNeutral = new HashMap<>();  // target -> requesting guilds

    private JavaPlugin plugin;
    private File guildFile;

    private void fireEvent(UUID id, Guild g, GuildMembershipEvent.Action action) {
        Player p = Bukkit.getPlayer(id);
        if (p != null) {
            if (plugin != null) {
                plugin.getLogger().info("[GuildDebug] Firing " + action + " for " + p.getName());
            }
            Bukkit.getPluginManager().callEvent(new GuildMembershipEvent(p, g, action));
        } else if (plugin != null) {
            plugin.getLogger().info("[GuildDebug] Tried to fire " + action + " for offline player " + id);
        }
    }

    public Guild createGuild(String name, UUID leader) {
        if (guilds.containsKey(name) || playerGuild.containsKey(leader)) return null;
        Guild g = new Guild(name, leader);
        guilds.put(name, g);
        playerGuild.put(leader, name);
        GuildQuestManager.getInstance().ensureQuests(g);
        fireEvent(leader, g, GuildMembershipEvent.Action.JOIN);
        return g;
    }

    public boolean disbandGuild(String name) {
        Guild g = guilds.remove(name);
        if (g == null) return false;
        me.nakilex.levelplugin.environment.EnvironmentManager env = me.nakilex.levelplugin.Main.getInstance().getEnvironmentManager();
        env.neutralizeGuildTown(g);
        me.nakilex.levelplugin.guild.siege.GuildSiegeManager.getInstance().handleGuildDisband(name, g.getMembers());
        for (UUID m : g.getMembers()) {
            playerGuild.remove(m);
            fireEvent(m, g, GuildMembershipEvent.Action.LEAVE);
        }
        return true;
    }

    public Guild getGuild(String name) { return guilds.get(name); }

    /**
     * Retrieve a guild by name, ignoring capitalization differences.
     * Falls back to an exact match first before scanning all guilds.
     */
    public Guild getGuildIgnoreCase(String name) {
        Guild g = guilds.get(name);
        if (g != null) return g;
        for (Guild guild : guilds.values()) {
            if (guild.getName().equalsIgnoreCase(name)) {
                return guild;
            }
        }
        return null;
    }

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
        if (!g.addMember(player)) return false;
        playerGuild.put(player, name);
        fireEvent(player, g, GuildMembershipEvent.Action.JOIN);
        return true;
    }

    public boolean removeMember(UUID actor, UUID target) {
        Guild g = getGuild(actor);
        if (g == null || !hasPermission(actor, GuildPermission.KICK)) return false;
        if (actor.equals(target)) return false; // cannot kick yourself
        if (!g.removeMember(target)) return false;
        playerGuild.remove(target);
        fireEvent(target, g, GuildMembershipEvent.Action.LEAVE);
        if (g.getMembers().isEmpty()) {
            me.nakilex.levelplugin.environment.EnvironmentManager env = me.nakilex.levelplugin.Main.getInstance().getEnvironmentManager();
            env.neutralizeGuildTown(g);
            guilds.remove(g.getName());
            me.nakilex.levelplugin.guild.siege.GuildSiegeManager.getInstance().handleGuildDisband(g.getName(), java.util.Collections.emptySet());
        }
        return true;
    }

    public boolean promote(UUID executor, UUID target, GuildRole role) {
        Guild g = getGuild(executor);
        if (g == null || g.getRole(executor) != GuildRole.LEADER) return false;
        if (!g.getMembers().contains(target)) return false;
        if (role == GuildRole.LEADER) {
            g.setRole(g.getLeader(), GuildRole.ADVISOR);
        }
        g.setRole(target, role);
        me.nakilex.levelplugin.Main.getInstance().getEnvironmentManager().syncGuildTown(g);
        return true;
    }

    public boolean hasPermission(UUID player, GuildPermission perm) {
        Guild g = getGuild(player);
        if (g == null) return false;
        GuildRole role = g.getRole(player);
        return g.getPermissions(role).has(perm);
    }

    public boolean setPermission(UUID executor, GuildRole role, GuildPermission perm, boolean value) {
        Guild g = getGuild(executor);
        if (g == null || g.getRole(executor) != GuildRole.LEADER) return false;
        g.setPermission(role, perm, value);
        return true;
    }

    // ----- Applications -----

    /** Apply to join a guild. */
    public boolean apply(UUID player, String guildName) {
        Guild g = guilds.get(guildName);
        if (g == null) return false;
        if (playerGuild.containsKey(player)) return false;
        return g.addApplicant(player);
    }

    public boolean acceptApplicant(String guildName, UUID applicant) {
        Guild g = guilds.get(guildName);
        if (g == null) return false;
        if (!g.removeApplicant(applicant)) return false;
        if (!g.addMember(applicant)) return false;
        playerGuild.put(applicant, guildName);
        fireEvent(applicant, g, GuildMembershipEvent.Action.JOIN);
        return true;
    }

    public boolean denyApplicant(String guildName, UUID applicant) {
        Guild g = guilds.get(guildName);
        if (g == null) return false;
        return g.removeApplicant(applicant);
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

    /**
     * Immediately remove an existing alliance without requiring approval.
     */
    public boolean revokeAlliance(String from, String to) {
        Guild gA = guilds.get(from);
        Guild gB = guilds.get(to);
        if (gA == null || gB == null) return false;
        if (!gA.getAllies().contains(to)) return false;
        return setNeutral(from, to);
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

    // ----- Persistence -----

    /** Load guild data from disk. */
    public void init(JavaPlugin plugin) {
        this.plugin = plugin;
        guildFile = new File(plugin.getDataFolder(), "guilds.yml");
        if (!guildFile.exists()) {
            try {
                guildFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create guilds.yml: " + e.getMessage());
            }
        }
        loadGuilds();
    }

    private void loadGuilds() {
        guilds.clear();
        playerGuild.clear();
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(guildFile);
        if (!cfg.contains("guilds")) return;

        for (String name : cfg.getConfigurationSection("guilds").getKeys(false)) {
            String base = "guilds." + name + ".";
            try {
                UUID leader = UUID.fromString(cfg.getString(base + "leader"));
                Guild g = new Guild(name, leader);

                for (String id : cfg.getStringList(base + "members")) {
                    UUID uuid = UUID.fromString(id);
                    if (!uuid.equals(leader)) {
                        g.addMember(uuid);
                    }
                    playerGuild.put(uuid, name);
                }
                playerGuild.put(leader, name);

                g.getAllies().addAll(cfg.getStringList(base + "allies"));
                g.getHostiles().addAll(cfg.getStringList(base + "hostiles"));
                g.setMotd(cfg.getString(base + "motd", ""));
                g.setCoins(cfg.getInt(base + "coins", 0));
                g.setLevel(cfg.getInt(base + "level", 1));
                g.setExp(cfg.getInt(base + "exp", 0));

                for (String perkName : cfg.getStringList(base + "perks")) {
                    try {
                        g.getTownPerks().add(TownPerk.valueOf(perkName));
                    } catch (IllegalArgumentException ignored) {}
                }

                ConfigurationSection apps = cfg.getConfigurationSection(base + "applicants");
                if (apps != null) {
                    for (String key : apps.getKeys(false)) {
                        UUID uuid = UUID.fromString(key);
                        long ts = apps.getLong(key);
                        g.getApplicants().put(uuid, ts);
                    }
                }

                ConfigurationSection questSec = cfg.getConfigurationSection(base + "quests");
                if (questSec != null) {
                    for (String qid : questSec.getKeys(false)) {
                        ConfigurationSection qs = questSec.getConfigurationSection(qid);
                        if (qs == null) continue;
                        String qname = qs.getString("name", "Quest");
                        int stars = qs.getInt("stars", 1);
                        ConfigurationSection objSec = qs.getConfigurationSection("objective");
                        QuestObjective obj = new QuestObjective(
                                QuestObjectiveType.valueOf(objSec.getString("type", "KILL")),
                                objSec.getString("target", null),
                                objSec.getInt("amount", 1)
                        );
                        QuestReward pr = QuestRewardCompat.create(
                                qs.getInt("personal.xp", 0),
                                qs.getInt("personal.coins", 0),
                                0,
                                java.util.Collections.emptyList()
                        );
                        GuildQuest quest = new GuildQuest(qid, qname, stars, obj, pr,
                                qs.getInt("guild_exp", 0), qs.getInt("guild_coins", 0));
                        quest.setAccepted(qs.getBoolean("accepted", false));
                        quest.setRerolled(qs.getBoolean("rerolled", false));
                        quest.setCompleted(qs.getBoolean("completed", false));
                        ConfigurationSection contrib = qs.getConfigurationSection("contrib");
                        if (contrib != null) {
                            for (String key : contrib.getKeys(false)) {
                                try {
                                    quest.addContribution(UUID.fromString(key), contrib.getInt(key));
                                } catch (IllegalArgumentException ignored) {}
                            }
                        }
                        g.getQuests().put(qid, quest);
                    }
                }

                GuildQuestManager.getInstance().ensureQuests(g);

                ConfigurationSection progression = cfg.getConfigurationSection(base + "progression");
                if (progression != null) {
                    ConfigurationSection target = g.getProgressionData();
                    for (String key : progression.getKeys(false)) {
                        target.set(key, progression.get(key));
                    }
                }

                guilds.put(name, g);
            } catch (IllegalArgumentException ignored) {
                // Skip invalid UUIDs
            }
        }
    }

    /** Save guild data to disk. */
    public void save() {
        if (guildFile == null) return;
        YamlConfiguration cfg = new YamlConfiguration();
        for (Guild g : guilds.values()) {
            String base = "guilds." + g.getName() + ".";
            cfg.set(base + "leader", g.getLeader().toString());
            List<String> members = new ArrayList<>();
            for (UUID id : g.getMembers()) {
                members.add(id.toString());
            }
            cfg.set(base + "members", members);
            cfg.set(base + "allies", new ArrayList<>(g.getAllies()));
            cfg.set(base + "hostiles", new ArrayList<>(g.getHostiles()));
            cfg.set(base + "motd", g.getMotd());
            cfg.set(base + "coins", g.getCoins());
            cfg.set(base + "level", g.getLevel());
            cfg.set(base + "exp", g.getExp());
            if (!g.getTownPerks().isEmpty()) {
                java.util.List<String> perkNames = new java.util.ArrayList<>();
                for (TownPerk perk : g.getTownPerks()) {
                    perkNames.add(perk.name());
                }
                cfg.set(base + "perks", perkNames);
            }
            if (!g.getApplicants().isEmpty()) {
                ConfigurationSection sec = cfg.createSection(base + "applicants");
                for (Map.Entry<UUID, Long> e : g.getApplicants().entrySet()) {
                    sec.set(e.getKey().toString(), e.getValue());
                }
            }
            if (!g.getQuests().isEmpty()) {
                ConfigurationSection qRoot = cfg.createSection(base + "quests");
                for (Map.Entry<String, GuildQuest> e : g.getQuests().entrySet()) {
                    GuildQuest q = e.getValue();
                    ConfigurationSection qs = qRoot.createSection(e.getKey());
                    qs.set("name", q.getName());
                    qs.set("stars", q.getStars());
                    QuestObjective obj = q.getObjective();
                    qs.set("objective.type", obj.getType().name());
                    qs.set("objective.target", obj.getTarget());
                    qs.set("objective.amount", obj.getAmount());
                    QuestReward pr = q.getPersonalReward();
                    if (pr != null) {
                        qs.set("personal.xp", pr.getXp());
                        qs.set("personal.coins", pr.getCoins());
                    }
                    qs.set("guild_exp", q.getGuildExpReward());
                    qs.set("guild_coins", q.getGuildCoinReward());
                    qs.set("accepted", q.isAccepted());
                    qs.set("rerolled", q.isRerolled());
                    qs.set("completed", q.isCompleted());
                    if (!q.getContributions().isEmpty()) {
                        ConfigurationSection contrib = qs.createSection("contrib");
                        for (Map.Entry<UUID, Integer> c : q.getContributions().entrySet()) {
                            contrib.set(c.getKey().toString(), c.getValue());
                        }
                    }
                }
            }
            ConfigurationSection progression = g.getProgressionData();
            if (progression != null && !progression.getKeys(false).isEmpty()) {
                cfg.createSection(base + "progression", progression.getValues(true));
            }
        }
        try {
            cfg.save(guildFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save guilds.yml: " + e.getMessage());
        }
    }
}
