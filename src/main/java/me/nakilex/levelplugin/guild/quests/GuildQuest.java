package me.nakilex.levelplugin.guild.quests;

/** Data for an individual guild quest. */
public class GuildQuest {
    private final String id;
    private final GuildQuestType type;
    private final int amount;
    private final int difficulty; // 1-3 stars
    private final GuildQuestReward reward;
    private int progress;

    public GuildQuest(String id, GuildQuestType type, int amount, int difficulty, GuildQuestReward reward) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.difficulty = difficulty;
        this.reward = reward;
        this.progress = 0;
    }

    public String getId() { return id; }
    public GuildQuestType getType() { return type; }
    public int getAmount() { return amount; }
    public int getDifficulty() { return difficulty; }
    public GuildQuestReward getReward() { return reward; }
    public int getProgress() { return progress; }

    public void addProgress(int value) {
        progress = Math.min(amount, progress + value);
    }

    public boolean isCompleted() {
        return progress >= amount;
    }
}
