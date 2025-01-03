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

    private int upgradeLevel = 0;

    // Constructor for NEW item instance with a unique UUID
    public CustomItem(int id, String baseName, ItemRarity rarity, int levelRequirement, String classRequirement,
                      Material material, int hp, int def, int str, int agi, int intel, int dex) {
        this.uuid = UUID.randomUUID(); // Generate unique UUID
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

    // Constructor for LOADING existing item with UUID
    public CustomItem(UUID uuid, int id, String baseName, ItemRarity rarity, int levelRequirement, String classRequirement,
                      Material material, int hp, int def, int str, int agi, int intel, int dex, int upgradeLevel) {
        this.uuid = uuid;
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
        this.upgradeLevel = upgradeLevel;
    }

    // Getter for UUID
    public UUID getUuid() {
        return uuid;
    }

    // Existing getters and setters remain unchanged...

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
        String stars = "★".repeat(upgradeLevel);
        return baseName + stars;
    }

    public void setUpgradeLevel(int upgradeLevel) {
        this.upgradeLevel = upgradeLevel;
    }

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

    public void increaseStats() {
        double multiplier = 1.0 + (upgradeLevel * 0.1) + getRarityMultiplier();
        this.baseHp = (int) (baseHp * multiplier);
        this.baseDef = (int) (baseDef * multiplier);
        this.baseStr = (int) (baseStr * multiplier);
        this.baseAgi = (int) (baseAgi * multiplier);
        this.baseIntel = (int) (baseIntel * multiplier);
        this.baseDex = (int) (baseDex * multiplier);
    }

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

    public String getDisplayName() {
        return getName();
    }

    public void applyUpgrade() {
        if (upgradeLevel < 5) {
            upgradeLevel++;
            increaseStats();
        }
    }
}
