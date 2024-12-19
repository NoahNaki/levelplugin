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
    private int hp;
    private int def;
    private int str;
    private int agi;
    private int intel;
    private int dex;

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
        this.hp = hp;
        this.def = def;
        this.str = str;
        this.agi = agi;
        this.intel = intel;
        this.dex = dex;
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
        return hp;
    }

    public int getDef() {
        return def;
    }

    public int getStr() {
        return str;
    }

    public int getAgi() {
        return agi;
    }

    public int getIntel() {
        return intel;
    }

    public int getDex() {
        return dex;
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

    // Increases stats based on rarity and upgrade level
    public void increaseStats() {
        double multiplier = 1.0 + (upgradeLevel * 0.1) + getRarityMultiplier();

        this.hp = (int) (hp * multiplier);
        this.def = (int) (def * multiplier);
        this.str = (int) (str * multiplier);
        this.agi = (int) (agi * multiplier);
        this.intel = (int) (intel * multiplier);
        this.dex = (int) (dex * multiplier);
    }

    // Helper method to calculate rarity multiplier
    private double getRarityMultiplier() {
        switch (rarity) {
            case COMMON:
                return 0.0;
            case UNCOMMON:
                return 0.1;
            case RARE:
                return 0.2;
            case EPIC:
                return 0.3;
            case LEGENDARY:
                return 0.5;
            default:
                return 0.0;
        }
    }

    // Generates the display name with stars based on upgrade level
    public String getDisplayName() {
        return getName(); // Uses the getName method for consistent naming
    }

    // Converts the item's current stats into a readable string for item lore
    public List<String> generateLore() {
        List<String> lore = new ArrayList<>();
        lore.add("§7HP: §a" + hp);
        lore.add("§7Defense: §a" + def);
        lore.add("§7Strength: §a" + str);
        lore.add("§7Agility: §a" + agi);
        lore.add("§7Intelligence: §a" + intel);
        lore.add("§7Dexterity: §a" + dex);
        lore.add("§7Upgrade Level: §a" + upgradeLevel + "/5");
        return lore;
    }

    // Updates stats and lore after an upgrade
    public void applyUpgrade() {
        if (upgradeLevel < 5) { // Ensure the upgrade level is within the limit
            upgradeLevel++;
            increaseStats(); // Recalculate stats based on new upgrade level
        }
    }
}
