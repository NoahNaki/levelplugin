package me.nakilex.levelplugin.managers;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.ui.XPBarHandler;

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

        // Also init the StatsManager record (so they have default stats).
        // StatsManager is a singleton, so just init the player there
        me.nakilex.levelplugin.managers.StatsManager.getInstance().initPlayer(player);

        // Debugging player initialization
        Bukkit.getLogger().info("[LevelManager] Initialized player: " + player.getName() + ", Level: " + getLevel(player) + ", XP: " + getXP(player));
    }

    public void addXP(Player player, int amount) {
        if (getLevel(player) >= MAX_LEVEL) return;

        UUID uuid = player.getUniqueId();
        int newXP = getXP(player) + amount;
        playerXp.put(uuid, newXP);

        checkLevelUp(player);
        XPBarHandler.updateXPBar(player, this);

        // Debugging XP addition
        Bukkit.getLogger().info("[LevelManager] Added XP: " + amount + " to player: " + player.getName() + ", Total XP: " + newXP);
    }

    private void checkLevelUp(Player player) {
        UUID uuid = player.getUniqueId();
        int level = getLevel(player);
        int xp = getXP(player);

        int xpNeeded = level * XP_PER_LEVEL_MULTIPLIER;
        while (level < MAX_LEVEL && xp >= xpNeeded) {
            xp -= xpNeeded;
            level++;

            applyLevelUpBenefits(player, level);

            // Each time they level up, give 5 skill points
            me.nakilex.levelplugin.managers.StatsManager.getInstance().addSkillPoints(player, 5);

            XPBarHandler.handleLevelUpEvent(player, level);
            xpNeeded = level * XP_PER_LEVEL_MULTIPLIER;

            // Debugging level up
            Bukkit.getLogger().info("[LevelManager] Player: " + player.getName() + " leveled up to: " + level);
        }

        playerLevels.put(uuid, level);
        playerXp.put(uuid, xp);

        // Debugging final XP and level state after level-up check
        Bukkit.getLogger().info("[LevelManager] Final State - Player: " + player.getName() + ", Level: " + level + ", XP: " + xp);
    }

    private void applyLevelUpBenefits(Player player, int newLevel) {
        // Example: +1 max health on level up
        double newMaxHealth = player.getMaxHealth() + 1.0;
        player.setMaxHealth(Math.min(newMaxHealth, 40.0));

        // Debugging level-up benefits
        Bukkit.getLogger().info("[LevelManager] Applied level-up benefits for: " + player.getName() + ", New Max Health: " + player.getMaxHealth());
    }

    public int getLevel(Player player) {
        return playerLevels.getOrDefault(player.getUniqueId(), 1);
    }

    public int getXP(Player player) {
        return playerXp.getOrDefault(player.getUniqueId(), 0);
    }

    public void setLevel(UUID uuid, int newLevel) {
        playerLevels.put(uuid, newLevel);
        // Optionally set XP to 0 or recalc?
        playerXp.put(uuid, 0);

        // Debugging level set manually
        Bukkit.getLogger().info("[LevelManager] Set level for UUID: " + uuid + " to: " + newLevel);
    }

    public int getXpNeededForNextLevel(Player player) {
        int currentLevel = getLevel(player);
        if (currentLevel >= MAX_LEVEL) return 0;
        int xpNeeded = currentLevel * XP_PER_LEVEL_MULTIPLIER;

        // Debugging XP needed for next level
        Bukkit.getLogger().info("[LevelManager] Player: " + player.getName() + ", XP Needed for Next Level: " + xpNeeded);

        return xpNeeded;
    }

    public int getMaxLevel() {
        return MAX_LEVEL;
    }
}
