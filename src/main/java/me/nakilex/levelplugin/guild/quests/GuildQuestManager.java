package me.nakilex.levelplugin.guild.quests;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.guild.Guild;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.quests.data.QuestObjective;
import me.nakilex.levelplugin.quests.data.QuestObjectiveType;
import me.nakilex.levelplugin.quests.data.QuestReward;

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

    /** Ensure the guild always has three quest slots populated. */
    public void ensureQuests(Guild guild) {
        while (guild.getQuests().size() < 3) {
            String key = String.valueOf(guild.getQuests().size());
            guild.getQuests().put(key, generateQuest(guild));
        }
    }

    /** Replace the quest in the given slot with a new one and mark reroll used. */
    public void rerollQuest(Guild guild, String slot) {
        GuildQuest current = guild.getQuests().get(slot);
        if (current == null || current.isAccepted() || current.isRerolled()) return;
        GuildQuest next = generateQuest(guild);
        next.setRerolled(true); // reroll for this slot has been consumed
        guild.getQuests().put(slot, next);
    }

    /** Generate a quest with difficulty scaled to the guild. */
    public GuildQuest generateQuest(Guild guild) {
        int stars = computeDifficulty(guild);

        QuestObjectiveType[] types = {
                QuestObjectiveType.LOOTCHEST_OPEN,
                QuestObjectiveType.KILL,
                QuestObjectiveType.COLLECT,
                QuestObjectiveType.SIEGE_PARTICIPATE,
                QuestObjectiveType.DUEL_WIN
        };
        QuestObjectiveType type = types[random.nextInt(types.length)];

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

        QuestReward personal = new QuestReward(stars * 50, stars * 25);
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

