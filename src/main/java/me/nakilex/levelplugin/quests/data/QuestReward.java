package me.nakilex.levelplugin.quests.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;

/** Simple container for quest rewards. */
public class QuestReward {
    private final int xp;
    private final int coins;
    private final int gems;
    private final List<Integer> itemIds;
    private final List<PlayerClass> unlockClasses;
    private final List<String> customLines;

    public QuestReward(int xp, int coins, int gems,
                       List<Integer> itemIds) {
        this(xp, coins, gems, itemIds, Collections.emptyList(), Collections.emptyList());
    }

    public QuestReward(int xp, int coins, int gems,
                       List<Integer> itemIds,
                       List<PlayerClass> unlockClasses) {
        this(xp, coins, gems, itemIds, unlockClasses, Collections.emptyList());
    }

    public QuestReward(int xp, int coins, int gems,
                       List<Integer> itemIds,
                       List<PlayerClass> unlockClasses,
                       List<String> customLines) {
        this.xp = xp;
        this.coins = coins;
        this.gems = gems;
        this.itemIds = itemIds != null ? new ArrayList<>(itemIds) : new ArrayList<>();
        this.unlockClasses = unlockClasses != null ? new ArrayList<>(unlockClasses) : new ArrayList<>();
        this.customLines = customLines != null ? new ArrayList<>(customLines) : new ArrayList<>();
    }

    /**
     * Legacy constructor that accepted rune IDs. Runes were removed from the
     * plugin, but this overload remains for backward compatibility with older
     * builds which still invoke it.
     */
    // Legacy constructor kept for binary compatibility with old saves that
    // included rune rewards. The rune list is ignored but the signature is
    // distinct by using an array parameter to avoid type erasure conflicts.
    public QuestReward(int xp, int coins, int gems,
                       List<Integer> itemIds,
                       String[] runeIds) {
        this(xp, coins, gems, itemIds, java.util.Collections.emptyList());
    }

    public int getXp() { return xp; }
    public int getCoins() { return coins; }
    public int getGems() { return gems; }
    public List<Integer> getItemIds() { return itemIds; }
    public List<PlayerClass> getUnlockClasses() { return unlockClasses; }
    public List<String> getCustomLines() { return customLines; }
}
