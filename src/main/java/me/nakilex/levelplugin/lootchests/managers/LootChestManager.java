package me.nakilex.levelplugin.lootchests.managers;

import com.nexomc.nexo.api.NexoFurniture;
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.lootchests.config.ConfigManager;
import me.nakilex.levelplugin.lootchests.data.ChestData;
import me.nakilex.levelplugin.lootchests.utils.ParticleUtils;
import me.nakilex.levelplugin.lootchests.utils.LocationUtils;
import me.nakilex.levelplugin.potions.data.PotionInstance;
import me.nakilex.levelplugin.potions.data.PotionTemplate;
import me.nakilex.levelplugin.potions.managers.PotionManager;
import me.nakilex.levelplugin.Main;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import me.nakilex.levelplugin.utils.NexoUtil;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

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

    // NEW: remember which chest each player opened
    private final java.util.Map<java.util.UUID, Integer> openChestByPlayer = new java.util.HashMap<>();


    public LootChestManager(JavaPlugin plugin, ConfigManager configManager, CooldownManager cooldownManager, PotionManager potionManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.cooldownManager = cooldownManager;
        this.potionManager = potionManager; // assign it here

        loadChestDataFromConfig();

        // Delay spawning chests until the server has fully started. This gives
        // the Nexo plugin time to finish registering furniture IDs.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            spawnAllChestsOnStartup();
        }, 20L); // ~1 second after startup
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
                BlockFace face = BlockFace.valueOf(root.getString(key + ".facing", "NORTH"));
                String world = root.getString(key + ".world", "MmoRPG");

                ChestData data = new ChestData(chestId, world, x, y, z, tier, face);
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
        if (loc == null) {
            plugin.getLogger().warning(
                "[LootChestManager] Cannot spawn chest; location is null for ID " + data.getChestId()
            );
            return;
        }

        // Remove any existing block at this location
        loc.getBlock().setType(Material.AIR, false);

        // 1) Place our tier specific crate furniture instead of a vanilla CHEST block.
        //    Use the recorded facing to orient the crate correctly.
        String crateId = getCrateIdForTier(data.getTier());
        FurnitureMechanic mech = NexoFurniture.furnitureMechanic(crateId);
        if (mech == null) {
            plugin.getLogger().severe(
                "[LootChestManager] Could not find FurnitureMechanic for ID '" + crateId + "'. Did your YAML register it?"
            );
            NexoUtil.logAvailableFurnitureIds(plugin.getLogger());
            return;
        }
        // Center the furniture within the block to avoid spawning offset issues.
        Location centered = LocationUtils.centerOnBlock(loc);
        // The place(...) call returns the spawned Entity; we ignore it here.
        NexoFurniture.place(crateId, centered, 0f, data.getFacing());

        // 2) Remember this location so getChestIdAtLocation(loc) will still work:
        spawnedChests.put(data.getChestId(), loc.getBlock().getLocation());

        // 3) Pre-buffer one random loot ItemStack (we’ll place it into the GUI when a player opens it)
        //    NOTE: ChestData must have a method setBufferedLootItem(ItemStack).
        //          Add that setter to ChestData if it's missing.
        ItemStack loot = getRandomLootForTier(data.getTier(), "default", null);
        data.setBufferedLootItem(loot);

        // 4) Start the particle task (handles hologram spawning based on player proximity)
        startParticleTask(data.getChestId(), loc, data.getTier(), data);
    }

    /**
     * Convenience for dynamic chests. Generates a new ID, adds the data list
     * and spawns the crate at the provided location.
     */
    public int createAndSpawnChest(Location loc, int tier) {
        return createAndSpawnChest(loc, tier, BlockFace.NORTH);
    }

    public int createAndSpawnChest(Location loc, int tier, BlockFace facing) {
        int id = chestDataList.stream().mapToInt(ChestData::getChestId).max().orElse(0) + 1;
        ChestData data = new ChestData(id, loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), tier, facing);
        chestDataList.add(data);
        spawnChest(data);
        return id;
    }


    public Location getLocationForChestId(int chestId) {
        return spawnedChests.get(chestId);
    }

    public void spawnHologramForChest(ChestData data) {
        Location base = data.toLocation();
        if (base == null) {
            plugin.getLogger().warning(
                "[LootChestManager] No location for chest " + data.getChestId()
            );
            return;
        }

        if (!data.getHolograms().isEmpty()) {
            return; // already spawned
        }

        boolean chunkLoaded = base.getChunk().isLoaded();

        // Check if there is STILL the correct crate furniture at that Location:
        String crateId = getCrateIdForTier(data.getTier());
        FurnitureMechanic mechAtLoc = NexoFurniture.furnitureMechanic(base.getBlock());
        boolean isCrate = (mechAtLoc != null && mechAtLoc.getItemID().equals(crateId));

        if (!chunkLoaded || !isCrate) {
            return;
        }

        // Positions for the three lines of text above the crate:
        Location line1Loc = base.clone().add(0.5, 1.2, 0.5);
        Location line2Loc = base.clone().add(0.5, 0.95, 0.5);
        Location line3Loc = base.clone().add(0.5, 0.70, 0.5);

        String namePrefix = data.getCustomName().orElse("Loot Chest");
        String tierText   = "Tier " + toRoman(data.getTier());
        String text1      = formatTierLine(namePrefix, tierText, data.getTier());
        String text2      = "§fRight-Click §7to open";
        String text3      = data.getContentType()
            .map(t -> "§7[Contains: " + t + "]")
            .orElse(null);

        spawnArmorStand(line1Loc, text1, data);
        spawnArmorStand(line2Loc, text2, data);
        if (text3 != null) {
            spawnArmorStand(line3Loc, text3, data);
        }
    }

    /**
     * Builds and returns a new Inventory for the player, where:
     * - The title is "Loot Chest <RomanTier>" (e.g. "Loot Chest IV")
     * - The single buffered loot item is placed in a random slot within the 27‐slot GUI.
     */
    public Inventory buildLootInventory(int chestId, Player player) {
        // 1) Find the ChestData for this ID
        ChestData data = null;
        for (ChestData cd : chestDataList) {
            if (cd.getChestId() == chestId) {
                data = cd;
                break;
            }
        }

        // 2) If somehow we didn’t find it (shouldn’t happen), fall back to an empty GUI with a generic title
        if (data == null) {
            return Bukkit.createInventory(null, 27, "Loot Chest");
        }

        // 3) Determine the tier and convert it to Roman numerals.
        int tier = data.getTier();
        String romanTier = toRoman(tier);

        // 4) Create a new 27‐slot inventory, titled "Loot Chest <RomanTier>"
        Inventory inv = Bukkit.createInventory(null, 27, "Loot Chest " + romanTier);

        // 5) Grab the buffered loot item from ChestData
        ItemStack loot = data.getBufferedLootItem();
        if (loot != null) {
            // 6) Choose a random slot between 0 (inclusive) and inv.getSize() (exclusive).
            int size = inv.getSize(); // 27
            int randomSlot = new Random().nextInt(size);

            // 7) Place the loot in that random slot
            inv.setItem(randomSlot, loot);
        }

        // 8) Return the newly built Inventory
        return inv;
    }



    private void spawnArmorStand(Location loc, String text, ChestData data) {
        org.bukkit.entity.ArmorStand stand = loc.getWorld().spawn(loc, org.bukkit.entity.ArmorStand.class);
        // Tag it so we can find and kill it later, even if its chunk was unloaded
        stand.addScoreboardTag("loot_hologram");

        stand.setVisible(false);
        stand.setMarker(true);
        stand.setGravity(false);
        stand.setCustomName(text);
        stand.setCustomNameVisible(true);
        stand.setSilent(true);
        stand.setSmall(true);

        data.getHolograms().add(stand); // Track it in-memory
    }

    public void killAllHologramArmorStands() {
        // Loop through every world
        for (org.bukkit.World world : plugin.getServer().getWorlds()) {
            // Only loaded chunks are visible; unloaded ones will get cleaned when they load if you also use the ChunkLoad listener
            for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
                for (org.bukkit.entity.Entity e : chunk.getEntities()) {
                    if (e instanceof org.bukkit.entity.ArmorStand stand
                        && stand.getScoreboardTags().contains("loot_hologram")) {
                        stand.remove();
                    }
                }
            }
        }
    }



    private String formatTierLine(String name, String tierText, int tier) {
        String fullPrefix = name; // just "Loot Chest" by default

        // Gradient from left to right (smooth), over Loot Chest
        ChatColor[] gradient = getSmoothGradientForTier(tier, fullPrefix.length());

        StringBuilder sb = new StringBuilder();

        // Apply gradient to Loot Chest
        for (int i = 0; i < fullPrefix.length(); i++) {
            sb.append(gradient[i]).append(fullPrefix.charAt(i));
        }

        // Append non-bold bracket + tier
        sb.append(ChatColor.RESET).append(" ").append(ChatColor.DARK_GRAY).append("[").append(ChatColor.GRAY)
            .append(tierText).append(ChatColor.DARK_GRAY).append("]");

        return sb.toString();
    }


    private ChatColor[] getSmoothGradientForTier(int tier, int length) {
        java.awt.Color start, end;

        switch (tier) {
            case 1: start = new java.awt.Color(255, 255, 255); end = new java.awt.Color(180, 180, 180); break;
            case 2: start = new java.awt.Color(0, 255, 0);     end = new java.awt.Color(0, 150, 0);     break;
            case 3: start = new java.awt.Color(0, 255, 255);   end = new java.awt.Color(0, 100, 255);   break;
            case 4: start = new java.awt.Color(255, 0, 255);   end = new java.awt.Color(100, 0, 150);   break;
            case 5: start = new java.awt.Color(255, 215, 0);   end = new java.awt.Color(255, 140, 0);   break;
            case 6: start = new java.awt.Color(255, 0, 0);     end = new java.awt.Color(150, 0, 0);     break;
            default: start = new java.awt.Color(200, 200, 200); end = new java.awt.Color(100, 100, 100); break;
        }

        ChatColor[] result = new ChatColor[length];
        for (int i = 0; i < length; i++) {
            float ratio = (float) i / (length - 1);
            int r = (int) (start.getRed() * (1 - ratio) + end.getRed() * ratio);
            int g = (int) (start.getGreen() * (1 - ratio) + end.getGreen() * ratio);
            int b = (int) (start.getBlue() * (1 - ratio) + end.getBlue() * ratio);
            result[i] = ChatColor.of(new java.awt.Color(r, g, b));
        }
        return result;
    }


    private String toRoman(int number) {
        String[] romanNumerals = {
            "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"
        };
        return (number >= 1 && number <= 10) ? romanNumerals[number - 1] : String.valueOf(number);
    }


    private void startParticleTask(int chestId, Location loc, int tier, ChestData data) {
        cancelParticleTask(chestId);

        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(
            plugin,
            () -> {
                String crateId = getCrateIdForTier(tier);
                FurnitureMechanic mechAtLoc = NexoFurniture.furnitureMechanic(loc.getBlock());
                boolean hasCrate = mechAtLoc != null && mechAtLoc.getItemID().equals(crateId);
                if (!hasCrate) {
                    removeHolograms(data);
                    return;
                }

                boolean playerNearby = loc.getWorld().getPlayers().stream()
                        .anyMatch(p -> p.getLocation().distanceSquared(loc) <= 20 * 20);

                if (playerNearby) {
                    if (data.getHolograms().isEmpty()) {
                        spawnHologramForChest(data);
                    }
                    ParticleUtils.displayTierParticles(loc, tier);
                } else {
                    removeHolograms(data);
                }
            },
            0L,
            20L
        );
        chestParticleTasks.put(chestId, task);
    }

    private void removeHolograms(ChestData data) {
        data.getHolograms().removeIf(stand -> {
            if (!stand.isDead()) stand.remove();
            return true;
        });
    }



    private void cancelParticleTask(int chestId) {
        if (chestParticleTasks.containsKey(chestId)) {
            chestParticleTasks.get(chestId).cancel();
            chestParticleTasks.remove(chestId);
        }
    }

    public boolean removeChest(int chestId) {
        // 1) Look up the stored Location for this chestId
        Location loc = spawnedChests.get(chestId);
        if (loc == null) {
            plugin.getLogger().warning("[LootChestManager] No spawned chest found for ID " + chestId);
            return false;
        }

        // 2) Attempt to remove the Nexo furniture at that location
        //    The remove(...) call will find the barrier entity/display entity combo and delete them.
        boolean removed = NexoFurniture.remove(loc);
        if (!removed) {
            plugin.getLogger().warning("[LootChestManager] Could not remove Nexo furniture at " + loc +
                " (ID " + chestId + "). Maybe it's already gone?");
        }

        // 3) Remove only this chest’s holograms (ArmorStands)
        for (ChestData data : chestDataList) {
            if (data.getChestId() == chestId) {
                for (ArmorStand stand : data.getHolograms()) {
                    if (!stand.isDead()) {
                        stand.remove();
                    }
                }
                data.getHolograms().clear();
                break;
            }
        }

        // 4) Cancel its particle task, if one is running
        cancelParticleTask(chestId);

        // 5) Remove from our spawned‐map so that future lookups no longer think it exists
        spawnedChests.remove(chestId);

        plugin.getLogger().info("[LootChestManager] Removed crate with ID " + chestId + " at " + loc);
        return true;
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
            Location stored = entry.getValue();
            if (stored.getWorld().equals(location.getWorld())
                    && stored.getBlockX() == location.getBlockX()
                    && stored.getBlockY() == location.getBlockY()
                    && stored.getBlockZ() == location.getBlockZ()) {
                return entry.getKey();
            }
        }
        return null;
    }

    public int getTierForChest(int chestId) {
        for (ChestData data : chestDataList) {
            if (data.getChestId() == chestId) {
                return data.getTier();
            }
        }
        return -1;
    }

    /**
     * Convenience method to map a chest tier to its furniture ID.
     */
    public String getCrateIdForTier(int tier) {
        return "crate_lvl" + tier;
    }

    public void removeAllChests() {
        List<Integer> ids = new ArrayList<>(spawnedChests.keySet());
        for (int chestId : ids) {
            removeChest(chestId);
        }
    }

    // NEW: call this when a player opens a chest GUI
    public void markPlayerViewingChest(java.util.UUID playerUUID, int chestId) {
        openChestByPlayer.put(playerUUID, chestId);
    }

    // NEW: call this when a player closes a chest GUI
    public Integer unmarkPlayerViewingChest(java.util.UUID playerUUID) {
        return openChestByPlayer.remove(playerUUID);
    }



    public boolean clearChest(int chestId) {
        Location loc = spawnedChests.get(chestId);
        if (loc == null) {
            plugin.getLogger().warning("[LootChestManager] No spawned chest found for ID " + chestId);
            return false;
        }

        Block block = loc.getBlock();
        if (!(block.getState() instanceof org.bukkit.block.Chest)) {
            plugin.getLogger().warning("[LootChestManager] Block at " + loc + " is not a chest!");
            return false;
        }

        org.bukkit.block.Chest chestState = (org.bukkit.block.Chest) block.getState();
        chestState.getBlockInventory().clear();
        chestState.update(true);
        plugin.getLogger().info("[LootChestManager] Cleared contents of chest " + chestId);
        return true;
    }

    public void clearAllChests() {
        // Make a copy of the IDs to avoid CME if spawn map is modified
        List<Integer> ids = new ArrayList<>(spawnedChests.keySet());
        for (int chestId : ids) {
            clearChest(chestId);
        }
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public ItemStack getRandomLootForTier(int tier, String mobType, String modelSet) {
        // Example: 20% chance to drop a potion
        double potionChance = 0.2;
        if (Math.random() < potionChance) {
            List<PotionTemplate> availablePotions;
            if (tier >= 1 && tier <= 3) {
                availablePotions = potionManager.getTemplatesForTier(1);
            } else if (tier >= 4 && tier <= 6) {
                availablePotions = potionManager.getTemplatesForTier(2);
            } else {
                availablePotions = potionManager.getTemplatesForTier(3);
            }
            if (!availablePotions.isEmpty()) {
                PotionTemplate selected = availablePotions.get(new Random().nextInt(availablePotions.size()));
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

        int level = ThreadLocalRandom.current().nextInt(minLevel, maxLevel + 1);

        // 30% chance to roll a procedural item instead of template
        if (Math.random() < 0.3) {
            CustomItem generated = ItemManager.getInstance().generateItem(mobType, level);
            String nexo = modelSet != null ? Main.getInstance().getModelSetManager().getModelId(modelSet, generated.getMaterial()) : null;
            return ItemUtil.createItemStackFromCustomItem(generated, 1, null, nexo);
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
        CustomItem chosen = matching.get(new Random().nextInt(matching.size()));

        CustomItem newInstance = new CustomItem(
            chosen.getId(),
            chosen.getBaseName(),
            chosen.getRarity(),
            chosen.getLevelRequirement(),
            chosen.getClassRequirement(),
            chosen.getMaterial(),
            chosen.getHpRange(),
            chosen.getDefRange(),
            chosen.getStrRange(),
            chosen.getAgiRange(),
            chosen.getIntelRange(),
            chosen.getDexRange(),
            chosen.getWilRange(),
            chosen.getTecRange()
        );
        ItemManager.getInstance().addInstance(newInstance);

        String nexo = modelSet != null ? Main.getInstance().getModelSetManager().getModelId(modelSet, newInstance.getMaterial()) : null;
        return ItemUtil.createItemStackFromCustomItem(newInstance, 1, null, nexo);

    }
}
