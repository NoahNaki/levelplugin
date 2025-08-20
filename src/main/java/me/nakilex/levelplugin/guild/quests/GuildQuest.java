package me.nakilex.levelplugin.guild.quests;

import java.util.HashMap;
import java.util.List;
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
    private final int stars;
    private final List<String> rewardTiers;
    private final Map<UUID, Integer> contributions = new HashMap<>();

    public GuildQuest(String id, String name, int stars, List<String> rewardTiers) {
        this.id = id;
        this.name = name;
        this.stars = stars;
        this.rewardTiers = rewardTiers;
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

    public List<String> getRewardTiers() {
        return rewardTiers;
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

    /** Reset contributions, effectively rerolling progress. */
    public void reroll() {
        contributions.clear();
    }
}
