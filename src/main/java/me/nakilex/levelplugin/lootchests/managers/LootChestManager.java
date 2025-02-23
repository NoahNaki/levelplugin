package me.nakilex.levelplugin.lootchests.managers;

import me.nakilex.levelplugin.lootchests.config.ConfigManager;
import me.nakilex.levelplugin.lootchests.data.ChestData;
import me.nakilex.levelplugin.lootchests.utils.ParticleUtils;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.potions.data.PotionInstance;
import me.nakilex.levelplugin.potions.data.PotionTemplate;
import me.nakilex.levelplugin.potions.managers.PotionManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

public class LootChestManager {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private CooldownManager cooldownManager;
    private final PotionManager potionManager; // New field

    // Each chest’s data (ID -> ChestData)
    private final List<ChestData> chestDataList = new ArrayList<>();

    // Track where we actually spawned each chest. chestId -> location
    private final java.util.Map<Integer, Location> spawnedChests = new java.util.HashMap<>();

    // For continuous particles: chestId -> repeating task
    private final java.util.Map<Integer, org.bukkit.scheduler.BukkitTask> chestParticleTasks = new java.util.HashMap<>();

    public LootChestManager(JavaPlugin plugin, ConfigManager configManager, CooldownManager cooldownManager, PotionManager potionManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.cooldownManager = cooldownManager;
        this.potionManager = potionManager; // assign it here

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
                chestDataList.add(data);
            } catch (Exception e) {
                plugin.getLogger().warning("Error loading chest ID: " + key);
            }
        }
    }

    // 2) Spawn all on startup
    private void spawnAllChestsOnStartup() {
        for (ChestData data : chestDataList) {
            spawnChest(data);
        }
    }

    public void spawnChest(ChestData data) {
        Location loc = data.toLocation();
        Block block = loc.getBlock();
        block.setType(Material.CHEST);
        spawnedChests.put(data.getChestId(), loc);
        org.bukkit.block.Chest chestState = (org.bukkit.block.Chest) block.getState();
        chestState.getBlockInventory().clear();

        ItemStack loot = getRandomLootForTier(data.getTier());

        // Add the item into a random slot within the chest's inventory
        Random random = new Random();
        int randomSlot = random.nextInt(chestState.getBlockInventory().getSize());

        // Force the inventory to update to reflect changes visually
        chestState.update(true);

        // Start particle effect
        startParticleTask(data.getChestId(), loc, data.getTier());
        chestState.getBlockInventory().setItem(randomSlot, loot);
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

        ChestData data = null;
        for (ChestData cd : chestDataList) {
            if (cd.getChestId() == chestId) {
                data = cd;
                break;
            }
        }
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
        return chestDataList;
    }

    // So we can add new data via a command
    public void addChestData(ChestData data) {
        chestDataList.add(data);
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    // Check if a given location belongs to a spawned chest
    public Integer getChestIdAtLocation(Location location) {
        for (java.util.Map.Entry<Integer, Location> entry : spawnedChests.entrySet()) {
            if (entry.getValue().equals(location)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public ItemStack getRandomLootForTier(int tier) {
        // Example: 20% chance to drop a potion
        double potionChance = 0.2;
        if (Math.random() < potionChance) {
            Collection<PotionTemplate> potionTemplates = potionManager.getAllTemplates();
            List<PotionTemplate> availablePotions = new ArrayList<>(potionTemplates);
            if (!availablePotions.isEmpty()) {
                // Randomly pick one potion template
                PotionTemplate selected = availablePotions.get(new Random().nextInt(availablePotions.size()));
                // Create a new potion instance and convert it to an ItemStack
                PotionInstance instance = potionManager.createInstance(selected);
                return instance.toItemStack(plugin);
            }
        }

        // If no potion is chosen, or if none exist, fallback to custom item logic
        int minLevel, maxLevel;
        switch (tier) {
            case 1:
                minLevel = 1;
                maxLevel = 12;
                break;
            case 2:
                minLevel = 13;
                maxLevel = 25;
                break;
            case 3:
                minLevel = 26;
                maxLevel = 38;
                break;
            case 4:
                minLevel = 39;
                maxLevel = 50;
                break;
            case 5:
                minLevel = 51;
                maxLevel = 62;
                break;
            case 6:
                minLevel = 63;
                maxLevel = 75;
                break;
            case 7:
                minLevel = 76;
                maxLevel = 88;
                break;
            case 8:
                minLevel = 89;
                maxLevel = 100;
                break;
            default:
                return null;
        }

        // Gather matching custom items
        List<CustomItem> matching = new ArrayList<>();
        for (CustomItem cItem : ItemManager.getInstance().getAllTemplates().values()) {
            int req = cItem.getLevelRequirement();
            if (req >= minLevel && req <= maxLevel) {
                matching.add(cItem);
            }
        }

        if (matching.isEmpty()) {
            return null; // No matching custom item: chest gets no loot
        }

        // Pick one custom item at random from the templates
        // Pick one custom item at random from the templates
        CustomItem chosen = matching.get(new Random().nextInt(matching.size()));

// Create a new unique instance based on the chosen template using the 12-parameter constructor
        CustomItem newInstance = new CustomItem(
            chosen.getId(),
            chosen.getBaseName(),
            chosen.getRarity(),
            chosen.getLevelRequirement(),
            chosen.getClassRequirement(),
            chosen.getMaterial(),
            chosen.getHp(),
            chosen.getDef(),
            chosen.getStr(),
            chosen.getAgi(),
            chosen.getIntel(),
            chosen.getDex()
        );
// Register the new instance so it's tracked properly
        ItemManager.getInstance().addInstance(newInstance);

        return ItemUtil.createItemStackFromCustomItem(newInstance, 1, null);

    }
}
