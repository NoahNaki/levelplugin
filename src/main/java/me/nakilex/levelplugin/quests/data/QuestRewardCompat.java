package me.nakilex.levelplugin.quests.data;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.List;

/**
 * Utility to instantiate {@link QuestReward} across different plugin versions.
 * Older builds had an extra runeIds parameter while newer ones do not.
 */
public final class QuestRewardCompat {
    private static final Constructor<QuestReward> FOUR_ARG;
    private static final Constructor<QuestReward> FIVE_ARG;

    static {
        Constructor<QuestReward> four = null;
        Constructor<QuestReward> five = null;
        try {
            four = QuestReward.class.getConstructor(int.class, int.class, int.class, List.class);
        } catch (NoSuchMethodException ignore) {
        }
        try {
            five = QuestReward.class.getConstructor(int.class, int.class, int.class, List.class, List.class);
        } catch (NoSuchMethodException ignore) {
        }
        FOUR_ARG = four;
        FIVE_ARG = five;
    }

    private QuestRewardCompat() {}

    public static QuestReward create(int xp, int coins, int gems, List<Integer> itemIds) {
        try {
            if (FOUR_ARG != null) {
                return FOUR_ARG.newInstance(xp, coins, gems, itemIds);
            }
            if (FIVE_ARG != null) {
                return FIVE_ARG.newInstance(xp, coins, gems, itemIds, Collections.emptyList());
            }
            throw new IllegalStateException("No compatible QuestReward constructor");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
