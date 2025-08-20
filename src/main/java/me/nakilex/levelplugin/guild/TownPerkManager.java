package me.nakilex.levelplugin.guild;

import java.util.EnumMap;
import java.util.Map;

/**
 * Handles purchasing and applying guild town perks.
 */
public class TownPerkManager {
    private static final TownPerkManager INSTANCE = new TownPerkManager();
    public static TownPerkManager getInstance() { return INSTANCE; }

    private final Map<TownPerk, Integer> perkCosts = new EnumMap<>(TownPerk.class);

    private TownPerkManager() {
        perkCosts.put(TownPerk.MERCHANT_DISCOUNT, 5000);
        perkCosts.put(TownPerk.BLACKSMITH_DISCOUNT, 5000);
        perkCosts.put(TownPerk.ENCHANTING_DISCOUNT, 5000);
        perkCosts.put(TownPerk.FAST_TRAVEL_DISCOUNT, 3000);
        perkCosts.put(TownPerk.DAILY_DUNGEON_PLUS_ONE, 8000);
    }

    public int getCost(TownPerk perk) {
        return perkCosts.getOrDefault(perk, 0);
    }

    /**
     * Attempt to purchase the specified perk for the guild.
     * Coins are deducted from the guild balance if successful.
     */
    public boolean purchase(Guild guild, TownPerk perk) {
        if (guild == null || perk == null) return false;
        if (guild.hasPerk(perk)) return false;
        int cost = getCost(perk);
        if (!guild.removeCoins(cost)) return false;
        guild.addPerk(perk);
        return true;
    }

    /**
     * Apply a perk discount to the base cost.
     */
    public int applyDiscount(Guild guild, TownPerk perk, int baseCost) {
        if (guild != null && guild.hasPerk(perk)) {
            return (int) Math.round(baseCost * (1.0 - perk.getDiscount()));
        }
        return baseCost;
    }
}
