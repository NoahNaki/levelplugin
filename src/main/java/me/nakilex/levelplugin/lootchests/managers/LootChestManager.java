package me.nakilex.levelplugin.lootchests.managers;

import me.nakilex.levelplugin.lootchests.config.ConfigManager;
import me.nakilex.levelplugin.lootchests.data.ChestData;
import me.nakilex.levelplugin.lootchests.utils.ParticleUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LootChestManager {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private CooldownManager cooldownManager;  // We'll allow setLootChestManager if there's a circular reference

    // Store ChestData by chest ID
    private final Map<Integer, ChestData> chestsById = new HashMap<>();

    // Tracks which chests are currently spawned
    private final Map<Integer, Location> spawnedChests = new HashMap<>();

    public LootChestManager(JavaPlugin plugin,
                            ConfigManager configManager,
                            CooldownManager cooldownManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.cooldownManager = cooldownManager;

        loadChestDataFromConfig();
        spawnAllChestsOnStartup();
    }

    /**
     * If needed, we can set the cooldownManager after both are constructed
     */
    public void setCooldownManager(CooldownManager cooldownManager) {
        this.cooldownManager = cooldownManager;
    }

    /**
     * Reads lootchests.yml and creates ChestData objects for each entry.
     */
    private void loadChestDataFromConfig() {
        ConfigurationSection rootSection =
            configManager.getLootChestsConfig().getConfigurationSection("loot_chests");
        if (rootSection == null) {
            plugin.getLogger().warning("No 'loot_chests' section found in lootchests.yml!");
            return;
        }

        for (String key : rootSection.getKeys(false)) {
            try {
                int chestId = Integer.parseInt(key);

                // "coordinates" looks like "100, 64, 200"
                String coords = rootSection.getString(key + ".coordinates", "0,0,0");
                String[] split = coords.split(",");
                double x = Double.parseDouble(split[0].trim());
                double y = Double.parseDouble(split[1].trim());
                double z = Double.parseDouble(split[2].trim());

                int tier = rootSection.getInt(key + ".tier", 1);

                ChestData chestData = new ChestData(chestId, x, y, z, tier);
                chestsById.put(chestId, chestData);

            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid chest ID or coordinate format for key: " + key);
            }
        }
    }

    /**
     * Spawns all chests on plugin startup (no cooldown check).
     */
    private void spawnAllChestsOnStartup() {
        for (ChestData chestData : chestsById.values()) {
            spawnChest(chestData);
        }
    }

    /**
     * Create a chest block at the chest's location.
     */
    public void spawnChest(ChestData chestData) {
        Location loc = chestData.toLocation();
        if (loc == null) {
            // Means "rpgworld" isn't loaded or coords invalid
            return;
        }
        Block block = loc.getBlock();
        block.setType(Material.CHEST);
        spawnedChests.put(chestData.getChestId(), loc);

        // Tier-based particles
        ParticleUtils.displayTierParticles(loc, chestData.getTier());
    }

    /**
     * Remove the chest block from the world.
     */
    public void removeChest(int chestId) {
        Location loc = spawnedChests.get(chestId);
        if (loc != null) {
            Block block = loc.getBlock();
            if (block.getType() == Material.CHEST) {
                block.setType(Material.AIR);
            }
            spawnedChests.remove(chestId);
        }
    }

    /**
     * Player opens a chest -> remove immediately, start cooldown, etc.
     */
    public void openChest(int chestId, UUID playerUuid) {
        removeChest(chestId);

        // (Optional) Give loot to player here or in the listener
        // e.g. giveTierLoot(playerUuid, chestsById.get(chestId).getTier());

        // Start cooldown
        if (cooldownManager != null) {
            cooldownManager.startChestCooldown(chestId);
        }
    }

    /**
     * Respawn chest after cooldown.
     */
    public void respawnChest(int chestId) {
        ChestData chestData = chestsById.get(chestId);
        if (chestData != null) {
            spawnChest(chestData);
        }
    }

    /**
     * For /lootchest list command
     */
    public Iterable<ChestData> getAllChestData() {
        return chestsById.values();
    }

    /**
     * Add a new chest at runtime (used by a 'GiveLootChestCommand')
     */
    public void addChestData(ChestData newChest) {
        chestsById.put(newChest.getChestId(), newChest);
    }

    /**
     * Return the ID if a location has one of our spawned chests
     */
    public Integer getChestIdAtLocation(Location location) {
        for (Map.Entry<Integer, Location> entry : spawnedChests.entrySet()) {
            if (entry.getValue().equals(location)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
