package me.nakilex.levelplugin.mob.data;

import java.util.List;

public class MobConfig {

    private final String id;
    private final String entityType;
    private final String name;
    private final int minLevel;
    private final int maxLevel;

    private final double baseHealth;
    private final double healthMultiplier;
    private final double baseDamage;
    private final double damageMultiplier;
    private final double movementSpeed;
    private final int xpDrop; // The XP that the mob gives upon death

    private final List<LootEntry> lootTable;

    public MobConfig(String id, String entityType, String name,
                     int minLevel, int maxLevel,
                     double baseHealth, double healthMultiplier,
                     double baseDamage, double damageMultiplier,
                     double movementSpeed,
                     int xpDrop,
                     List<LootEntry> lootTable) {

        this.id = id;
        this.entityType = entityType;
        this.name = name;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.baseHealth = baseHealth;
        this.healthMultiplier = healthMultiplier;
        this.baseDamage = baseDamage;
        this.damageMultiplier = damageMultiplier;
        this.movementSpeed = movementSpeed;
        this.xpDrop = xpDrop;
        this.lootTable = lootTable;
    }

    public String getId() {
        return id;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getName() {
        return name;
    }

    public int getMinLevel() {
        return minLevel;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public double getBaseHealth() {
        return baseHealth;
    }

    public double getHealthMultiplier() {
        return healthMultiplier;
    }

    public double getBaseDamage() {
        return baseDamage;
    }

    public double getDamageMultiplier() {
        return damageMultiplier;
    }

    public double getMovementSpeed() {
        return movementSpeed;
    }

    public int getXpDrop() {
        return xpDrop;
    }

    public List<LootEntry> getLootTable() {
        return lootTable;
    }

    public static class LootEntry {
        private final String item;
        private final int dropRate;
        private final int dropMin;
        private final int dropMax;

        public LootEntry(String item, int dropRate, int dropMin, int dropMax) {
            this.item = item;
            this.dropRate = dropRate;
            this.dropMin = dropMin;
            this.dropMax = dropMax;
        }

        public String getItem() {
            return item;
        }
        public int getDropRate() {
            return dropRate;
        }
        public int getDropMin() {
            return dropMin;
        }
        public int getDropMax() {
            return dropMax;
        }
    }
}
