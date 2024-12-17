package me.nakilex.levelplugin.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StatsManager {

    private static final StatsManager instance = new StatsManager();
    public static StatsManager getInstance() { return instance; }

    private LevelManager levelManager; // Reference to the actual LevelManager
    private final Map<UUID, PlayerStats> statsMap = new HashMap<>();

    private StatsManager() {}

    /**
     * Link the StatsManager to your LevelManager instance.
     */
    public void setLevelManager(LevelManager levelManager) {
        this.levelManager = levelManager;
    }

    /**
     * A new method to get the player's level via LevelManager
     */
    public int getLevel(Player player) {
        if (levelManager == null) {
            Bukkit.getLogger().warning("[StatsManager] LevelManager is null! Did you call setLevelManager(...) in onEnable()?");
            return 1;
        }
        return levelManager.getLevel(player);
    }

    // Ensure the player has a default record in statsMap
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

    /**
     * Give skill points (e.g., from leveling up or commands).
     */
    public void addSkillPoints(Player player, int points) {
        PlayerStats ps = getPlayerStats(player);
        ps.skillPoints += points;
    }

    /**
     * Increase 1 point in a specific stat.
     */
    public void investStat(Player player, StatType stat) {
        PlayerStats ps = getPlayerStats(player);
        if (ps.skillPoints <= 0) {
            player.sendMessage("§cYou have no skill points left!");
            return;
        }

        ps.skillPoints--;

        switch (stat) {
            case STR: ps.strength++; break;
            case AGI: ps.agility++; break;
            case INT: ps.intelligence++; break;
            case DEX: ps.dexterity++; break;
            case HP:  ps.healthStat++; break;
            case DEF: ps.defenceStat++; break;
        }

        //player.sendMessage("§aYou invested 1 point into " + stat + ".");
        recalcDerivedStats(player);
    }

    /**
     * Refund 1 point from a specific stat, if possible.
     */
    public void refundStat(Player player, StatType stat) {
        PlayerStats ps = getPlayerStats(player);
        boolean refunded = false;

        switch (stat) {
            case STR:
                if (ps.strength > 0) { ps.strength--; refunded = true; }
                break;
            case AGI:
                if (ps.agility > 0) { ps.agility--; refunded = true; }
                break;
            case INT:
                if (ps.intelligence > 0) { ps.intelligence--; refunded = true; }
                break;
            case DEX:
                if (ps.dexterity > 0) { ps.dexterity--; refunded = true; }
                break;
            case HP:
                if (ps.healthStat > 0) { ps.healthStat--; refunded = true; }
                break;
            case DEF:
                if (ps.defenceStat > 0) { ps.defenceStat--; refunded = true; }
                break;
        }

        if (refunded) {
            ps.skillPoints++;
            //player.sendMessage("§aYou refunded 1 point from " + stat + ".");
            recalcDerivedStats(player);
        } else {
            //player.sendMessage("§cYou have no points to refund in " + stat + "!");
        }
    }

    public void refundAllStats(Player player) {
        PlayerStats ps = getPlayerStats(player);

        // Calculate total points invested
        int totalRefundedPoints = ps.strength + ps.agility + ps.intelligence
            + ps.dexterity + ps.healthStat + ps.defenceStat;

        // Reset all stats
        ps.strength = 0;
        ps.agility = 0;
        ps.intelligence = 0;
        ps.dexterity = 0;
        ps.healthStat = 0;
        ps.defenceStat = 0;

        // Add all refunded points back to skillPoints
        ps.skillPoints += totalRefundedPoints;

        // Recalculate derived stats
        recalcDerivedStats(player);

        player.sendMessage(ChatColor.GREEN + "All skill points have been refunded!");
    }


    /**
     * Recalculate the derived stats, including maxHealth & maxMana.
     */
    public void recalcDerivedStats(Player player) {
        PlayerStats ps = getPlayerStats(player);

        // Health
        double newMaxHealth = 20.0 + (ps.healthStat * 2.0);
        newMaxHealth = Math.min(newMaxHealth, 200.0);
        player.setMaxHealth(newMaxHealth);

        // Mana
        ps.maxMana = 50 + (ps.intelligence * 10);
        if (ps.currentMana > ps.maxMana) {
            ps.currentMana = ps.maxMana;
        }

        // Walk speed: base .20f + 0.001 * agility
        float newWalkSpeed = 0.20f + (ps.agility * 0.001f);
        if (newWalkSpeed > 1.0f) newWalkSpeed = 1.0f;
        player.setWalkSpeed(newWalkSpeed);
    }

    /**
     * Called every second in ManaRegenTask, for example.
     */
    public void regenManaForAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerStats ps = getPlayerStats(player);

            // Example formula: base 2.0 + (0.3 * INT)
            double baseRegenPerSec = 2.0;
            double intBonus = ps.intelligence * 0.3;
            double totalRegen = baseRegenPerSec + intBonus;

            ps.currentMana += totalRegen;
            if (ps.currentMana > ps.maxMana) {
                ps.currentMana = ps.maxMana;
            }
        }
    }

    /**
     * Get the current value of a specific stat for a player.
     */
    public int getStatValue(Player player, StatType stat) {
        PlayerStats ps = getPlayerStats(player);

        switch (stat) {
            case STR: return ps.strength;
            case AGI: return ps.agility;
            case INT: return ps.intelligence;
            case DEX: return ps.dexterity;
            case HP:  return ps.healthStat;
            case DEF: return ps.defenceStat;
            default: return 0; // Return 0 if stat type is invalid
        }
    }


    public static class PlayerStats {
        public int healthStat = 0;
        public int strength = 0;
        public int agility = 0;
        public int dexterity = 0;
        public int defenceStat = 0;
        public int intelligence = 0;

        public int maxMana = 50;
        public int currentMana = 50;

        public int skillPoints = 0;

        // Add playerClass, default to VILLAGER
        public PlayerClass playerClass = PlayerClass.VILLAGER;
    }


    public enum StatType {
        STR, AGI, INT, DEX, HP, DEF
    }
}
