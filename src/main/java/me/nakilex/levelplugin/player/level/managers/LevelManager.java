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
    private final HashMap<UUID, Integer> playerLevels = new HashMap<>();
    private final HashMap<UUID, Integer> playerXp     = new HashMap<>();

    private final int MAX_LEVEL               = 100;
    private final int XP_PER_LEVEL_MULTIPLIER = 100;

    // Tier breakpoints
    private final int TIER1_CAP = 10;   // 1–10
    private final int TIER2_CAP = 30;   // 11–30
    private final int TIER3_CAP = 44;   // 31–44
    private final int TIER4_CAP = 52;   // 45–52  ← new
    private final int TIER5_CAP = 60;   // 53–60  ← new
    private final int TIER6_CAP = 74;   // 61–74
    private final int TIER7_CAP = 90;   // 75–90  ← new

    // Tier multipliers
    private final double TIER1_MULT = 0.5;   // very easy
    private final double TIER2_MULT = 1.0;
    private final double TIER3_MULT = 1.5;
    private final double TIER4_MULT = 2.0;
    private final double TIER5_MULT = 2.75;
    private final double TIER6_MULT = 3.5;
    private final double TIER7_MULT = 4.5;
    private final double TIER8_MULT = 7.5;

    public LevelManager(Main plugin) {
        this.plugin = plugin;
        instance    = this;
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
        playerXp    .putIfAbsent(uuid, 0);

        XPBarHandler.updateXPBar(player, this);
        StatsManager.getInstance().getPlayerStats(uuid);
    }

    // Add XP by UUID
    public void addXP(UUID uuid, int amount) {
        if (getLevel(uuid) >= MAX_LEVEL) return;

        int newXP = getXP(uuid) + amount;
        playerXp.put(uuid, newXP);

        checkLevelUp(uuid);

        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            XPBarHandler.updateXPBar(player, this);
        }
    }

    // Add XP by Player
    public void addXP(Player player, int amount) {
        if (player == null) return;
        addXP(player.getUniqueId(), amount);
    }

    private void checkLevelUp(UUID uuid) {
        int level = getLevel(uuid);
        int xp    = getXP(uuid);

        int xpNeeded = getXpRequired(level);
        while (level < MAX_LEVEL && xp >= xpNeeded) {
            xp -= xpNeeded;
            level++;

            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                applyLevelUpBenefits(player, level);
                StatsManager.getInstance().addSkillPoints(uuid, 5);
                XPBarHandler.handleLevelUpEvent(player, level, xpNeeded);
            }

            xpNeeded = getXpRequired(level);
        }

        playerLevels.put(uuid, level);
        playerXp    .put(uuid, xp);
    }

    /** Returns XP needed to go from “level” → “level+1” */
    private int getXpRequired(int level) {
        int base = level * XP_PER_LEVEL_MULTIPLIER;

        if (level <= TIER1_CAP) {
            return (int)(base * TIER1_MULT);
        } else if (level <= TIER2_CAP) {
            return (int)(base * TIER2_MULT);
        } else if (level <= TIER3_CAP) {
            return (int)(base * TIER3_MULT);
        } else if (level <= TIER4_CAP) {
            return (int)(base * TIER4_MULT);
        } else if (level <= TIER5_CAP) {
            return (int)(base * TIER5_MULT);
        } else if (level <= TIER6_CAP) {
            return (int)(base * TIER6_MULT);
        } else if (level <= TIER7_CAP) {
            return (int)(base * TIER7_MULT);
        } else {
            // levels 91–100
            return (int)(base * TIER8_MULT);
        }
    }

    private void applyLevelUpBenefits(Player player, int newLevel) {
        double newMaxHealth = player.getMaxHealth() + 1.0;
        player.setMaxHealth(Math.min(newMaxHealth, 40.0));
    }

    // --- Getters & Setters ---

    public int getLevel(UUID uuid) {
        return playerLevels.getOrDefault(uuid, 1);
    }

    public int getLevel(Player player) {
        if (player == null) return 1;
        return getLevel(player.getUniqueId());
    }

    public int getXP(UUID uuid) {
        return playerXp.getOrDefault(uuid, 0);
    }

    public int getXP(Player player) {
        if (player == null) return 0;
        return getXP(player.getUniqueId());
    }

    public void setLevel(UUID uuid, int newLevel) {
        playerLevels.put(uuid, newLevel);
        playerXp    .put(uuid, 0);
    }

    /** How much XP to next level for a Player */
    public int getXpNeededForNextLevel(Player player) {
        int level = getLevel(player);
        if (level >= MAX_LEVEL) return 0;
        return getXpRequired(level);
    }

    public int getMaxLevel() {
        return MAX_LEVEL;
    }
}
