package me.nakilex.levelplugin.guild;

/**
 * Persistent upgrades that a guild can purchase for long-term bonuses.
 * These perks remain with the guild even if town ownership changes.
 */
public enum TownPerk {
    MERCHANT_DISCOUNT(0.10),
    BLACKSMITH_DISCOUNT(0.10),
    ENCHANTING_DISCOUNT(0.10),
    FAST_TRAVEL_DISCOUNT(0.20),
    DAILY_DUNGEON_PLUS_ONE(0.0);

    private final double discount;

    TownPerk(double discount) {
        this.discount = discount;
    }

    /**
     * @return percentage discount represented as a decimal (e.g. 0.10 = 10%).
     */
    public double getDiscount() {
        return discount;
    }
}
