package me.nakilex.levelplugin.items.data;

import org.bukkit.Material;

import java.util.UUID;

public class CustomItem {

    // Unique instance ID
    private final UUID uuid;

    // Template metadata
    private final int id;
    private final String baseName;
    private final ItemRarity rarity;
    private final int levelRequirement;
    private final String classRequirement;
    private final Material material;

    // The ranges from which we roll each stat exactly once
    private final StatRange hpRange;
    private final StatRange defRange;
    private final StatRange strRange;
    private final StatRange agiRange;
    private final StatRange intelRange;
    private final StatRange dexRange;

    // The mutable base stats (initialized by rolling once from each range)
    private int baseHp;
    private int baseDef;
    private int baseStr;
    private int baseAgi;
    private int baseIntel;
    private int baseDex;

    // Any temporary bonuses (e.g. from enchantments, buffs)
    private int bonusHp    = 0;
    private int bonusDef   = 0;
    private int bonusStr   = 0;
    private int bonusAgi   = 0;
    private int bonusIntel = 0;
    private int bonusDex   = 0;

    // How many times this item has been upgraded (max 5)
    private int upgradeLevel = 0;

    /**
     * Primary constructor: loads an existing item instance (with a fixed UUID and upgradeLevel),
     * rolling its base stats once from the given ranges.
     */
    public CustomItem(UUID uuid,
                      int id,
                      String baseName,
                      ItemRarity rarity,
                      int levelRequirement,
                      String classRequirement,
                      Material material,
                      StatRange hpRange,
                      StatRange defRange,
                      StatRange strRange,
                      StatRange agiRange,
                      StatRange intelRange,
                      StatRange dexRange,
                      int upgradeLevel) {
        this.uuid             = uuid;
        this.id               = id;
        this.baseName         = baseName;
        this.rarity           = rarity;
        this.levelRequirement = levelRequirement;
        this.classRequirement = classRequirement;
        this.material         = material;

        this.hpRange    = hpRange;
        this.defRange   = defRange;
        this.strRange   = strRange;
        this.agiRange   = agiRange;
        this.intelRange = intelRange;
        this.dexRange   = dexRange;

        // Roll each stat once and store as the mutable base
        this.baseHp    = hpRange.roll();
        this.baseDef   = defRange.roll();
        this.baseStr   = strRange.roll();
        this.baseAgi   = agiRange.roll();
        this.baseIntel = intelRange.roll();
        this.baseDex   = dexRange.roll();

        this.upgradeLevel = upgradeLevel;
    }

    /**
     * Convenience constructor for brand-new items: generates a UUID
     * and starts at upgradeLevel 0.
     */
    public CustomItem(int id,
                      String baseName,
                      ItemRarity rarity,
                      int levelRequirement,
                      String classRequirement,
                      Material material,
                      StatRange hpRange,
                      StatRange defRange,
                      StatRange strRange,
                      StatRange agiRange,
                      StatRange intelRange,
                      StatRange dexRange) {
        this(UUID.randomUUID(),
            id, baseName, rarity, levelRequirement, classRequirement, material,
            hpRange, defRange, strRange, agiRange, intelRange, dexRange,
            0);
    }

    // ─── Getters ───────────────────────────────────────────────────────────────

    public UUID getUuid()               { return uuid; }
    public int getId()                  { return id; }
    public String getBaseName()         { return baseName; }
    public ItemRarity getRarity()       { return rarity; }
    public int getLevelRequirement()    { return levelRequirement; }
    public String getClassRequirement() { return classRequirement; }
    public Material getMaterial()       { return material; }

    public StatRange getHpRange()    { return hpRange; }
    public StatRange getDefRange()   { return defRange; }
    public StatRange getStrRange()   { return strRange; }
    public StatRange getAgiRange()   { return agiRange; }
    public StatRange getIntelRange() { return intelRange; }
    public StatRange getDexRange()   { return dexRange; }

    public int getHp()    { return baseHp    + bonusHp; }
    public int getDef()   { return baseDef   + bonusDef; }
    public int getStr()   { return baseStr   + bonusStr; }
    public int getAgi()   { return baseAgi   + bonusAgi; }
    public int getIntel() { return baseIntel + bonusIntel; }
    public int getDex()   { return baseDex   + bonusDex; }

    public int getUpgradeLevel() { return upgradeLevel; }

    /** Display name with stars for upgrades. */
    public String getName() {
        return baseName + "★".repeat(upgradeLevel);
    }
    public String getDisplayName() { return getName(); }

    // ─── Mutators ─────────────────────────────────────────────────────────────

    public void setUpgradeLevel(int upgradeLevel) {
        this.upgradeLevel = Math.min(5, Math.max(0, upgradeLevel));
    }

    public void addBonusStats(int hp, int def, int str, int agi, int intel, int dex) {
        this.bonusHp    += hp;
        this.bonusDef   += def;
        this.bonusStr   += str;
        this.bonusAgi   += agi;
        this.bonusIntel += intel;
        this.bonusDex   += dex;
    }

    public void removeBonusStats(int hp, int def, int str, int agi, int intel, int dex) {
        this.bonusHp    -= hp;
        this.bonusDef   -= def;
        this.bonusStr   -= str;
        this.bonusAgi   -= agi;
        this.bonusIntel -= intel;
        this.bonusDex   -= dex;
    }

    /**
     * Increases upgradeLevel by 1 (up to 5) and then scales base stats
     * by (1 + 0.1×upgradeLevel + rarityBonus).
     */
    public void applyUpgrade() {
        if (upgradeLevel < 5) {
            upgradeLevel++;
            increaseStats();
        }
    }

    /** Multiplies each base stat by the combined upgrade & rarity multiplier. */
    public void increaseStats() {
        double multiplier = 1.0 + (upgradeLevel * 0.1) + getRarityMultiplier();
        baseHp    = (int)(baseHp    * multiplier);
        baseDef   = (int)(baseDef   * multiplier);
        baseStr   = (int)(baseStr   * multiplier);
        baseAgi   = (int)(baseAgi   * multiplier);
        baseIntel = (int)(baseIntel * multiplier);
        baseDex   = (int)(baseDex   * multiplier);
    }

    private double getRarityMultiplier() {
        switch (rarity) {
            case COMMON:    return 0.0;
            case UNCOMMON:  return 0.007;
            case RARE:      return 0.01;
            case EPIC:      return 0.02;
            case LEGENDARY: return 0.03;
            case MYTHIC:    return 0.05;
            case FABLED:    return 0.04; // if you want custom ratio
            default:        return 0.0;
        }
    }
}
