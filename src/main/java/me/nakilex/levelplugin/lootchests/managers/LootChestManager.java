package me.nakilex.levelplugin.lootchests.managers;

import com.nexomc.nexo.api.NexoFurniture;
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.environment.stage.BuildingStageManager;
import me.nakilex.levelplugin.items.data.ArmorType;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.items.generator.ProceduralItemGenerator;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.lootchests.config.ConfigManager;
import me.nakilex.levelplugin.lootchests.data.ChestData;
import me.nakilex.levelplugin.lootchests.utils.LocationUtils;
import me.nakilex.levelplugin.lootchests.utils.ParticleUtils;
import me.nakilex.levelplugin.mercenary.MercenaryAffinityManager;
import me.nakilex.levelplugin.mercenary.MercenaryGift;
import me.nakilex.levelplugin.mob.utils.CombatRewardCalculator;
import me.nakilex.levelplugin.potions.data.PotionInstance;
import me.nakilex.levelplugin.potions.data.PotionTemplate;
import me.nakilex.levelplugin.potions.managers.PotionManager;
import me.nakilex.levelplugin.salvage.managers.SalvageManager;
import me.nakilex.levelplugin.utils.NexoUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.TextUtil;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class LootChestManager {

    private static final String DEFAULT_CRATE_ID = "crate_lvl1";
    private static final int LOOT_ROLLS = 3;
    private static final int INVENTORY_SIZE = 27;

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private CooldownManager cooldownManager;
    private final PotionManager potionManager;
    private final NamespacedKey wandKey;
    private final Random random = new Random();

    // Each chest’s data (ID -> ChestData)
    private final List<ChestData> chestDataList = new ArrayList<>();

    // Track where we actually spawned each chest. chestId -> location
    private final Map<Integer, Location> spawnedChests = new HashMap<>();

    // For continuous particles: chestId -> repeating task
    private final Map<Integer, BukkitTask> chestParticleTasks = new HashMap<>();

    // Track active loot sessions so we can pay out coins once per open
    private final Map<UUID, LootSession> openChestSessions = new HashMap<>();

    private final Set<Material> upgradeMaterials = new HashSet<>();


    public LootChestManager(JavaPlugin plugin, ConfigManager configManager, CooldownManager cooldownManager, PotionManager potionManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.cooldownManager = cooldownManager;
        this.potionManager = potionManager;
        this.wandKey = new NamespacedKey(plugin, "lootchest_wand");

        loadChestDataFromConfig();
        refreshUpgradeMaterials();

        // Delay spawning chests until the server has fully started. This gives
        // the Nexo plugin time to finish registering furniture IDs.
        plugin.getServer().getScheduler().runTaskLater(plugin, this::spawnAllChests, 20L); // ~1 second after startup
    }

    public void setCooldownManager(CooldownManager manager) {
        this.cooldownManager = manager;
    }

    // 1) Load from lootchests.yml
    private void loadChestDataFromConfig() {
        chestDataList.clear();
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
                BlockFace face = BlockFace.valueOf(root.getString(key + ".facing", "NORTH"));
                String world = root.getString(key + ".world", "MmoRPG");

                ChestData data = new ChestData(chestId, world, x, y, z, face);
                chestDataList.add(data);
            } catch (Exception e) {
                plugin.getLogger().warning("Error loading chest ID: " + key);
            }
        }
    }

    // 2) Spawn all on startup
    private void spawnAllChests() {
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

        // 1) Place our standard crate furniture instead of a vanilla CHEST block.
        //    Use the recorded facing to orient the crate correctly.
        String crateId = DEFAULT_CRATE_ID;
        FurnitureMechanic mech = NexoFurniture.furnitureMechanic(crateId);
        if (mech == null) {
            plugin.getLogger().severe(
                "[LootChestManager] Could not find FurnitureMechanic for ID '" + crateId + "'. Did your YAML register it?"
            );
            NexoUtil.logAvailableFurnitureIds(plugin.getLogger());
            loc.getBlock().setType(Material.CHEST, false);
            org.bukkit.block.data.BlockData dataBlock = loc.getBlock().getBlockData();
            if (dataBlock instanceof org.bukkit.block.data.Directional directional) {
                directional.setFacing(data.getFacing());
                loc.getBlock().setBlockData(directional, false);
            }
        } else {
            // Center the furniture within the block to avoid spawning offset issues.
            Location centered = LocationUtils.centerOnBlock(loc);
            // The place(...) call returns the spawned Entity; we ignore it here.
            NexoFurniture.place(crateId, centered, 0f, data.getFacing());
        }

        // 2) Remember this location so getChestIdAtLocation(loc) will still work:
        spawnedChests.put(data.getChestId(), loc.getBlock().getLocation());

        // 3) Start the particle task (handles hologram spawning based on player proximity)
        startParticleTask(data.getChestId(), loc, data);
    }

    /**
     * Convenience for dynamic chests. Generates a new ID, adds the data list
     * and spawns the crate at the provided location.
     */
    public int createAndSpawnChest(Location loc) {
        return createAndSpawnChest(loc, BlockFace.NORTH);
    }

    public int createAndSpawnChest(Location loc, BlockFace facing) {
        int id = chestDataList.stream().mapToInt(ChestData::getChestId).max().orElse(0) + 1;
        ChestData data = new ChestData(id, loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), facing);
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
        FurnitureMechanic mechAtLoc = NexoFurniture.furnitureMechanic(base.getBlock());
        boolean isCrate = (mechAtLoc != null && mechAtLoc.getItemID().equals(DEFAULT_CRATE_ID))
                || base.getBlock().getType() == Material.CHEST;

        if (!chunkLoaded || !isCrate) {
            return;
        }

        // Positions for the three lines of text above the crate:
        Location line1Loc = base.clone().add(0.5, 1.2, 0.5);
        Location line2Loc = base.clone().add(0.5, 0.95, 0.5);
        Location line3Loc = base.clone().add(0.5, 0.70, 0.5);

        String namePrefix = data.getCustomName().orElse("Loot Chest");
        String text1      = ChatColor.GOLD + "" + ChatColor.BOLD + namePrefix;
        String text2      = "§fRight-Click §7to open";
        String text3      = ChatColor.GRAY + "[Scaled to your gear score]";

        spawnArmorStand(line1Loc, text1, data);
        spawnArmorStand(line2Loc, text2, data);
        spawnArmorStand(line3Loc, text3, data);
    }

    /**
     * Builds and returns a new Inventory for the player with loot that scales off of
     * their current gear score. Results are cached in {@link #openChestSessions} so we can
     * pay out coins once when the player closes the GUI.
     */
    public Inventory buildLootInventory(int chestId, Player player) {
        LootSession existing = openChestSessions.get(player.getUniqueId());
        if (existing != null && existing.chestId() == chestId) {
            return existing.inventory();
        }

        ChestData data = getChestData(chestId);
        if (data == null) {
            return Bukkit.createInventory(null, INVENTORY_SIZE, ChatColor.DARK_GREEN + "Loot Chest");
        }

        Inventory inv = Bukkit.createInventory(null, INVENTORY_SIZE, ChatColor.DARK_GREEN + "Loot Chest");
        int gearScore = Math.max(50, ItemUtil.calculateTotalGearScore(player));
        LootResult loot = rollLootForPlayer(player, gearScore);

        List<Integer> availableSlots = new ArrayList<>();
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            availableSlots.add(i);
        }

        for (ItemStack item : loot.items()) {
            if (availableSlots.isEmpty()) break;
            int slotIndex = random.nextInt(availableSlots.size());
            int slot = availableSlots.remove(slotIndex);
            inv.setItem(slot, item);
        }

        LootSession session = new LootSession(chestId, inv, loot.coinReward(), gearScore);
        openChestSessions.put(player.getUniqueId(), session);
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


    private void startParticleTask(int chestId, Location loc, ChestData data) {
        cancelParticleTask(chestId);

        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(
            plugin,
            () -> {
                FurnitureMechanic mechAtLoc = NexoFurniture.furnitureMechanic(loc.getBlock());
                boolean hasCrate = mechAtLoc != null && mechAtLoc.getItemID().equals(DEFAULT_CRATE_ID);
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
                    ParticleUtils.displayChestParticles(loc);
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

    public boolean deleteChest(int chestId) {
        ChestData data = getChestData(chestId);
        Location loc = data != null ? data.toLocation() : null;

        boolean removed = removeChest(chestId);

        chestDataList.removeIf(cd -> cd.getChestId() == chestId);
        spawnedChests.remove(chestId);
        openChestSessions.entrySet().removeIf(entry -> entry.getValue().chestId() == chestId);

        if (cooldownManager != null) {
            cooldownManager.clearCooldown(chestId);
        }
        configManager.removeLootChest(chestId);

        if (loc != null) {
            plugin.getLogger().info("[LootChestManager] Deleted loot chest #" + chestId + " at " + loc);
        }
        return removed;
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

    private ChestData getChestData(int chestId) {
        for (ChestData data : chestDataList) {
            if (data.getChestId() == chestId) {
                return data;
            }
        }
        return null;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    // Check if a given location belongs to a spawned chest
    public Integer getChestIdAtLocation(Location location) {
        for (Map.Entry<Integer, Location> entry : spawnedChests.entrySet()) {
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

    public Integer findNearestChestId(Location reference) {
        if (reference == null || reference.getWorld() == null) {
            return null;
        }

        double closest = Double.MAX_VALUE;
        Integer closestId = null;

        for (ChestData data : chestDataList) {
            Location chestLoc = data.toLocation();
            if (chestLoc == null || chestLoc.getWorld() == null) {
                continue;
            }
            if (!reference.getWorld().equals(chestLoc.getWorld())) {
                continue;
            }

            double distanceSq = chestLoc.distanceSquared(reference);
            if (distanceSq < closest) {
                closest = distanceSq;
                closestId = data.getChestId();
            }
        }

        return closestId;
    }

    public String getCrateModelId() {
        return DEFAULT_CRATE_ID;
    }

    public void removeAllChests() {
        List<Integer> ids = new ArrayList<>(spawnedChests.keySet());
        for (int chestId : ids) {
            removeChest(chestId);
        }
        openChestSessions.clear();
    }

    public synchronized void reloadFromConfig() {
        removeAllChests();
        chestParticleTasks.values().forEach(BukkitTask::cancel);
        chestParticleTasks.clear();
        spawnedChests.clear();
        openChestSessions.clear();
        upgradeMaterials.clear();
        loadChestDataFromConfig();
        refreshUpgradeMaterials();
        spawnAllChests();
    }

    public LootSession consumeSession(UUID playerUUID) {
        return openChestSessions.remove(playerUUID);
    }

    public LootSession peekSession(UUID playerUUID) {
        return openChestSessions.get(playerUUID);
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

    public ItemStack createWand() {
        ItemStack wand = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = wand.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Loot Chest Wand");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Right-click a block to register a loot chest");
            lore.add(ChatColor.GRAY + "Left-click a block to delete the nearest loot chest");
            lore.addAll(TooltipUtil.clickInstructions(null, "to save the location"));
            meta.getPersistentDataContainer().set(wandKey, PersistentDataType.INTEGER, 1);
            meta.setLore(lore);
            wand.setItemMeta(meta);
        }
        return wand;
    }

    public boolean isWand(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) {
            return false;
        }
        Integer marker = stack.getItemMeta().getPersistentDataContainer().get(wandKey, PersistentDataType.INTEGER);
        return marker != null && marker == 1;
    }

    public int registerChest(Location location, BlockFace facing) {
        int id = configManager.addLootChest(location, facing);
        ChestData data = new ChestData(id, location.getWorld().getName(), location.getX(), location.getY(), location.getZ(), facing);
        addChestData(data);
        spawnChest(data);
        return id;
    }

    private LootResult rollLootForPlayer(Player player, int gearScore) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < LOOT_ROLLS; i++) {
            LootType type = rollLootCategory();
            ItemStack rolled = switch (type) {
                case GEAR -> rollGearLoot(gearScore);
                case POTION -> generatePotionForGearScore(gearScore);
                case GIFT -> rollGift();
                case MATERIAL -> rollUpgradeMaterial(gearScore);
            };
            if (rolled != null && !rolled.getType().isAir()) {
                items.add(rolled);
            }
        }

        if (items.isEmpty()) {
            ItemStack fallback = rollGearLoot(gearScore);
            if (fallback != null) {
                items.add(fallback);
            }
        }

        int coinReward = ThreadLocalRandom.current().nextInt(1, gearScore + 1);
        return new LootResult(items, coinReward);
    }

    private LootType rollLootCategory() {
        int roll = random.nextInt(100);
        if (roll < 45) return LootType.GEAR;
        if (roll < 65) return LootType.MATERIAL;
        if (roll < 80) return LootType.POTION;
        return LootType.GIFT;
    }

    private ItemStack rollGearLoot(int gearScore) {
        return getRandomLootForCombatPower(gearScore, null, null);
    }

    private ItemStack rollGift() {
        MercenaryAffinityManager affinityManager = Main.getInstance().getMercenaryAffinityManager();
        if (affinityManager == null) {
            return null;
        }
        List<MercenaryGift> gifts = new ArrayList<>(affinityManager.getGifts());
        if (gifts.isEmpty()) {
            return null;
        }
        MercenaryGift gift = gifts.get(random.nextInt(gifts.size()));
        return gift.getIcon();
    }

    private ItemStack rollUpgradeMaterial(int gearScore) {
        if (upgradeMaterials.isEmpty()) {
            return null;
        }
        List<Material> materials = new ArrayList<>(upgradeMaterials);
        Material material = materials.get(random.nextInt(materials.size()));
        int amount = Math.max(1, Math.min(32, gearScore / 40 + random.nextInt(3) + 1));
        ItemStack stack = new ItemStack(material, amount);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + TextUtil.beautifyWords(material.name().toLowerCase().replace('_', ' ')));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Town upgrade material");
            lore.addAll(TooltipUtil.clickInstructions(null, "to collect"));
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack generatePotionForGearScore(int gearScore) {
        int tier;
        if (gearScore < 200) {
            tier = 1;
        } else if (gearScore < 400) {
            tier = 2;
        } else {
            tier = 3;
        }

        List<PotionTemplate> availablePotions = potionManager.getTemplatesForTier(tier);
        if (availablePotions.isEmpty()) {
            return null;
        }
        PotionTemplate selected = availablePotions.get(random.nextInt(availablePotions.size()));
        PotionInstance instance = potionManager.createInstance(selected);
        return instance.toItemStack(plugin);
    }

    private void refreshUpgradeMaterials() {
        BuildingStageManager stageManager = Main.getInstance().getBuildingStageManager();
        if (stageManager == null) {
            return;
        }
        for (String building : stageManager.getStageNames()) {
            int stage = 1;
            while (true) {
                BuildingStageManager.BuildingStage stageData = stageManager.getStage(building, stage);
                if (stageData == null) {
                    break;
                }
                upgradeMaterials.addAll(stageData.materialCost.keySet());
                stage++;
            }
        }
    }

    public ItemStack getRandomLootForCombatPower(double combatPower, String mobType, String modelSet) {
        return getRandomLootForCombatPower(combatPower, null, mobType, modelSet);
    }

    public ItemStack getRandomLootForCombatPower(double combatPower, Integer levelRequirement, String mobType, String modelSet) {
        CombatRewardCalculator.GearTarget target = CombatRewardCalculator.rollGearTarget((int) Math.round(combatPower));
        return generateLootForTarget(target, mobType, modelSet, levelRequirement);
    }

    public ItemStack getRandomLootForTier(int tier, String mobType, String modelSet) {
        int gearScore = Math.max(50, tier * 40);
        if (random.nextDouble() < 0.2) {
            ItemStack potion = generatePotionForGearScore(gearScore);
            if (potion != null) {
                return potion;
            }
        }
        return getRandomLootForCombatPower(gearScore, mobType, modelSet);
    }

    private ItemStack generateLootForTarget(CombatRewardCalculator.GearTarget target, String mobType, String modelSet, Integer levelRequirement) {
        if (target == null) {
            return null;
        }

        // 30% chance to roll a procedural item instead of template
        if (Math.random() < 0.3) {
            CustomItem generated = levelRequirement != null
                    ? ItemManager.getInstance().generateItemForGearScore(
                            mobType, target.targetGearScore(), target.rarity(), levelRequirement)
                    : ItemManager.getInstance().generateItemForGearScore(
                            mobType, target.targetGearScore(), target.rarity());
            String nexo = modelSet != null
                    ? Main.getInstance().getModelSetManager().getModelId(modelSet, generated.getMaterial())
                    : null;
            return ItemUtil.createItemStackFromCustomItem(generated, 1, null, nexo);
        }

        // Gather matching custom items
        List<CustomItem> matching = new ArrayList<>();
        for (CustomItem cItem : ItemManager.getInstance().getAllTemplates().values()) {
            if (cItem.getRarity().ordinal() > ItemRarity.RARE.ordinal()) {
                continue; // enforce rare and below
            }
            int score = SalvageManager.getInstance().getTotalStats(cItem);
            double diff = Math.abs(score - target.targetGearScore());
            double allowance = target.targetGearScore() * 0.25;
            if (diff <= allowance) {
                matching.add(cItem);
            }
        }

        CustomItem template;
        if (!matching.isEmpty()) {
            template = matching.get(random.nextInt(matching.size()));
        } else {
            template = levelRequirement != null
                    ? ItemManager.getInstance().generateItemForGearScore(
                            mobType, target.targetGearScore(), target.rarity(), levelRequirement)
                    : ItemManager.getInstance().generateItemForGearScore(
                            mobType, target.targetGearScore(), target.rarity());
        }

        Material material = template.getMaterial();
        ArmorType armorSlot = ArmorType.fromMaterial(material);
        if (levelRequirement != null && armorSlot != null) {
            material = ProceduralItemGenerator.resolveArmorMaterial(levelRequirement, armorSlot);
        }

        CustomItem newInstance = new CustomItem(
            template.getId(),
            template.getBaseName(),
            template.getRarity(),
            levelRequirement != null ? levelRequirement : template.getLevelRequirement(),
            template.getClassRequirement(),
            material,
            template.getHpRange(),
            template.getDefRange(),
            template.getStrRange(),
            template.getAgiRange(),
            template.getIntelRange(),
            template.getDexRange(),
            template.getWilRange(),
            template.getTecRange()
        );
        ItemManager.getInstance().addInstance(newInstance);

        String nexo = modelSet != null ? Main.getInstance().getModelSetManager().getModelId(modelSet, newInstance.getMaterial()) : null;
        return ItemUtil.createItemStackFromCustomItem(newInstance, 1, null, nexo);

    }

    private enum LootType {
        GEAR,
        POTION,
        GIFT,
        MATERIAL
    }

    public record LootSession(int chestId, Inventory inventory, int coinReward, int gearScore) {}

    private record LootResult(List<ItemStack> items, int coinReward) {}
}
