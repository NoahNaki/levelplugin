package me.nakilex.levelplugin.lootchests.managers;

import me.nakilex.levelplugin.lootchests.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class CooldownManager {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private LootChestManager lootChestManager;

    // If each chest or tier can have its own cooldown, we can store it in a Map.
    // For a single global cooldown, we just keep an int or long.
    private final int defaultCooldownSeconds;

    // Stores the timestamp when a chest will be ready to respawn.
    // Key: chestId, Value: time in milliseconds (System.currentTimeMillis)
    private final Map<Integer, Long> chestCooldownExpiration = new HashMap<>();

    public CooldownManager(JavaPlugin plugin,
                           ConfigManager configManager,
                           LootChestManager lootChestManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.lootChestManager = lootChestManager;

        // Read default from lootchests.yml => "cooldowns.default" or fallback
        this.defaultCooldownSeconds = configManager.getLootChestsConfig()
            .getInt("cooldowns.default", 600); // 600 = 10 mins default
    }

    /**
     * Start the cooldown timer for a chest that was just opened.
     * After 'defaultCooldownSeconds' pass, respawn the chest.
     */
    public void startChestCooldown(int chestId) {
        // 1. Calculate expiration time
        long expirationTime = System.currentTimeMillis() + (defaultCooldownSeconds * 1000L);
        chestCooldownExpiration.put(chestId, expirationTime);

        // 2. Schedule a task to re-spawn the chest after the cooldown
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Double-check if the chest is actually expired before respawning
            // (In case of a reload or some other edge case.)
            if (isChestCooldownExpired(chestId)) {
                lootChestManager.respawnChest(chestId);
                chestCooldownExpiration.remove(chestId);
            }
        }, defaultCooldownSeconds * 20L); // 20 ticks = 1 second
    }

    public void setLootChestManager(LootChestManager lootChestManager) {
        this.lootChestManager = lootChestManager;
    }


    /**
     * Checks whether the cooldown for a chest is expired.
     */
    public boolean isChestCooldownExpired(int chestId) {
        Long expireTime = chestCooldownExpiration.get(chestId);
        if (expireTime == null) {
            // No cooldown info => either never started or already expired
            return true;
        }
        return System.currentTimeMillis() >= expireTime;
    }

    /**
     * Gets the time (in seconds) until a chest's cooldown is finished.
     * (Optional method if you want to display how long is left.)
     */
    public int getTimeLeftSeconds(int chestId) {
        if (isChestCooldownExpired(chestId)) {
            return 0;
        }
        long expirationTime = chestCooldownExpiration.get(chestId);
        long millisLeft = expirationTime - System.currentTimeMillis();
        return (int) (millisLeft / 1000L);
    }
}
