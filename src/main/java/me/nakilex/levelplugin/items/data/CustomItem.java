package me.nakilex.levelplugin.items.data;

import me.nakilex.levelplugin.items.listeners.WeaponStatsListener;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.StatType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Set;
import java.util.UUID;

public class CustomItem {

    // Unique instance ID
    private final UUID uuid;

    // Template metadata
    private final int id;
    private String baseName;
    private final ItemRarity rarity;
    private final int levelRequirement;
    private final String classRequirement;
    private final Material material;
    private final boolean ego;
    private final String egoKey;

    private int currentDurability;
    private static final int MAX_DURABILITY = 100;
    private boolean broken;


    // The ranges from which we roll each stat exactly once
    private final StatRange hpRange;
    private final StatRange defRange;
    private final StatRange strRange;
    private final StatRange agiRange;
    private final StatRange intelRange;
    private final StatRange dexRange;
    private final StatRange wilRange;
    private final StatRange tecRange;

    // The mutable base stats (initialized by rolling once from each range)
    private int baseHp;
    private int baseDef;
    private int baseStr;
    private int baseAgi;
    private int baseIntel;
    private int baseDex;
    private int baseWil;
    private int baseTec;

    // Any temporary bonuses (e.g. from enchantments, buffs)
    private int bonusHp    = 0;
    private int bonusDef   = 0;
    private int bonusStr   = 0;
    private int bonusAgi   = 0;
    private int bonusIntel = 0;
    private int bonusDex   = 0;
    private int bonusWil   = 0;
    private int bonusTec   = 0;

    // How many times this item has been upgraded (max 5)
    private int upgradeLevel = 0;
    // How many times the item has been enchanted
    private int enchantCount = 0;

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
                      StatRange wilRange,
                      StatRange tecRange,
                      int upgradeLevel,
                      boolean ego,
                      String egoKey) {
        this.uuid             = uuid;
        this.id               = id;
        this.baseName         = baseName;
        this.rarity           = rarity;
        this.levelRequirement = levelRequirement;
        this.classRequirement = classRequirement;
        this.material         = material;
        this.ego              = ego;
        this.egoKey           = egoKey;

        this.hpRange    = hpRange;
        this.defRange   = defRange;
        this.strRange   = strRange;
        this.agiRange   = agiRange;
        this.intelRange = intelRange;
        this.dexRange   = dexRange;
        this.wilRange   = wilRange;
        this.tecRange   = tecRange;

        // Roll each stat once and store as the mutable base
        this.baseHp    = hpRange.roll();
        this.baseDef   = defRange.roll();
        this.baseStr   = strRange.roll();
        this.baseAgi   = agiRange.roll();
        this.baseIntel = intelRange.roll();
        this.baseDex   = dexRange.roll();
        this.baseWil   = wilRange.roll();
        this.baseTec   = tecRange.roll();

        this.upgradeLevel = upgradeLevel;

        this.currentDurability = MAX_DURABILITY;
        this.broken            = false;
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
                      StatRange dexRange,
                      StatRange wilRange,
                      StatRange tecRange,
                      boolean ego,
                      String egoKey) {
        this(UUID.randomUUID(),
            id, baseName, rarity, levelRequirement, classRequirement, material,
            hpRange, defRange, strRange, agiRange, intelRange, dexRange, wilRange, tecRange,
            0, ego, egoKey);
    }

    // ─── Getters ───────────────────────────────────────────────────────────────

    public UUID getUuid()               { return uuid; }
    public int getId()                  { return id; }
    public String getBaseName()         { return baseName; }
    public ItemRarity getRarity()       { return rarity; }
    public int getLevelRequirement()    { return levelRequirement; }
    public String getClassRequirement() { return classRequirement; }
    public Material getMaterial()       { return material; }
    public boolean isEgo()              { return ego; }
    public String getEgoKey()           { return egoKey; }

    public StatRange getHpRange()    { return hpRange; }
    public StatRange getDefRange()   { return defRange; }
    public StatRange getStrRange()   { return strRange; }
    public StatRange getAgiRange()   { return agiRange; }
    public StatRange getIntelRange() { return intelRange; }
    public StatRange getDexRange()   { return dexRange; }
    public StatRange getWilRange()   { return wilRange; }
    public StatRange getTecRange()   { return tecRange; }

    public int getHp()    { return baseHp    + bonusHp; }
    public int getDef()   { return baseDef   + bonusDef; }
    public int getStr()   { return baseStr   + bonusStr; }
    public int getAgi()   { return baseAgi   + bonusAgi; }
    public int getIntel() { return baseIntel + bonusIntel; }
    public int getDex()   { return baseDex   + bonusDex; }
    public int getWil()   { return baseWil   + bonusWil; }
    public int getTec()   { return baseTec   + bonusTec; }

    public int getUpgradeLevel() { return upgradeLevel; }
    public int getEnchantCount() { return enchantCount; }

    public int getCurrentDurability() {
        return currentDurability;
    }

    /** Returns the hard cap (always 100). */
    public int getMaxDurability() {
        return MAX_DURABILITY;
    }

    /** Returns true if this item’s durability has dropped to 0 (i.e. “broken”). */
    public boolean isBroken() {
        return broken;
    }

    /** Display name with stars for upgrades. */
    public String getName() {
        return baseName + "<glyph:star>".repeat(upgradeLevel);
    }
    public String getDisplayName() { return getName(); }



    // ─── Mutators ─────────────────────────────────────────────────────────────

    public void setUpgradeLevel(int upgradeLevel) {
        this.upgradeLevel = Math.min(5, Math.max(0, upgradeLevel));
    }

    public void setEnchantCount(int enchantCount) {
        this.enchantCount = Math.max(0, enchantCount);
    }

    public void incrementEnchantCount() { this.enchantCount++; }

    public void addBonusStats(int hp, int def, int str, int agi, int intel, int dex, int wil, int tec) {
        adjustBonusStat(StatType.VIT, hp);
        adjustBonusStat(StatType.VIT, def);
        adjustBonusStat(StatType.STR, str);
        adjustBonusStat(StatType.AGI, agi);
        adjustBonusStat(StatType.INT, intel);
        adjustBonusStat(StatType.DEX, dex);
        adjustBonusStat(StatType.WIL, wil);
        adjustBonusStat(StatType.TEC, tec);
    }

    public void removeBonusStats(int hp, int def, int str, int agi, int intel, int dex, int wil, int tec) {
        adjustBonusStat(StatType.VIT, -hp);
        adjustBonusStat(StatType.VIT, -def);
        adjustBonusStat(StatType.STR, -str);
        adjustBonusStat(StatType.AGI, -agi);
        adjustBonusStat(StatType.INT, -intel);
        adjustBonusStat(StatType.DEX, -dex);
        adjustBonusStat(StatType.WIL, -wil);
        adjustBonusStat(StatType.TEC, -tec);
    }

    public void adjustBonusStat(StatType stat, int amount) {
        switch (stat) {
            case STR -> bonusStr += amount;
            case AGI -> bonusAgi += amount;
            case INT -> bonusIntel += amount;
            case DEX -> bonusDex += amount;
            case VIT -> bonusHp += amount;
            case WIL -> bonusWil += amount;
            case TEC -> bonusTec += amount;
        }
    }

    // Setters used for stat rerolls
    public void setBaseHp(int value)    { this.baseHp = value; }
    public void setBaseDef(int value)   { this.baseDef = value; }
    public void setBaseStr(int value)   { this.baseStr = value; }
    public void setBaseAgi(int value)   { this.baseAgi = value; }
    public void setBaseIntel(int value) { this.baseIntel = value; }
    public void setBaseDex(int value)   { this.baseDex = value; }
    public void setBaseWil(int value)   { this.baseWil = value; }
    public void setBaseTec(int value)   { this.baseTec = value; }

    public void setBaseName(String name) { this.baseName = name; }

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

    public void reduceDurability(int amount) {
        reduceDurability(amount, null, null);
    }

    public void reduceDurability(int amount, Player holder, ItemStack stack) {
        if (broken) return; // Already broken—nothing more to do.

        currentDurability = Math.max(0, currentDurability - amount);
        if (currentDurability == 0) {
            broken = true;

            // 1) Determine who currently “holds” this item if not provided
            ItemManager im = ItemManager.getInstance();
            if (holder == null) holder = im.getHolderOf(this.id);

            if (holder != null) {
                UUID puuid = holder.getUniqueId();
                StatsManager statsMgr = StatsManager.getInstance();

                // 2) Only strip stats if that player’s equipped-set still contains this ID
                Set<Integer> equipped = statsMgr.getEquippedItems(puuid);
                if (equipped.contains(this.id)) {
                    Bukkit.getLogger().info(
                        "[CustomItem] WeaponID=" + this.id
                            + " broke while equipped by " + holder.getName()
                            + ". Stripping stats now."
                    );

                    if (stack == null) {
                        stack = ItemUtil.createItemStackFromCustomItem(this, 1, holder);
                    }

                    // 3) Call removeWeaponStats(...) (now public) to subtract all the bonuses
                    new WeaponStatsListener().removeWeaponStats(holder, this, stack);

                    // 4) Remove that ID so WeaponStatsListener never tries again on death/respawn
                    equipped.remove(this.id);

                    // 5) Unregister from ItemManager’s holderMap
                    im.unregisterHolder(this.id);
                }
            }
        }
    }

    public void setDurability(int durability) {
        this.currentDurability = Math.max(0, Math.min(MAX_DURABILITY, durability));
        this.broken = (this.currentDurability == 0);
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
        baseWil   = (int)(baseWil   * multiplier);
        baseTec   = (int)(baseTec   * multiplier);
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
