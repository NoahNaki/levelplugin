package me.nakilex.levelplugin.items;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public class CustomItem {

    private final int id;
    private final String baseName; // Store the original name
    private final ItemRarity rarity;
    private final int levelRequirement;
    private final String classRequirement;  // WARRIOR, ROGUE, etc., or ANY
    private final Material material;

    // Stats
    private int baseHp;
    private int baseDef;
    private int baseStr;
    private int baseAgi;
    private int baseIntel;
    private int baseDex;

    private int bonusHp = 0;
    private int bonusDef = 0;
    private int bonusStr = 0;
    private int bonusAgi = 0;
    private int bonusIntel = 0;
    private int bonusDex = 0;

    // Current level of the item (starts at 0 by default)
    private int upgradeLevel = 0;

    public CustomItem(int id, String baseName, ItemRarity rarity, int levelRequirement, String classRequirement,
                      Material material, int hp, int def, int str, int agi, int intel, int dex) {
        this.id = id;
        this.baseName = baseName;
        this.rarity = rarity;
        this.levelRequirement = levelRequirement;
        this.classRequirement = classRequirement;
        this.material = material;
        this.baseHp = hp;
        this.baseDef = def;
        this.baseStr = str;
        this.baseAgi = agi;
        this.baseIntel = intel;
        this.baseDex = dex;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getBaseName() {
        return baseName;
    }

    public ItemRarity getRarity() {
        return rarity;
    }

    public int getLevelRequirement() {
        return levelRequirement;
    }

    public String getClassRequirement() {
        return classRequirement;
    }

    public Material getMaterial() {
        return material;
    }

    public int getHp() {
        return baseHp + bonusHp;
    }

    public int getDef() {
        return baseDef + bonusDef;
    }

    public int getStr() {
        return baseStr + bonusStr;
    }

    public int getAgi() {
        return baseAgi + bonusAgi;
    }

    public int getIntel() {
        return baseIntel + bonusIntel;
    }

    public int getDex() {
        return baseDex + bonusDex;
    }

    public int getUpgradeLevel() {
        return upgradeLevel;
    }

    public String getName() {
        // Returns the display name with upgrade stars
        String stars = "★".repeat(upgradeLevel);
        return baseName + stars;
    }

    // Setters
    public void setUpgradeLevel(int upgradeLevel) {
        this.upgradeLevel = upgradeLevel;
    }

    // Adds bonus stats from items or temporary effects
    public void addBonusStats(int hp, int def, int str, int agi, int intel, int dex) {
        this.bonusHp += hp;
        this.bonusDef += def;
        this.bonusStr += str;
        this.bonusAgi += agi;
        this.bonusIntel += intel;
        this.bonusDex += dex;
    }

    public void removeBonusStats(int hp, int def, int str, int agi, int intel, int dex) {
        this.bonusHp -= hp;
        this.bonusDef -= def;
        this.bonusStr -= str;
        this.bonusAgi -= agi;
        this.bonusIntel -= intel;
        this.bonusDex -= dex;
    }

    // Increases base stats based on rarity and upgrade level
    public void increaseStats() {
        double multiplier = 1.0 + (upgradeLevel * 0.1) + getRarityMultiplier();

        this.baseHp = (int) (baseHp * multiplier);
        this.baseDef = (int) (baseDef * multiplier);
        this.baseStr = (int) (baseStr * multiplier);
        this.baseAgi = (int) (baseAgi * multiplier);
        this.baseIntel = (int) (baseIntel * multiplier);
        this.baseDex = (int) (baseDex * multiplier);
    }

    // Helper method to calculate rarity multiplier
    private double getRarityMultiplier() {
        switch (rarity) {
            case COMMON:
                return 0.0;
            case UNCOMMON:
                return 0.07;
            case RARE:
                return 0.1;
            case EPIC:
                return 0.2;
            case LEGENDARY:
                return 0.3;
            case MYTHIC:
                return 0.5;
            default:
                return 0.0;
        }
    }

    // Generates the display name with stars based on upgrade level
    public String getDisplayName() {
        return getName(); // Uses the getName method for consistent naming
    }

    // Updates stats and lore after an upgrade
    public void applyUpgrade() {
        if (upgradeLevel < 5) { // Ensure the upgrade level is within the limit
            upgradeLevel++;
            increaseStats(); // Recalculate stats based on new upgrade level
        }
    }
}
