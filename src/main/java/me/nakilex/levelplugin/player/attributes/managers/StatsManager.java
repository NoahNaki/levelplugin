package me.nakilex.levelplugin.player.attributes.managers;

import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.items.data.CustomItem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.*;

public class StatsManager {

    private static final StatsManager instance = new StatsManager();
    public static StatsManager getInstance() { return instance; }

    private LevelManager levelManager; // Reference to the actual LevelManager
    private final Map<UUID, PlayerStats> statsMap = new HashMap<>();
    private final Map<UUID, Set<Integer>> equippedItemsMap = new HashMap<>();

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

    public Set<Integer> getEquippedItems(UUID uuid){
        return equippedItemsMap.computeIfAbsent(uuid, k -> new HashSet<>());
    }

    public void initPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        if (!statsMap.containsKey(uuid)) {
            PlayerStats defaultStats = new PlayerStats();
            statsMap.put(uuid, defaultStats);
        }
    }

    public PlayerStats getPlayerStats(Player player) {
        initPlayer(player);
        return statsMap.get(player.getUniqueId());
    }

    public void addSkillPoints(Player player, int points) {
        PlayerStats ps = getPlayerStats(player);
        ps.skillPoints += points;
    }

    public void investStat(Player player, StatType stat) {
        PlayerStats ps = getPlayerStats(player);
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
            case HP:  ps.baseHealthStat++; break;
            case DEF: ps.baseDefenceStat++; break;
        }

        recalcDerivedStats(player);
    }

    public void refundStat(Player player, StatType stat) {
        PlayerStats ps = getPlayerStats(player);
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
            case HP:
                if (ps.baseHealthStat > 0) { ps.baseHealthStat--; refunded = true; }
                break;
            case DEF:
                if (ps.baseDefenceStat > 0) { ps.baseDefenceStat--; refunded = true; }
                break;
        }

        if (refunded) {
            ps.skillPoints++;
            recalcDerivedStats(player);
        }
    }

    public void refundAllStats(Player player) {
        PlayerStats ps = getPlayerStats(player);

        int totalRefundedPoints = ps.baseStrength + ps.baseAgility + ps.baseIntelligence
            + ps.baseDexterity + ps.baseHealthStat + ps.baseDefenceStat;

        ps.baseStrength = 0;
        ps.baseAgility = 0;
        ps.baseIntelligence = 0;
        ps.baseDexterity = 0;
        ps.baseHealthStat = 0;
        ps.baseDefenceStat = 0;

        ps.skillPoints += totalRefundedPoints;
        recalcDerivedStats(player);

        // DEBUG:
        Bukkit.getLogger().info("[DEBUG] [StatsManager] " + player.getName()
            + " refunded all stats. skillPoints=" + ps.skillPoints
            + ", baseSTR=" + ps.baseStrength + ", baseAGI=" + ps.baseAgility
            + ", baseINT=" + ps.baseIntelligence + ", baseDEX=" + ps.baseDexterity
            + ", baseHP=" + ps.baseHealthStat + ", baseDEF=" + ps.baseDefenceStat);

        player.sendMessage(ChatColor.GREEN + "All skill points have been refunded!");
    }


    public void recalcDerivedStats(Player player) {
        PlayerStats ps = getPlayerStats(player);

        // Ensure stats can't be negative
        ps.baseHealthStat = Math.max(0, ps.baseHealthStat);
        ps.bonusHealthStat = Math.max(0, ps.bonusHealthStat);

        // Calculate new max health
        double newMaxHealth = 20.0 + ((ps.baseHealthStat + ps.bonusHealthStat) * 2.0);

        // Ensure valid health ranges
        newMaxHealth = Math.max(1.0, Math.min(newMaxHealth, 200.0));

        // Set health safely
        player.setMaxHealth(newMaxHealth);
        player.setHealth(Math.min(player.getHealth(), newMaxHealth));

        // Mana and walk speed calculations
        ps.maxMana = 50 + ((ps.baseIntelligence + ps.bonusIntelligence) * 10);
        if (ps.currentMana > ps.maxMana) {
            ps.currentMana = ps.maxMana;
        }

        float newWalkSpeed = 0.20f + ((ps.baseAgility + ps.bonusAgility) * 0.001f);
        if (newWalkSpeed > 1.0f) newWalkSpeed = 1.0f;
        player.setWalkSpeed(newWalkSpeed);
    }



    public void regenManaForAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerStats ps = getPlayerStats(player);

            double baseRegenPerSec = 2.0;
            double intBonus = (ps.baseIntelligence + ps.bonusIntelligence) * 0.3;
            double totalRegen = baseRegenPerSec + intBonus;

            ps.currentMana += totalRegen;
            if (ps.currentMana > ps.maxMana) {
                ps.currentMana = ps.maxMana;
            }
        }
    }

    // Handle stats application for manual equipping
    public void handleArmorManual(Player player, CustomItem newItem, InventoryClickEvent event) {
        PlayerStats ps = getPlayerStats(player);

        // Apply stats
        ps.bonusHealthStat += newItem.getHp();
        ps.bonusDefenceStat += newItem.getDef();
        ps.bonusStrength += newItem.getStr();
        ps.bonusAgility += newItem.getAgi();
        ps.bonusIntelligence += newItem.getIntel();
        ps.bonusDexterity += newItem.getDex();

        recalcDerivedStats(player);
        Bukkit.getLogger().info("[DEBUG] Manually equipped: " + newItem.getName() + " for " + player.getName());
    }


    public int getStatValue(Player player, StatType stat) {
        PlayerStats ps = getPlayerStats(player);

        switch (stat) {
            case STR: return ps.baseStrength + ps.bonusStrength;
            case AGI: return ps.baseAgility + ps.bonusAgility;
            case INT: return ps.baseIntelligence + ps.bonusIntelligence;
            case DEX: return ps.baseDexterity + ps.bonusDexterity;
            case HP:  return ps.baseHealthStat + ps.bonusHealthStat;
            case DEF: return ps.baseDefenceStat + ps.bonusDefenceStat;
            default: return 0;
        }
    }

    public static class PlayerStats {
        public int baseHealthStat = 0, bonusHealthStat = 0;
        public int baseStrength = 0, bonusStrength = 0;
        public int baseAgility = 0, bonusAgility = 0;
        public int baseDexterity = 0, bonusDexterity = 0;
        public int baseDefenceStat = 0, bonusDefenceStat = 0;
        public int baseIntelligence = 0, bonusIntelligence = 0;

        public int maxMana = 50;
        public int currentMana = 50;
        public int skillPoints = 0;

        public PlayerClass playerClass = PlayerClass.VILLAGER;

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
        STR, AGI, INT, DEX, HP, DEF
    }
}
