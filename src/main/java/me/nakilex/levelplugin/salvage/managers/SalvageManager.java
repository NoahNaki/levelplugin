package me.nakilex.levelplugin.salvage.managers;

import me.nakilex.levelplugin.items.data.CustomItem;

public class SalvageManager {

    // Example rate: 1 coin per point of any stat
    // You can make this dynamic or configurable if you like.
    private static final int COINS_PER_STAT_POINT = 1;

    private static SalvageManager instance;

    private SalvageManager() {
        // Private constructor for singleton pattern
    }

    public static SalvageManager getInstance() {
        if (instance == null) {
            instance = new SalvageManager();
        }
        return instance;
    }

    /**
     * Calculates how many coins a CustomItem is worth
     * based on its stats.
     */
    public int getSellPrice(CustomItem cItem) {
        // Add up whichever stats you want to count for selling
        int totalStats =
            cItem.getHp() +
                cItem.getDef() +
                cItem.getStr() +
                cItem.getAgi() +
                cItem.getIntel() +
                cItem.getDex();

        return totalStats * COINS_PER_STAT_POINT;
    }
}
