package me.nakilex.levelplugin.progression.threshold;

import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Generic threshold resolver for milestone-style progression systems.
 */
public class ThresholdRewardEngine<T> {
    private final NavigableMap<Integer, T> rewards = new TreeMap<>();

    public void put(int threshold, T reward) {
        if (threshold > 0 && reward != null) {
            rewards.put(threshold, reward);
        }
    }

    public T resolve(int value) {
        var entry = rewards.floorEntry(value);
        return entry == null ? null : entry.getValue();
    }

    public int resolveLevel(int value) {
        int level = 0;
        for (Integer threshold : rewards.navigableKeySet()) {
            if (value >= threshold) {
                level++;
            } else {
                break;
            }
        }
        return level;
    }
}
