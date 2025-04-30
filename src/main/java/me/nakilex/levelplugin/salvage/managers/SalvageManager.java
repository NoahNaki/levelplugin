package me.nakilex.levelplugin.salvage.managers;

import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.data.ItemRarity;

import java.util.EnumMap;
import java.util.Map;

public class SalvageManager {

    // coins per stat‐point (unchanged)
    private static final int COINS_PER_STAT_POINT = 1;

    // how many gem‐units per stat‐point by rarity
    private static final Map<ItemRarity, Integer> GEM_MULTIPLIERS;
    static {
        GEM_MULTIPLIERS = new EnumMap<>(ItemRarity.class);
        GEM_MULTIPLIERS.put(ItemRarity.EPIC,      1);
        GEM_MULTIPLIERS.put(ItemRarity.LEGENDARY, 2);
        GEM_MULTIPLIERS.put(ItemRarity.MYTHIC,    3);
        GEM_MULTIPLIERS.put(ItemRarity.FABLED,    4);
    }

    private static SalvageManager instance;
    private SalvageManager() {}
    public static SalvageManager getInstance() {
        if (instance == null) instance = new SalvageManager();
        return instance;
    }

    /** As before: total coins based on stats */
    public int getSellPrice(CustomItem cItem) {
        int totalStats =
            cItem.getHp()
                + cItem.getDef()
                + cItem.getStr()
                + cItem.getAgi()
                + cItem.getIntel()
                + cItem.getDex();
        return totalStats * COINS_PER_STAT_POINT;
    }

    /**
     * Returns the gem‐currency reward for epic+ items.
     * Rarity below EPIC yields 0.
     */
    public int getGemReward(CustomItem cItem) {
        // sum stats just like coins
        int totalStats =
            cItem.getHp()
                + cItem.getDef()
                + cItem.getStr()
                + cItem.getAgi()
                + cItem.getIntel()
                + cItem.getDex();

        int multiplier = GEM_MULTIPLIERS.getOrDefault(cItem.getRarity(), 0);
        int rawGems    = totalStats * multiplier;

        // Divide by a larger number to make gems much rarer.
        // We still guarantee at least 1 gem for epic+.
        return multiplier > 0
            ? Math.max(1, rawGems / 10)
            : 0;
    }

}
