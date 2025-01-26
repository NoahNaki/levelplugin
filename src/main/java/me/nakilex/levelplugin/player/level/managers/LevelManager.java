package me.nakilex.levelplugin.player.level.managers;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class LevelManager {

    private static LevelManager instance;

    private final Main plugin;
    private HashMap<UUID, Integer> playerLevels = new HashMap<>();
    private HashMap<UUID, Integer> playerXp = new HashMap<>();

    private final int MAX_LEVEL = 100;
    private final int XP_PER_LEVEL_MULTIPLIER = 100;

    public LevelManager(Main plugin) {
        this.plugin = plugin;
        instance = this;
    }

    public static LevelManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("LevelManager has not been initialized!");
        }
        return instance;
    }

    public void initializePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        playerLevels.putIfAbsent(uuid, 1);
        playerXp.putIfAbsent(uuid, 0);

        XPBarHandler.updateXPBar(player, this);

        StatsManager.getInstance().getPlayerStats(player.getUniqueId());
    }

    // Add XP to a player (UUID-based version)
    public void addXP(UUID uuid, int amount) {
        if (getLevel(uuid) >= MAX_LEVEL) return; // Player is already max level, no XP can be added.

        int newXP = getXP(uuid) + amount;
        playerXp.put(uuid, newXP);

        checkLevelUp(uuid);
    }

    // Add XP to a player (Player-based version)
    public void addXP(Player player, int amount) {
        if (player == null) return; // Do nothing if player is null
        addXP(player.getUniqueId(), amount); // Delegate to UUID-based version
    }

    private void checkLevelUp(UUID uuid) {
        int level = getLevel(uuid);
        int xp = getXP(uuid);

        int xpNeeded = level * XP_PER_LEVEL_MULTIPLIER;
        while (level < MAX_LEVEL && xp >= xpNeeded) {
            xp -= xpNeeded;
            level++;

            // Apply level-up benefits if the player is online
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                applyLevelUpBenefits(player, level);
                StatsManager.getInstance().addSkillPoints(uuid, 5);
                XPBarHandler.handleLevelUpEvent(player, level, xpNeeded);
            }

            xpNeeded = level * XP_PER_LEVEL_MULTIPLIER;
        }

        playerLevels.put(uuid, level);
        playerXp.put(uuid, xp);
    }

    private void applyLevelUpBenefits(Player player, int newLevel) {
        double newMaxHealth = player.getMaxHealth() + 1.0;
        player.setMaxHealth(Math.min(newMaxHealth, 40.0));
    }

    // Get level by UUID
    public int getLevel(UUID uuid) {
        return playerLevels.getOrDefault(uuid, 1);
    }

    // Get level by Player
    public int getLevel(Player player) {
        if (player == null) return 1; // Default level for null player
        return getLevel(player.getUniqueId());
    }

    // Get XP by UUID
    public int getXP(UUID uuid) {
        return playerXp.getOrDefault(uuid, 0);
    }

    // Get XP by Player
    public int getXP(Player player) {
        if (player == null) return 0; // Default XP for null player
        return getXP(player.getUniqueId());
    }

    public void setLevel(UUID uuid, int newLevel) {
        playerLevels.put(uuid, newLevel);
        playerXp.put(uuid, 0); // Reset XP to 0 when level is set manually
    }

    public int getXpNeededForNextLevel(Player player) {
        int currentLevel = getLevel(player);
        if (currentLevel >= MAX_LEVEL) return 0;
        return currentLevel * XP_PER_LEVEL_MULTIPLIER;
    }

    public int getMaxLevel() {
        return MAX_LEVEL;
    }
}
