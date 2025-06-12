package me.nakilex.levelplugin.mining.managers;

import me.nakilex.levelplugin.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

/**
 * Simple profession manager for the Mining skill.
 */
public class MiningManager {
    private static MiningManager instance;

    private final Main plugin;
    private final HashMap<UUID, Integer> levels = new HashMap<>();
    private final HashMap<UUID, Integer> xp = new HashMap<>();

    private final int MAX_LEVEL = 100;
    private final int XP_MULT = 50;

    public MiningManager(Main plugin) {
        this.plugin = plugin;
        instance = this;
    }

    public static MiningManager getInstance() {
        return instance;
    }

    public void initializePlayer(Player player) {
        UUID id = player.getUniqueId();
        levels.putIfAbsent(id, 1);
        xp.putIfAbsent(id, 0);
    }

    public void addXP(Player player, int amount) {
        addXP(player.getUniqueId(), amount);
    }

    public void addXP(UUID uuid, int amount) {
        if (getLevel(uuid) >= MAX_LEVEL) return;
        int newXp = getXP(uuid) + amount;
        xp.put(uuid, newXp);
        checkLevelUp(uuid);
    }

    private void checkLevelUp(UUID uuid) {
        int lvl = getLevel(uuid);
        int cur = getXP(uuid);
        int req = getXpRequired(lvl);
        boolean leveled = false;
        while (lvl < MAX_LEVEL && cur >= req) {
            cur -= req;
            lvl++;
            leveled = true;
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.sendMessage("§aMining level up! You are now level §e" + lvl + "§a.");
            }
            req = getXpRequired(lvl);
        }
        levels.put(uuid, lvl);
        xp.put(uuid, cur);
        if (leveled && plugin.getPlayerConfig() != null) {
            plugin.getPlayerConfig().savePlayerData(uuid);
        }
    }

    private int getXpRequired(int level) {
        return level * XP_MULT;
    }

    public int getLevel(Player player) {
        return getLevel(player.getUniqueId());
    }

    public int getLevel(UUID uuid) {
        return levels.getOrDefault(uuid, 1);
    }

    public int getXP(Player player) {
        return getXP(player.getUniqueId());
    }

    public int getXP(UUID uuid) {
        return xp.getOrDefault(uuid, 0);
    }

    public int getXpNeededForNextLevel(Player player) {
        int lvl = getLevel(player);
        if (lvl >= MAX_LEVEL) return 0;
        return getXpRequired(lvl);
    }

    public int getMaxLevel() {
        return MAX_LEVEL;
    }

    public void setLevel(UUID uuid, int level) {
        levels.put(uuid, level);
        xp.put(uuid, 0);
    }
}
