package me.nakilex.levelplugin.salvage.managers;

import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.player.classes.essence.ClassEssence;
import me.nakilex.levelplugin.potions.data.PotionInstance;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.Map;

public class SalvageManager {

    // coins per stat‐point (unchanged)
    private static final int COINS_PER_STAT_POINT = 1;
    private static final int VANILLA_POTION_COINS = 1;

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

    /** Per-player toggle whether depositing a chosen rarity also moves lower rarities. */
    private final Map<java.util.UUID, Boolean> includeLower = new java.util.HashMap<>();

    /** Check if the player has enabled including lower rarities. */
    public boolean isIncludingLower(java.util.UUID player) {
        return includeLower.getOrDefault(player, false);
    }

    /** Toggle the player's include-lower-rarity setting. */
    public void toggleIncludingLower(java.util.UUID player) {
        includeLower.put(player, !isIncludingLower(player));
    }

    /** As before: total coins based on stats */
    /** Sum of all base stats for reuse in multiple calculations. */
    public int getTotalStats(CustomItem cItem) {
        return cItem.getHp()
            + cItem.getDef()
            + cItem.getStr()
            + cItem.getAgi()
            + cItem.getIntel()
            + cItem.getDex();
    }

    public int getSellPrice(CustomItem cItem) {
        int totalStats = getTotalStats(cItem);
        return totalStats * COINS_PER_STAT_POINT;
    }

    /**
     * Returns the gem‐currency reward for epic+ items.
     * Rarity below EPIC yields 0.
     */
    public int getGemReward(CustomItem cItem) {
        // sum stats just like coins
        int totalStats = getTotalStats(cItem);

        int multiplier = GEM_MULTIPLIERS.getOrDefault(cItem.getRarity(), 0);
        int rawGems    = totalStats * multiplier;

        // Divide by a larger number to make gems much rarer.
        // We still guarantee at least 1 gem for epic+.
        return multiplier > 0
            ? Math.max(1, rawGems / 10)
            : 0;
    }

    /**
     * Coins returned when salvaging potions.
     * Currently 1 coin per remaining charge.
     */
    public int getPotionSellPrice(PotionInstance potion) {
        return potion.getCharges();
    }

    /** Coins returned for vanilla potions without custom data. */
    public int getVanillaPotionSellPrice() {
        return VANILLA_POTION_COINS;
    }

    /** Coins returned when salvaging a class essence. */
    public int getEssenceSellPrice(ItemStack essence) {
        if (!ClassEssence.isEssence(essence)) {
            return 0;
        }
        ItemRarity rarity = ClassEssence.getRarity(essence);
        int star = ClassEssence.getStar(essence);

        int base = switch (rarity) {
            case COMMON -> 25;
            case UNCOMMON -> 60;
            case RARE -> 140;
            case EPIC -> 260;
            case LEGENDARY -> 420;
            case MYTHIC, FABLED -> 540;
            default -> 0;
        };
        return Math.max(0, base + (int) Math.round(base * 0.35 * star));
    }

}
