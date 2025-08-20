package me.nakilex.levelplugin.guild.quests;

import me.nakilex.levelplugin.quests.data.QuestReward;

/** Rewards granted when completing a guild quest. */
public class GuildQuestReward {
    private final int guildExp;
    private final int guildCoins;
    private final QuestReward personalReward;

    public GuildQuestReward(int guildExp, int guildCoins, QuestReward personalReward) {
        this.guildExp = guildExp;
        this.guildCoins = guildCoins;
        this.personalReward = personalReward;
    }

    public int getGuildExp() {
        return guildExp;
    }

    public int getGuildCoins() {
        return guildCoins;
    }

    public QuestReward getPersonalReward() {
        return personalReward;
    }
}
