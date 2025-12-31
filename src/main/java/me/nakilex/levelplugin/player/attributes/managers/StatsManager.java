package me.nakilex.levelplugin.player.attributes.managers;

import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.items.data.CustomItem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.*;
import java.util.stream.Collectors;

public class StatsManager {

    private static final StatsManager instance = new StatsManager();
    public static StatsManager getInstance() { return instance; }

    private LevelManager levelManager; // Reference to the actual LevelManager
    private final Map<UUID, PlayerStats> statsMap = new HashMap<>();
    private final Map<UUID, Set<Integer>> equippedItemsMap = new HashMap<>();

    // ——— Health Scaling Constants ———
    public static final double BASE_HEALTH = 20.0;
    public static final double HEALTH_PER_VITALITY = 0.4;
    public static final double HEALTH_PER_STRENGTH = 0.1;
    private static final double REGEN_STAT_DIVISOR = 25.0;
    private static final int ESSENCE_SLOT_THREE_LEVEL = 50;

    public StatsManager() {}

    public void setLevelManager(LevelManager levelManager) {
        this.levelManager = levelManager;
    }

    public int getLevel(Player player) {
        if (levelManager == null) {
            Bukkit.getLogger().warning("[StatsManager] LevelManager is null! Did you call setLevelManager(...) in onEnable()?");
            return 1;
        }
        return levelManager.getLevel(player);
    }

    public int getUnlockedEssenceSlots(Player player) {
        PlayerStats stats = getPlayerStats(player.getUniqueId());
        int unlocked = 1;
        if (stats.secondEssenceSlotUnlocked) {
            unlocked++;
        }
        if (getLevel(player) >= ESSENCE_SLOT_THREE_LEVEL) {
            unlocked++;
        }
        return Math.min(unlocked, stats.essenceSlots.length);
    }

    public boolean isEssenceSlotUnlocked(Player player, int slotIndex) {
        PlayerStats stats = getPlayerStats(player.getUniqueId());
        return switch (slotIndex) {
            case 0 -> true;
            case 1 -> stats.secondEssenceSlotUnlocked;
            case 2 -> getLevel(player) >= ESSENCE_SLOT_THREE_LEVEL;
            default -> false;
        };
    }

    public int getEssenceSlotUnlockLevel(int slotIndex) {
        if (slotIndex == 2) {
            return ESSENCE_SLOT_THREE_LEVEL;
        }
        return Integer.MAX_VALUE;
    }

    public void unlockSecondEssenceSlot(UUID uuid) {
        PlayerStats stats = getPlayerStats(uuid);
        stats.secondEssenceSlotUnlocked = true;
    }

    public boolean hasSecondEssenceSlotUnlocked(UUID uuid) {
        return getPlayerStats(uuid).secondEssenceSlotUnlocked;
    }

    public Set<Integer> getEquippedItems(UUID playerUuid) {
        return equippedItemsMap.computeIfAbsent(playerUuid, k -> new HashSet<>());
    }

    public Set<UUID> getAllPlayerUUIDs() {
        return statsMap.keySet();
    }


    public void initPlayer(UUID uuid) {
        statsMap.putIfAbsent(uuid, new PlayerStats());
    }

    /** Completely resets a player's stats to defaults. */
    public void resetPlayer(UUID uuid) {
        statsMap.put(uuid, new PlayerStats());
    }

    public PlayerStats getPlayerStats(UUID uuid) {
        return statsMap.computeIfAbsent(uuid, k -> new PlayerStats());
    }


    public void addSkillPoints(UUID uuid, int points) {
        PlayerStats ps = getPlayerStats(uuid);
        ps.skillPoints += points;
    }

    public int getSkillPoints(UUID uuid) {
        return getPlayerStats(uuid).skillPoints;
    }


    public void investStat(Player player, StatType stat) {
        PlayerStats ps = getPlayerStats(player.getUniqueId());
        if (ps.skillPoints <= 0) {
            player.sendMessage("§cYou have no skill points left!");
            return;
        }

        ps.skillPoints--;

        switch (stat) {
            case STR: ps.baseStrength++; break;
            case AGI: ps.baseAgility++; break;
            case INT: ps.baseIntelligence++; break;
            case DEX: ps.baseDexterity++; break;
            case VIT: ps.baseVitality++; break;
            case WIL: ps.baseWill++; break;
            case TEC: ps.baseTechnique++; break;
        }

        recalcDerivedStats(player);
    }

    public void refundStat(Player player, StatType stat) {
        PlayerStats ps = getPlayerStats(player.getUniqueId());
        boolean refunded = false;

        switch (stat) {
            case STR:
                if (ps.baseStrength > 0) { ps.baseStrength--; refunded = true; }
                break;
            case AGI:
                if (ps.baseAgility > 0) { ps.baseAgility--; refunded = true; }
                break;
            case INT:
                if (ps.baseIntelligence > 0) { ps.baseIntelligence--; refunded = true; }
                break;
            case DEX:
                if (ps.baseDexterity > 0) { ps.baseDexterity--; refunded = true; }
                break;
            case VIT:
                if (ps.baseVitality > 0) { ps.baseVitality--; refunded = true; }
                break;
            case WIL:
                if (ps.baseWill > 0) { ps.baseWill--; refunded = true; }
                break;
            case TEC:
                if (ps.baseTechnique > 0) { ps.baseTechnique--; refunded = true; }
                break;
        }

        if (refunded) {
            ps.skillPoints++;
            recalcDerivedStats(player);
        }
    }

    public void refundAllStats(Player player) {
        PlayerStats ps = getPlayerStats(player.getUniqueId());

        int totalRefundedPoints = ps.baseStrength + ps.baseAgility + ps.baseIntelligence
            + ps.baseDexterity + ps.baseVitality + ps.baseWill + ps.baseTechnique;

        ps.baseStrength = 0;
        ps.baseAgility = 0;
        ps.baseIntelligence = 0;
        ps.baseDexterity = 0;
        ps.baseVitality = 0;
        ps.baseWill = 0;
        ps.baseTechnique = 0;

        ps.skillPoints += totalRefundedPoints;
        recalcDerivedStats(player);

        player.sendMessage(ChatColor.GREEN + "All skill points have been refunded!");
    }

    /** Set the base value of a stat directly, bypassing skill point checks. */
    public void setBaseStat(PlayerStats ps, StatType stat, int value) {
        switch (stat) {
            case STR -> ps.baseStrength = value;
            case AGI -> ps.baseAgility = value;
            case INT -> ps.baseIntelligence = value;
            case DEX -> ps.baseDexterity = value;
            case VIT -> ps.baseVitality = value;
            case WIL -> ps.baseWill = value;
            case TEC -> ps.baseTechnique = value;
        }
    }

    /** Increment a base stat directly and refresh derived attributes. */
    public void addBaseStat(UUID uuid, StatType stat, int amount) {
        if (amount == 0) {
            return;
        }
        PlayerStats ps = getPlayerStats(uuid);
        switch (stat) {
            case STR -> ps.baseStrength = Math.max(0, ps.baseStrength + amount);
            case AGI -> ps.baseAgility = Math.max(0, ps.baseAgility + amount);
            case INT -> ps.baseIntelligence = Math.max(0, ps.baseIntelligence + amount);
            case DEX -> ps.baseDexterity = Math.max(0, ps.baseDexterity + amount);
            case VIT -> ps.baseVitality = Math.max(0, ps.baseVitality + amount);
            case WIL -> ps.baseWill = Math.max(0, ps.baseWill + amount);
            case TEC -> ps.baseTechnique = Math.max(0, ps.baseTechnique + amount);
        }
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            recalcDerivedStats(player);
        }
    }

    /** Unlock a class for the given player without switching to it. */
    public void unlockClass(UUID uuid, PlayerClass pc) {
        PlayerStats ps = getPlayerStats(uuid);
        ps.unlockedClasses.add(pc);
    }

    /** Remove a class from a player's unlocked set. */
    public void lockClass(UUID uuid, PlayerClass pc) {
        PlayerStats ps = getPlayerStats(uuid);
        ps.unlockedClasses.remove(pc);
    }

    /** Unlock every available class for the given player. */
    public void unlockAllClasses(UUID uuid) {
        PlayerStats ps = getPlayerStats(uuid);
        for (PlayerClass pc : PlayerClass.values()) {
            ps.unlockedClasses.add(pc);
        }
    }


    public void recalcDerivedStats(Player player) {
        PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());

        // Ensure stats are not negative
        ps.baseVitality     = Math.max(0, ps.baseVitality);
        ps.bonusVitality    = Math.max(0, ps.bonusVitality);
        ps.baseStrength     = Math.max(0, ps.baseStrength);
        ps.bonusStrength    = Math.max(0, ps.bonusStrength);
        ps.baseAgility      = Math.max(0, ps.baseAgility);
        ps.bonusAgility     = Math.max(0, ps.bonusAgility);
        ps.baseDexterity    = Math.max(0, ps.baseDexterity);
        ps.bonusDexterity   = Math.max(0, ps.bonusDexterity);
        ps.baseWill         = Math.max(0, ps.baseWill);
        ps.bonusWill        = Math.max(0, ps.bonusWill);
        ps.baseTechnique    = Math.max(0, ps.baseTechnique);
        ps.bonusTechnique   = Math.max(0, ps.bonusTechnique);
        ps.baseIntelligence = Math.max(0, ps.baseIntelligence);
        ps.bonusIntelligence = Math.max(0, ps.bonusIntelligence);

        // Store the current health ratio (current health / old max health)
        double oldMaxHealth = player.getMaxHealth();
        double oldHealth = player.getHealth();
        double healthRatio = oldHealth / oldMaxHealth;

        // Calculate the new max health based on the health stats
        double totalVitality = ps.baseVitality + ps.bonusVitality;
        double totalStrength = ps.baseStrength + ps.bonusStrength;
        double newMaxHealth = BASE_HEALTH
                + (totalVitality * HEALTH_PER_VITALITY)
                + (totalStrength * HEALTH_PER_STRENGTH);
        newMaxHealth = Math.max(1.0, Math.min(newMaxHealth, 9999999.0));

        // Set the new max health
        player.setMaxHealth(newMaxHealth);

        // Adjust the player's current health so the percentage stays the same.
        double newHealth = newMaxHealth * healthRatio;
        player.setHealth(Math.max(1.0, newHealth));

        // Apply health scaling so the visual health bar remains at 20 health (10 hearts)
        player.setHealthScaled(true);
        player.setHealthScale(20.0);

        // Recalculate other derived stats (e.g., mana, walk speed) as needed.
        ps.maxMana = 50
                + ((ps.baseIntelligence + ps.bonusIntelligence) * 1)
                + ((ps.baseWill + ps.bonusWill) * 3);
        if (ps.currentMana > ps.maxMana) {
            ps.currentMana = ps.maxMana;
        }

        float newWalkSpeed = 0.20f + ((ps.baseAgility + ps.bonusAgility) * 0.0006f);
        if (newWalkSpeed > 1.0f) newWalkSpeed = 1.0f;
        player.setWalkSpeed(newWalkSpeed);

        ps.attackSpeed = 0.5 * (1.0 + 0.01 * (ps.baseTechnique + ps.bonusTechnique));
        AttributeInstance atkAttr = player.getAttribute(Attribute.ATTACK_SPEED);
        if (atkAttr != null) atkAttr.setBaseValue(ps.attackSpeed * 8.0);
    }

    public void regenHealthForAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerStats ps = getPlayerStats(player.getUniqueId());

            double baseRegenPerSec = 1.0;
            double regenFromStats = (ps.baseVitality + ps.bonusVitality
                    + ps.baseStrength + ps.bonusStrength) / REGEN_STAT_DIVISOR;
            double regenFromMaxHealth = player.getMaxHealth() / 100.0;

            double totalRegen = baseRegenPerSec + regenFromStats + regenFromMaxHealth;
            double newHealth = player.getHealth() + totalRegen;
            player.setHealth(Math.min(newHealth, player.getMaxHealth()));
        }
    }




    public void regenManaForAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerStats ps = getPlayerStats(player.getUniqueId());

            double baseRegenPerSec = 2.5;
            double willBonus = (ps.baseWill + ps.bonusWill) * 0.25;
            double totalRegen = baseRegenPerSec + willBonus;

            ps.currentMana += totalRegen;
            if (ps.currentMana > ps.maxMana) {
                ps.currentMana = ps.maxMana;
            }
        }
    }

    // Handle stats application for manual equipping
    public void handleArmorManual(Player player, CustomItem newItem, InventoryClickEvent event) {
        PlayerStats ps = getPlayerStats(player.getUniqueId());

        // Apply stats
        ps.bonusVitality     += newItem.getHp();
        ps.bonusStrength     += newItem.getStr();
        ps.bonusAgility      += newItem.getAgi();
        ps.bonusIntelligence += newItem.getIntel();
        ps.bonusDexterity    += newItem.getDex();
        ps.bonusWill         += newItem.getWil();
        ps.bonusTechnique    += newItem.getTec();

        recalcDerivedStats(player);
    }


    public int getStatValue(Player player, StatType stat) {
        PlayerStats ps = getPlayerStats(player.getUniqueId());

        switch (stat) {
            case STR: return ps.baseStrength + ps.bonusStrength;
            case AGI: return ps.baseAgility + ps.bonusAgility;
            case INT: return ps.baseIntelligence + ps.bonusIntelligence;
            case DEX: return ps.baseDexterity + ps.bonusDexterity;
            case VIT: return ps.baseVitality + ps.bonusVitality;
            case WIL: return ps.baseWill + ps.bonusWill;
            case TEC: return ps.baseTechnique + ps.bonusTechnique;
            default: return 0;
        }
    }

    public static class PlayerStats {
        public int baseVitality = 0, bonusVitality = 0;
        public int baseStrength = 0, bonusStrength = 0;
        public int baseAgility = 0, bonusAgility = 0;
        public int baseDexterity = 0, bonusDexterity = 0;
        public int baseIntelligence = 0, bonusIntelligence = 0;
        public int baseWill = 0, bonusWill = 0;
        public int baseTechnique = 0, bonusTechnique = 0;

        public int maxMana = 50;
        public int currentMana = 50;
        public double attackSpeed = 0.5; // attacks per second
        public int skillPoints = 0;

        public PlayerClass playerClass = PlayerClass.VILLAGER;

        public Set<PlayerClass> unlockedClasses = new HashSet<>();

        // Up to three slotted class essences for this player
        public final org.bukkit.inventory.ItemStack[] essenceSlots = new org.bukkit.inventory.ItemStack[3];
        public final boolean[] equippedEssences = new boolean[3];
        public boolean secondEssenceSlotUnlocked = false;

        public PlayerStats() {
            unlockedClasses.add(playerClass);
        }

        public int getCurrentMana() {
            return currentMana;
        }

        public int getMaxMana() {
            return maxMana;
        }

        public void setCurrentMana(int currentMana) {
            this.currentMana = Math.max(0, Math.min(currentMana, maxMana)); // Ensure it's within bounds
        }


    }

        public enum StatType {
        STR("Strength", "str"),
        AGI("Agility", "agi"),
        INT("Intelligence", "int"),
        DEX("Dexterity", "dex"),
        VIT("Vitality", "vit"),
        WIL("Will", "wil"),
        TEC("Technique", "tec");

        private final String displayName;
        private final String abbrev;

        StatType(String displayName, String abbrev) {
            this.displayName = displayName;
            this.abbrev = abbrev;
        }

        public String getDisplayName() { return displayName; }

        public String getAbbrev() { return abbrev; }

        private static final Map<String, StatType> BY_ABBREV = Arrays.stream(values())
                .collect(Collectors.toMap(stat -> stat.abbrev, stat -> stat));

        public static StatType fromAbbrev(String abbrev) {
            return BY_ABBREV.get(abbrev.toLowerCase());
        }

        /**
         * Maps configuration keys or common stat names to a {@link StatType}.
         * Accepts full names ("strength"), abbreviations ("str"), and legacy
         * identifiers like "hp" or "defense".
         *
         * @param key string from configuration
         * @return matching stat type or {@code null} if none
         */
        public static StatType fromKey(String key) {
            if (key == null) return null;
            return switch (key.toLowerCase()) {
                case "strength", "str" -> STR;
                case "agility", "agi" -> AGI;
                case "intelligence", "int" -> INT;
                case "dexterity", "dex" -> DEX;
                case "vitality", "vit", "hp", "defense", "def" -> VIT;
                case "will", "wil" -> WIL;
                case "technique", "tec" -> TEC;
                default -> null;
            };
        }

        /**
         * Preferred display ordering for stats when shown in tooltips.
         * Vitality first followed by the six primary attributes.
         */
        public static final List<StatType> DISPLAY_ORDER =
                java.util.List.of(VIT, STR, AGI, INT, DEX, WIL, TEC);
    }
}
