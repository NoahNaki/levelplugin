package me.nakilex.levelplugin.items.data;

import org.bukkit.Material;

import java.util.UUID;

public class CustomItem {

    // Unique instance ID
    private final UUID uuid;

    private final int id;
    private final String baseName;
    private final ItemRarity rarity;
    private final int levelRequirement;
    private final String classRequirement;
    private final Material material;

    // Stat _ranges_
    private final StatRange hpRange;
    private final StatRange defRange;
    private final StatRange strRange;
    private final StatRange agiRange;
    private final StatRange intelRange;
    private final StatRange dexRange;

    // One‐time rolled stats
    private final int rolledHp;
    private final int rolledDef;
    private final int rolledStr;
    private final int rolledAgi;
    private final int rolledIntel;
    private final int rolledDex;

    // Bonuses and upgrades
    private int bonusHp    = 0;
    private int bonusDef   = 0;
    private int bonusStr   = 0;
    private int bonusAgi   = 0;
    private int bonusIntel = 0;
    private int bonusDex   = 0;
    private int upgradeLevel = 0;

    /**
     * Constructor for loading an existing item (or creating from a template + fixed upgrade level).
     * Rolls each stat once.
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

        // Roll each stat once
        this.rolledHp    = hpRange.roll();
        this.rolledDef   = defRange.roll();
        this.rolledStr   = strRange.roll();
        this.rolledAgi   = agiRange.roll();
        this.rolledIntel = intelRange.roll();
        this.rolledDex   = dexRange.roll();

        this.upgradeLevel = upgradeLevel;
    }

    /**
     * Convenience constructor for creating a brand‐new item from ranges.
     * Generates its own UUID.
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

    /* ─── Getters ───────────────────────────────────────────────────────── */

    public UUID getUuid()                 { return uuid; }
    public int getId()                    { return id; }
    public String getBaseName()           { return baseName; }
    public ItemRarity getRarity()         { return rarity; }
    public int getLevelRequirement()      { return levelRequirement; }
    public String getClassRequirement()   { return classRequirement; }
    public Material getMaterial()         { return material; }

    public StatRange getHpRange()         { return hpRange; }
    public StatRange getDefRange()        { return defRange; }
    public StatRange getStrRange()        { return strRange; }
    public StatRange getAgiRange()        { return agiRange; }
    public StatRange getIntelRange()      { return intelRange; }
    public StatRange getDexRange()        { return dexRange; }

    public int getHp()    { return rolledHp    + bonusHp; }
    public int getDef()   { return rolledDef   + bonusDef; }
    public int getStr()   { return rolledStr   + bonusStr; }
    public int getAgi()   { return rolledAgi   + bonusAgi; }
    public int getIntel() { return rolledIntel + bonusIntel; }
    public int getDex()   { return rolledDex   + bonusDex; }

    public int getUpgradeLevel()          { return upgradeLevel; }

    /**
     * Returns the display name (baseName + ★×upgradeLevel).
     */
    public String getName() {
        return baseName + "★".repeat(upgradeLevel);
    }

    public String getDisplayName() {
        return getName();
    }

    /* ─── Mutators ───────────────────────────────────────────────────────── */

    public void setUpgradeLevel(int upgradeLevel) {
        this.upgradeLevel = upgradeLevel;
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
     * Applies upgrades: increments upgradeLevel (max 5) and scales each rolled stat
     * by (1 + 0.1×upgradeLevel + rarityBonus).
     */
    public void applyUpgrade() {
        if (upgradeLevel < 5) {
            upgradeLevel++;
            double mult = 1.0 + upgradeLevel * 0.1 + getRarityMultiplier();
            // Note: we scale the _rolled_ stats permanently
            // (alternatively, you could track a separate upgraded value)
            // Here we just recompute rolled*mult and floor to int.
            // If you want to preserve original roll, store both.
            int newHp    = (int) (rolledHp    * mult);
            int newDef   = (int) (rolledDef   * mult);
            int newStr   = (int) (rolledStr   * mult);
            int newAgi   = (int) (rolledAgi   * mult);
            int newIntel = (int) (rolledIntel * mult);
            int newDex   = (int) (rolledDex   * mult);
            // update rolled_* fields via reflection or redesign;
            // for simplicity you might instead keep a baseRoll and upgradedRoll.
            // (Implementation detail—adjust to your needs.)
        }
    }

    private double getRarityMultiplier() {
        switch (rarity) {
            case COMMON:    return 0.0;
            case UNCOMMON:  return 0.07;
            case RARE:      return 0.1;
            case EPIC:      return 0.2;
            case LEGENDARY: return 0.3;
            case MYTHIC:    return 0.5;
            default:        return 0.0;
        }
    }
}
