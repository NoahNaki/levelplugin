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

    private final int defaultCooldownSeconds;
    private final Map<Integer, Long> chestExpiration = new HashMap<>();

    public CooldownManager(JavaPlugin plugin, ConfigManager configManager, LootChestManager lootChestManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.lootChestManager = lootChestManager;

        this.defaultCooldownSeconds = configManager.getLootChestsConfig().getInt("cooldowns.default", 5);
    }

    public void setLootChestManager(LootChestManager manager) {
        this.lootChestManager = manager;
    }

    public void startChestCooldown(int chestId) {
        long expireTime = System.currentTimeMillis() + (defaultCooldownSeconds * 1000L);
        chestExpiration.put(chestId, expireTime);

        plugin.getLogger().info("[CooldownManager] Started cooldown for chest " + chestId
            + " for " + defaultCooldownSeconds + "s. Will expire at " + expireTime);

        // +1 second grace (or more if you prefer)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getLogger().info("[CooldownManager] Checking if chest " + chestId + " is expired...");

            if (isExpired(chestId)) {
                plugin.getLogger().info("[CooldownManager] Chest " + chestId + " is expired. Respawning now...");
                lootChestManager.respawnChest(chestId);
                chestExpiration.remove(chestId);
            } else {
                plugin.getLogger().info("[CooldownManager] Chest " + chestId
                    + " is NOT expired yet or something else prevented respawn.");
            }
        }, (defaultCooldownSeconds + 1) * 20L); // 1 second grace
    }



    public boolean isExpired(int chestId) {
        Long time = chestExpiration.get(chestId);
        if (time == null) return true;

        // We'll treat it as expired if we're within 100ms
        long now = System.currentTimeMillis();
        long delta = time - now;
        boolean expired = (delta <= 100);

        plugin.getLogger().info("[CooldownManager] isExpired check for chest " + chestId
            + " => " + expired + " (time left = " + delta + "ms)");

        return expired;
    }

}
