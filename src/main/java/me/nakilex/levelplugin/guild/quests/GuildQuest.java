package me.nakilex.levelplugin.guild.quests;

import me.nakilex.levelplugin.quests.data.QuestObjective;
import me.nakilex.levelplugin.quests.data.QuestReward;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a repeatable quest offered to a guild.  Each quest tracks the
 * contribution of individual members, exposes a star difficulty rating and a
 * list of reward tier descriptions.  The class is intentionally lightweight so
 * more advanced behaviour can be layered on top by other systems.
 */
public class GuildQuest {

    private final String id;
    private final String name;
    /**
     * Difficulty expressed as star count (1-3).  The star value is
     * determined when the quest is generated based on guild metrics
     * such as level and member averages.
     */
    private final int stars;

    /** Core objective describing what action the guild must complete. */
    private final QuestObjective objective;

    /** Rewards granted to the individual player when the quest completes. */
    private final QuestReward personalReward;

    /** Guild-wide experience reward. */
    private final int guildExpReward;

    /** Guild-wide coin reward. */
    private final int guildCoinReward;

    /** Track contributions from each guild member toward the objective. */
    private final Map<UUID, Integer> contributions = new HashMap<>();

    /** Whether the guild has accepted this quest. */
    private boolean accepted = false;

    /** Whether the quest slot has already been rerolled. */
    private boolean rerolled = false;

    public GuildQuest(String id, String name, int stars,
                      QuestObjective objective,
                      QuestReward personalReward,
                      int guildExpReward,
                      int guildCoinReward) {
        this.id = id;
        this.name = name;
        this.stars = stars;
        this.objective = objective;
        this.personalReward = personalReward;
        this.guildExpReward = guildExpReward;
        this.guildCoinReward = guildCoinReward;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getStars() {
        return stars;
    }

    public QuestObjective getObjective() {
        return objective;
    }

    /** Convenience accessor for the objective amount. */
    public int getTargetAmount() {
        return objective.getAmount();
    }

    public QuestReward getPersonalReward() {
        return personalReward;
    }

    public int getGuildExpReward() {
        return guildExpReward;
    }

    public int getGuildCoinReward() {
        return guildCoinReward;
    }

    /** Add contribution amount for the given member. */
    public void addContribution(UUID member, int amount) {
        contributions.merge(member, amount, Integer::sum);
    }

    /** Return total contribution from all members. */
    public int getTotalContribution() {
        return contributions.values().stream().mapToInt(Integer::intValue).sum();
    }

    /** Return contribution for a specific member. */
    public int getContribution(UUID member) {
        return contributions.getOrDefault(member, 0);
    }

    /** Expose contribution map for persistence. */
    public Map<UUID, Integer> getContributions() {
        return contributions;
    }

    /**
     * Reset contributions, effectively rerolling progress.
     * Callers should also swap the objective/rewards as needed.
     */
    public void reroll() {
        contributions.clear();
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    public boolean isRerolled() {
        return rerolled;
    }

    public void setRerolled(boolean rerolled) {
        this.rerolled = rerolled;
    }
}
