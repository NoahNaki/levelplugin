package me.nakilex.levelplugin.player.level.managers;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
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

        StatsManager.getInstance().initPlayer(player);
    }

    public void addXP(Player player, int amount) {
        if (getLevel(player) >= MAX_LEVEL) return;

        UUID uuid = player.getUniqueId();
        int newXP = getXP(player) + amount;
        playerXp.put(uuid, newXP);

        checkLevelUp(player);
        XPBarHandler.updateXPBar(player, this);
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
            StatsManager.getInstance().addSkillPoints(player, 5);

            // Recalculate xpNeeded for the next level before calling the event
            xpNeeded = level * XP_PER_LEVEL_MULTIPLIER;

            // Only now call the level up event, after xpNeeded has been recalculated
            XPBarHandler.handleLevelUpEvent(player, level, xpNeeded);
        }

        playerLevels.put(uuid, level);
        playerXp.put(uuid, xp);
    }


    private void applyLevelUpBenefits(Player player, int newLevel) {
        double newMaxHealth = player.getMaxHealth() + 1.0;
        player.setMaxHealth(Math.min(newMaxHealth, 40.0));
    }

    public int getLevel(Player player) {
        return playerLevels.getOrDefault(player.getUniqueId(), 1);
    }

    public int getXP(Player player) {
        return playerXp.getOrDefault(player.getUniqueId(), 0);
    }

    public void setLevel(UUID uuid, int newLevel) {
        playerLevels.put(uuid, newLevel);
        playerXp.put(uuid, 0);
    }

    public int getXpNeededForNextLevel(Player player) {
        int currentLevel = getLevel(player);
        if (currentLevel >= MAX_LEVEL) return 0;
        int xpNeeded = currentLevel * XP_PER_LEVEL_MULTIPLIER;

        return xpNeeded;
    }

    public int getMaxLevel() {
        return MAX_LEVEL;
    }
}
