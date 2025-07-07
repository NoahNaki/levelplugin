package me.nakilex.levelplugin.quests.data;

import java.util.Collections;
import java.util.List;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;

/**
 * Utility to instantiate {@link QuestReward} across different plugin versions.
 * Older builds had an extra runeIds parameter while newer ones do not.
 */
public final class QuestRewardCompat {

    private QuestRewardCompat() {}

    public static QuestReward create(int xp, int coins, int gems, List<Integer> itemIds) {
        return new QuestReward(xp, coins, gems, itemIds, Collections.emptyList());
    }

    public static QuestReward create(int xp, int coins, int gems, List<Integer> itemIds,
                                     List<PlayerClass> classes) {
        return new QuestReward(xp, coins, gems, itemIds, classes);
    }
}
