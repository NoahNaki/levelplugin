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
     * Backwards compatible constructor accepting the now-removed rune list.
     * <p>
     * Older compiled code expects this signature, so we simply ignore the
     * runeIds parameter and delegate to the primary constructor.
     */
    public QuestReward(int xp, int coins, int gems,
                       List<Integer> itemIds,
                       List<String> runeIds) {
        this(xp, coins, gems, itemIds);
    }

    public int getXp() { return xp; }
    public int getCoins() { return coins; }
    public int getGems() { return gems; }
    public List<Integer> getItemIds() { return itemIds; }
}
