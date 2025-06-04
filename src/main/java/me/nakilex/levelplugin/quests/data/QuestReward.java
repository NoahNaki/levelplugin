package me.nakilex.levelplugin.quests.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Simple container for quest rewards. */
public class QuestReward {
    private final int xp;
    private final int coins;
    private final int gems;
    private final List<Integer> itemIds;
    private final List<String> runeIds;

    public QuestReward(int xp, int coins, int gems,
                       List<Integer> itemIds,
                       List<String> runeIds) {
        this.xp = xp;
        this.coins = coins;
        this.gems = gems;
        this.itemIds = itemIds != null ? new ArrayList<>(itemIds) : new ArrayList<>();
        this.runeIds = runeIds != null ? new ArrayList<>(runeIds) : new ArrayList<>();
    }

    public int getXp() { return xp; }
    public int getCoins() { return coins; }
    public int getGems() { return gems; }
    public List<Integer> getItemIds() { return itemIds; }
    public List<String> getRuneIds() { return runeIds; }
}
