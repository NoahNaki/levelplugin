package me.nakilex.levelplugin.guild.quests;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.guild.Guild;
import me.nakilex.levelplugin.guild.GuildManager;
import me.nakilex.levelplugin.mob.utils.CombatPowerUtil;
import me.nakilex.levelplugin.mob.utils.ThreatUtil;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.quests.data.QuestObjective;
import me.nakilex.levelplugin.quests.data.QuestObjectiveType;
import me.nakilex.levelplugin.quests.data.QuestReward;
import me.nakilex.levelplugin.quests.data.QuestRewardCompat;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Utility responsible for generating and maintaining guild quests.
 * Quests scale their difficulty based on guild metrics to keep
 * objectives relevant for the guild's strength.
 */
public class GuildQuestManager {

    private static final GuildQuestManager INSTANCE = new GuildQuestManager();
    public static GuildQuestManager getInstance() { return INSTANCE; }

    private final Random random = new Random();
    private final List<String> easyMobs = new ArrayList<>();
    private final List<String> mediumMobs = new ArrayList<>();
    private final List<String> hardMobs = new ArrayList<>();
    private boolean mobsLoaded = false;
    /** Players tracking a specific guild quest. */
    private final Map<UUID, String> tracked = new HashMap<>();

    private GuildQuestManager() {}

    /**
     * Populate mob difficulty buckets from the configured mob rewards. This is
     * called lazily so the manager can be constructed before the mob rewards
     * configuration is loaded during plugin bootstrap.
     */
    public void reloadMobCategories() {
        easyMobs.clear();
        mediumMobs.clear();
        hardMobs.clear();
        mobsLoaded = false;

        Main plugin = Main.getInstance();
        if (plugin == null || plugin.getMobRewardsConfig() == null) {
            return; // config not ready yet
        }

        ConfigurationSection mobs = plugin.getMobRewardsConfig().getConfig()
                .getConfigurationSection("mobs");
        if (mobs == null) {
            return;
        }

        for (String key : mobs.getKeys(false)) {
            int power = CombatPowerUtil.estimateCombatPower(key);
            int threat = ThreatUtil.levelForPower(power);
            if (threat <= 2) {
                easyMobs.add(key);
            } else if (threat == 3) {
                mediumMobs.add(key);
            } else {
                hardMobs.add(key);
            }
        }

        mobsLoaded = true;
    }

    /** Ensure the guild always has three quest slots populated without duplicates. */
    public void ensureQuests(Guild guild) {
        Set<QuestObjectiveType> used = new HashSet<>();
        // Remove duplicates among existing quests
        for (Map.Entry<String, GuildQuest> e : new ArrayList<>(guild.getQuests().entrySet())) {
            QuestObjectiveType type = e.getValue().getObjective().getType();
            if (!used.add(type)) {
                GuildQuest q = generateQuest(guild, used);
                guild.getQuests().put(e.getKey(), q);
                used.add(q.getObjective().getType());
            }
        }
        while (guild.getQuests().size() < 3) {
            String key = String.valueOf(guild.getQuests().size());
            GuildQuest q = generateQuest(guild, used);
            guild.getQuests().put(key, q);
            used.add(q.getObjective().getType());
        }
    }

    /** Replace the quest in the given slot with a new one and mark reroll used. */
    public void rerollQuest(Guild guild, String slot) {
        GuildQuest current = guild.getQuests().get(slot);
        if (current == null || current.isAccepted() || current.isRerolled()) return;
        Set<QuestObjectiveType> used = new HashSet<>();
        for (Map.Entry<String, GuildQuest> e : guild.getQuests().entrySet()) {
            if (!e.getKey().equals(slot)) {
                used.add(e.getValue().getObjective().getType());
            }
        }
        GuildQuest next = generateQuest(guild, used);
        next.setRerolled(true); // reroll for this slot has been consumed
        guild.getQuests().put(slot, next);
    }

    /** Generate a quest with difficulty scaled to the guild, avoiding used types. */
    public GuildQuest generateQuest(Guild guild, Set<QuestObjectiveType> usedTypes) {
        int stars = computeDifficulty(guild);

        // Ensure mob difficulty buckets are loaded before attempting kill quests
        if (!mobsLoaded) {
            reloadMobCategories();
        }

        QuestObjectiveType[] types = {
                QuestObjectiveType.LOOTCHEST_OPEN,
                QuestObjectiveType.KILL,
                QuestObjectiveType.COLLECT,
                QuestObjectiveType.SIEGE_PARTICIPATE,
                QuestObjectiveType.DUEL_WIN
        };
        List<QuestObjectiveType> options = new ArrayList<>(Arrays.asList(types));
        if (usedTypes != null) {
            options.removeAll(usedTypes);
        }
        // If mob data isn't available, skip kill quests for now
        if (easyMobs.isEmpty() && mediumMobs.isEmpty() && hardMobs.isEmpty()) {
            options.remove(QuestObjectiveType.KILL);
        }
        if (options.isEmpty()) {
            options = Arrays.asList(types);
        }
        QuestObjectiveType type = options.get(random.nextInt(options.size()));

        int amount = switch (type) {
            case LOOTCHEST_OPEN -> stars * 5;
            case KILL -> stars * 30;
            case COLLECT -> stars * 50;
            case SIEGE_PARTICIPATE -> stars;
            case DUEL_WIN -> stars * 3;
            default -> stars * 10;
        };

        String name = switch (type) {
            case LOOTCHEST_OPEN -> "Treasure Hunt";
            case KILL -> "Monster Cull";
            case COLLECT -> "Resource Drive";
            case SIEGE_PARTICIPATE -> "Battle Preparations";
            case DUEL_WIN -> "Prove Your Might";
            default -> "Guild Task";
        };

        String target = null;
        if (type == QuestObjectiveType.KILL) {
            List<String> pool = switch (stars) {
                case 1 -> easyMobs;
                case 2 -> mediumMobs.isEmpty() ? easyMobs : mediumMobs;
                default -> hardMobs.isEmpty() ? mediumMobs : hardMobs;
            };
            if (!pool.isEmpty()) {
                target = pool.get(random.nextInt(pool.size()));
            }
        }

        QuestObjective obj = new QuestObjective(type, target, amount);

        QuestReward personal = QuestRewardCompat.create(
                stars * 50,
                stars * 25,
                0,
                java.util.Collections.emptyList()
        );
        int guildExp = stars * 100;
        int guildCoins = stars * 50;

        return new GuildQuest(UUID.randomUUID().toString(), name, stars, obj, personal, guildExp, guildCoins);
    }

    public void handleLootChestOpen(Player player) {
        updateObjective(player, QuestObjectiveType.LOOTCHEST_OPEN, "", 1);
    }

    public void handleKill(Player player, String mobType) {
        updateObjective(player, QuestObjectiveType.KILL, mobType, 1);
    }

    private void updateObjective(Player player, QuestObjectiveType type, String target, int amount) {
        Guild guild = GuildManager.getInstance().getGuild(player.getUniqueId());
        if (guild == null) return;
        for (GuildQuest quest : guild.getQuests().values()) {
            if (!quest.isAccepted()) continue;
            QuestObjective obj = quest.getObjective();
            if (obj.getType() != type) continue;
            String tgt = obj.getTarget();
            if (tgt != null && !tgt.equalsIgnoreCase(target) && !tgt.isEmpty()) continue;
            quest.addContribution(player.getUniqueId(), amount);
        }
    }

    /** Toggle tracking for the specified quest. Returns true if now tracked. */
    public boolean toggleTracking(Player player, GuildQuest quest) {
        UUID id = player.getUniqueId();
        String current = tracked.get(id);
        if (quest.getId().equals(current)) {
            tracked.remove(id);
            return false;
        } else {
            tracked.put(id, quest.getId());
            return true;
        }
    }

    /** Get the id of the guild quest a player is tracking, if any. */
    public String getTrackedQuest(UUID player) {
        return tracked.get(player);
    }

    /**
     * Compute difficulty between 1 and 3 stars from guild level, size
     * and average member level.
     */
    private int computeDifficulty(Guild guild) {
        int size = Math.max(1, guild.getMembers().size());
        int guildLevel = guild.getLevel();
        LevelManager lm = Main.getInstance().getLevelManager();
        int total = 0;
        for (UUID id : guild.getMembers()) {
            total += lm.getLevel(id);
        }
        int avg = total / size;
        int score = guildLevel + size / 5 + avg / 20;
        if (score >= 10) return 3;
        if (score >= 5) return 2;
        return 1;
    }
}

