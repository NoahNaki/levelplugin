package me.nakilex.levelplugin.lootchests.managers;

import me.nakilex.levelplugin.lootchests.config.ConfigManager;
import me.nakilex.levelplugin.lootchests.data.ChestData;
import me.nakilex.levelplugin.lootchests.utils.ParticleUtils;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class LootChestManager {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private CooldownManager cooldownManager;

    // Each chest’s data (ID -> ChestData)
    private final Map<Integer, ChestData> chestsById = new HashMap<>();

    // Track where we actually spawned each chest. chestId -> location
    private final Map<Integer, Location> spawnedChests = new HashMap<>();

    // For continuous particles: chestId -> repeating task
    private final Map<Integer, org.bukkit.scheduler.BukkitTask> chestParticleTasks = new HashMap<>();

    public LootChestManager(JavaPlugin plugin, ConfigManager configManager, CooldownManager cooldownManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.cooldownManager = cooldownManager;

        loadChestDataFromConfig();
        spawnAllChestsOnStartup();
    }

    public void setCooldownManager(CooldownManager manager) {
        this.cooldownManager = manager;
    }

    // 1) Load from lootchests.yml
    private void loadChestDataFromConfig() {
        ConfigurationSection root = configManager.getLootChestsConfig().getConfigurationSection("loot_chests");
        if (root == null) {
            plugin.getLogger().warning("No 'loot_chests' section found!");
            return;
        }
        for (String key : root.getKeys(false)) {
            try {
                int chestId = Integer.parseInt(key);
                String coords = root.getString(key + ".coordinates", "0,0,0");
                String[] split = coords.split(",");
                double x = Double.parseDouble(split[0].trim());
                double y = Double.parseDouble(split[1].trim());
                double z = Double.parseDouble(split[2].trim());
                int tier = root.getInt(key + ".tier", 1);

                ChestData data = new ChestData(chestId, x, y, z, tier);
                chestsById.put(chestId, data);
            } catch (Exception e) {
                plugin.getLogger().warning("Error loading chest ID: " + key);
            }
        }
    }

    // 2) Spawn all on startup
    private void spawnAllChestsOnStartup() {
        for (ChestData data : chestsById.values()) {
            spawnChest(data);
        }
    }

    public void spawnChest(ChestData data) {
        plugin.getLogger().info("[LootChestManager] spawnChest: Attempting to place chestId "
            + data.getChestId() + " at " + data.getX() + ", " + data.getY() + ", " + data.getZ());

        Location loc = data.toLocation();
        if (loc == null) {
            plugin.getLogger().info("[LootChestManager] spawnChest: location was null. Possibly invalid or world not loaded?");
            return;
        }

        // Replace existing block with a CHEST
        Block block = loc.getBlock();
        block.setType(Material.CHEST);

        // IMPORTANT: Track it in spawnedChests so we can find it in InventoryCloseEvent
        spawnedChests.put(data.getChestId(), loc);

        plugin.getLogger().info("[LootChestManager] Placed CHEST for chestId "
            + data.getChestId() + " at " + loc);

        // Optionally add random loot
        org.bukkit.block.Chest chestState = (org.bukkit.block.Chest) block.getState();
        chestState.getBlockInventory().clear();

        ItemStack loot = getRandomLootForTier(data.getTier());
        if (loot != null) {
            plugin.getLogger().info("[LootChestManager] Adding random loot "
                + loot.getType() + " to chest " + data.getChestId());
            chestState.getBlockInventory().addItem(loot);
        } else {
            plugin.getLogger().info("[LootChestManager] No loot found for tier " + data.getTier());
        }
        chestState.update(true);

        // Start continuous particle task
        startParticleTask(data.getChestId(), loc, data.getTier());

        // Right after you do chestState.getBlockInventory().addItem(loot)
        Map<Integer, ItemStack> leftover = chestState.getBlockInventory().addItem(loot);

// 1) Log leftover
        plugin.getLogger().info("[Debug] leftover => " + leftover);

// 2) Log each non-null slot
        ItemStack[] contentsAfter = chestState.getBlockInventory().getContents();
        for (int i = 0; i < contentsAfter.length; i++) {
            if (contentsAfter[i] != null) {
                plugin.getLogger().info("[Debug] slot " + i + ": " + contentsAfter[i].getType()
                    + " x" + contentsAfter[i].getAmount());
            }
        }


        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Re-check if that block is still a chest
            if (loc.getBlock().getState() instanceof Chest c) {
                plugin.getLogger().info("[Debug] One second later, chest contents at " + loc + " => "
                    + Arrays.toString(c.getBlockInventory().getContents()));
            }
        }, 20L);
    }



    private void startParticleTask(int chestId, Location loc, int tier) {
        cancelParticleTask(chestId);
        // Repeat every 20 ticks
        org.bukkit.scheduler.BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(
            plugin,
            () -> {
                if (loc.getBlock().getType() == Material.CHEST) {
                    ParticleUtils.displayTierParticles(loc, tier);
                }
            },
            0L,
            20L
        );
        chestParticleTasks.put(chestId, task);
    }

    private void cancelParticleTask(int chestId) {
        if (chestParticleTasks.containsKey(chestId)) {
            chestParticleTasks.get(chestId).cancel();
            chestParticleTasks.remove(chestId);
        }
    }

    // Called when we remove the chest
    public void removeChest(int chestId) {
        Location loc = spawnedChests.get(chestId);
        if (loc != null) {
            Block block = loc.getBlock();
            if (block.getType() == Material.CHEST) {
                block.setType(Material.AIR);
            }
            spawnedChests.remove(chestId);
        }
        cancelParticleTask(chestId);
    }

    // Respawn after cooldown
    public void respawnChest(int chestId) {
        plugin.getLogger().info("[LootChestManager] respawnChest called for chest " + chestId);

        ChestData data = chestsById.get(chestId);
        if (data == null) {
            plugin.getLogger().info("[LootChestManager] No ChestData found for chest " + chestId
                + "; cannot respawn!");
            return;
        }
        // Actually call spawnChest
        spawnChest(data);
        plugin.getLogger().info("[LootChestManager] Finished respawnChest for chest " + chestId);
    }



    // For the /lootchest list command
    public Collection<ChestData> getAllChestData() {
        return chestsById.values();
    }

    // So we can add new data via a command
    public void addChestData(ChestData data) {
        chestsById.put(data.getChestId(), data);
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }


    // Check if a given location belongs to a spawned chest
    public Integer getChestIdAtLocation(Location location) {
        for (Map.Entry<Integer, Location> entry : spawnedChests.entrySet()) {
            if (entry.getValue().equals(location)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }


    private ItemStack getRandomLootForTier(int tier) {
        // Determine level range for each tier
        int minLevel, maxLevel;
        switch (tier) {
            case 1:
                minLevel = 1;
                maxLevel = 25;
                break;
            case 2:
                minLevel = 26;
                maxLevel = 45;
                break;
            case 3:
                minLevel = 46;
                maxLevel = 65;
                break;
            case 4:
                minLevel = 66;
                maxLevel = 100;
                break;
            default:
                plugin.getLogger().info("[LootChestManager] getRandomLootForTier => Invalid tier: " + tier);
                return null;
        }

        // Gather matching items from your ItemManager
        plugin.getLogger().info("[LootChestManager] getRandomLootForTier => tier=" + tier
            + ", searching for items with level_requirement between " + minLevel + " and " + maxLevel);

        List<CustomItem> matching = new ArrayList<>();
        for (CustomItem cItem : ItemManager.getInstance().getAllItems().values()) {
            int req = cItem.getLevelRequirement();
            if (req >= minLevel && req <= maxLevel) {
                matching.add(cItem);
            }
        }

        plugin.getLogger().info("[LootChestManager] Found " + matching.size()
            + " item(s) matching the range for tier " + tier);

        if (matching.isEmpty()) {
            return null;  // no items match => chest gets no item
        }

        // Pick one at random from the matching items
        CustomItem chosen = matching.get(new Random().nextInt(matching.size()));

        plugin.getLogger().info("[LootChestManager] Chose item ID " + chosen.getId()
            + " (" + chosen.getBaseName() + ") levelReq=" + chosen.getLevelRequirement());

        // Convert the chosen CustomItem -> ItemStack
        return ItemUtil.createItemStackFromCustomItem(chosen, 1);
    }

}
