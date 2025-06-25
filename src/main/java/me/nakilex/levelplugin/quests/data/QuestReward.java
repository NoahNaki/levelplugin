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

    public QuestReward(int xp, int coins, int gems,
                       List<Integer> itemIds) {
        this.xp = xp;
        this.coins = coins;
        this.gems = gems;
        this.itemIds = itemIds != null ? new ArrayList<>(itemIds) : new ArrayList<>();
    }

    /**
     * Legacy constructor that accepted rune IDs. Runes were removed from the
     * plugin, but this overload remains for backward compatibility with older
     * builds which still invoke it.
     */
    public QuestReward(int xp, int coins, int gems,
                       List<Integer> itemIds,
                       List<String> runeIds) {
        this(xp, coins, gems, itemIds);
        // runeIds ignored
    }

    public int getXp() { return xp; }
    public int getCoins() { return coins; }
    public int getGems() { return gems; }
    public List<Integer> getItemIds() { return itemIds; }
}
