package me.nakilex.levelplugin.guild.quests;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.guild.Guild;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.quests.data.QuestObjective;
import me.nakilex.levelplugin.quests.data.QuestObjectiveType;
import me.nakilex.levelplugin.quests.data.QuestReward;
import me.nakilex.levelplugin.quests.data.QuestRewardCompat;

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

    private GuildQuestManager() {}

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

        QuestObjective obj = new QuestObjective(type, null, amount);

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

