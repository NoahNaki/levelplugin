package me.nakilex.levelplugin.guild.quests;

import java.util.*;

import me.nakilex.levelplugin.guild.Guild;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.quests.data.QuestReward;

/** Manages weekly guild quests and rerolls. */
public class GuildQuestManager {
    private static final GuildQuestManager INSTANCE = new GuildQuestManager();
    private static final int MAX_REROLLS = 2;

    private final Map<String, List<GuildQuest>> activeQuests = new HashMap<>();
    private final Map<String, Integer> rerollsRemaining = new HashMap<>();
    private final Random random = new Random();

    public static GuildQuestManager getInstance() {
        return INSTANCE;
    }

    private GuildQuestManager() {}

    /** Get or generate the weekly quests for the given guild. */
    public List<GuildQuest> getQuests(Guild guild) {
        return activeQuests.computeIfAbsent(guild.getName(), g -> generateQuests(guild));
    }

    /** Number of rerolls left for this guild. */
    public int getRerollsRemaining(Guild guild) {
        return rerollsRemaining.getOrDefault(guild.getName(), MAX_REROLLS);
    }

    /** Reroll a quest slot, if rerolls remain. */
    public void reroll(Guild guild, int index) {
        String key = guild.getName();
        int remaining = getRerollsRemaining(guild);
        if (remaining <= 0) return;
        List<GuildQuest> quests = getQuests(guild);
        quests.set(index, generateQuest(guild));
        rerollsRemaining.put(key, remaining - 1);
    }

    /** Add progress toward the specified quest. */
    public void addProgress(Guild guild, GuildQuest quest, int amount) {
        quest.addProgress(amount);
    }

    // ----- internal generation helpers -----
    private List<GuildQuest> generateQuests(Guild guild) {
        List<GuildQuest> list = new ArrayList<>();
        // generate one quest of each type for variety
        for (GuildQuestType type : GuildQuestType.values()) {
            list.add(generateQuest(guild, type));
        }
        rerollsRemaining.put(guild.getName(), MAX_REROLLS);
        return list;
    }

    private GuildQuest generateQuest(Guild guild) {
        GuildQuestType type = GuildQuestType.values()[random.nextInt(GuildQuestType.values().length)];
        return generateQuest(guild, type);
    }

    private GuildQuest generateQuest(Guild guild, GuildQuestType type) {
        int diff = determineDifficulty(guild);
        int amount = switch (diff) {
            case 1 -> 10;
            case 2 -> 25;
            default -> 50;
        };
        GuildQuestReward reward = new GuildQuestReward(
                diff * 100,
                diff * 50,
                new QuestReward(diff * 50, diff * 25, 0, Collections.emptyList()));
        return new GuildQuest(UUID.randomUUID().toString(), type, amount, diff, reward);
    }

    /**
     * Simple heuristic for quest difficulty based on guild level and average member level.
     */
    public int determineDifficulty(Guild guild) {
        int guildLevel = guild.getLevel();
        int total = 0;
        for (UUID id : guild.getMembers()) {
            total += LevelManager.getInstance().getLevel(id);
        }
        int avg = guild.getMembers().isEmpty() ? 0 : total / guild.getMembers().size();
        int score = guildLevel + avg / 10;
        if (score < 20) return 1;
        if (score < 40) return 2;
        return 3;
    }
}
