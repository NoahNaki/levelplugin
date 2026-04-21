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
import me.nakilex.levelplugin.utils.FurnitureCleanupUtil;
import me.nakilex.levelplugin.utils.ModelEngineUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.TextUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class LootChestManager {

    private static final String DEFAULT_CRATE_ID = "coffre_1";
    private static final Set<String> LEGACY_CRATE_IDS = Set.of("crate_lvl1");
    private static final String MODEL_TAG = "loot_chest_model";
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
    private final Map<Integer, UUID> spawnedChestModels = new HashMap<>();
    private final Map<Integer, Boolean> chestIdleMoveState = new HashMap<>();
    private final Map<Integer, Long> chestIdleAnimationLockUntilMs = new HashMap<>();

    // For continuous particles: chestId -> repeating task
    private final Map<Integer, BukkitTask> chestParticleTasks = new HashMap<>();
    private BukkitTask crateModelRetryTask;

    // Quick lookup for which chests belong to a given chunk
    private final Map<ChunkKey, List<ChestData>> chunkChestIndex = new HashMap<>();

    // Track active loot sessions so we can pay out coins once per open
    private final Map<UUID, LootSession> openChestSessions = new HashMap<>();
    // Track quick open streaks to add a small bonus loop to chest runs.
    private final Map<UUID, LootStreakState> lootStreaks = new HashMap<>();
    // One-off chests (e.g. stronghold generated) should not respawn after opening.
    private final Set<Integer> nonRespawningChestIds = new HashSet<>();
    // Track per-player opened chest progress by world.
    private final Map<UUID, Map<String, Set<Integer>>> openedChestProgressByPlayer = new HashMap<>();

    private final Set<Material> upgradeMaterials = new HashSet<>();
    private boolean missingCrateModelLogged;
    private static final long LOOT_STREAK_WINDOW_MS = 3 * 60 * 1000L;
    private static final double LOOT_STREAK_BONUS_PER_STEP = 0.08;
    private static final double LOOT_STREAK_BONUS_CAP = 0.40;

    private String normalizeWorldName(String storedWorld) {
        if (storedWorld == null || storedWorld.isBlank() || storedWorld.equalsIgnoreCase("mmorpg")) {
            return "world";
        }
        return storedWorld;
    }

    private String resolveCrateModelId() {
        String available = findAvailableCrateModelId();
        return available != null ? available : DEFAULT_CRATE_ID;
    }

    private String findAvailableCrateModelId() {
        List<String> loaded = ModelEngineUtil.getModelIdsSafely(plugin);
        List<String> blueprint = ModelEngineUtil.getBlueprintModelIds(plugin);
        for (String token : crateModelCandidates()) {
            for (String candidate : ModelEngineUtil.buildModelCandidates(token)) {
                if (containsIgnoreCase(loaded, candidate) || containsIgnoreCase(blueprint, candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private List<String> crateModelCandidates() {
        List<String> candidates = new ArrayList<>();
        candidates.add(DEFAULT_CRATE_ID);
        candidates.addAll(LEGACY_CRATE_IDS);
        return candidates;
    }

    private boolean isLootChestModelId(String modelId) {
        if (modelId == null || modelId.isBlank()) {
            return false;
        }
        String normalized = normalizeModelId(modelId);
        if (normalizeModelId(DEFAULT_CRATE_ID).equals(normalized)) {
            return true;
        }
        for (String legacyId : LEGACY_CRATE_IDS) {
            if (normalizeModelId(legacyId).equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeModelId(String modelId) {
        String normalized = modelId.trim().toLowerCase(Locale.ROOT);
        if (normalized.endsWith(".bbmodel")) {
            return normalized.substring(0, normalized.length() - ".bbmodel".length());
        }
        return normalized;
    }

    private boolean containsIgnoreCase(Collection<String> values, String target) {
        if (values == null || target == null) {
            return false;
        }
        for (String value : values) {
            if (value != null && value.equalsIgnoreCase(target)) {
                return true;
            }
        }
        return false;
    }

    private void scheduleCrateModelRetry() {
        if (crateModelRetryTask != null && !crateModelRetryTask.isCancelled()) {
            return;
        }
        crateModelRetryTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (findAvailableCrateModelId() == null) {
                return;
            }
            plugin.getLogger().info("[LootChestManager] ModelEngine crate model is ready; respawning loot chest models.");
            for (ChestData data : chestDataList) {
                spawnChest(data);
            }
            crateModelRetryTask.cancel();
            crateModelRetryTask = null;
        }, 100L, 100L);
    }


    public LootChestManager(JavaPlugin plugin, ConfigManager configManager, CooldownManager cooldownManager, PotionManager potionManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.cooldownManager = cooldownManager;
        this.potionManager = potionManager;
        this.wandKey = new NamespacedKey(plugin, "lootchest_wand");

        removeExistingLootHolograms();
        loadChestDataFromConfig();
        refreshUpgradeMaterials();

        // Clean up any stray crate models from earlier runs before respawning them.
        purgeStaleFurniture();

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
                String world = normalizeWorldName(root.getString(key + ".world", "world"));

                ChestData data = new ChestData(chestId, world, x, y, z, face);
                chestDataList.add(data);
            } catch (Exception e) {
                plugin.getLogger().warning("Error loading chest ID: " + key);
            }
        }

        rebuildChunkIndex();
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
        ensureChunkIsLoaded(loc);

        // Keep the block clear so only the model is visible/interactable.
        if (loc.getBlock().getType() != Material.AIR) {
            loc.getBlock().setType(Material.AIR, false);
        }

        spawnChestModel(data.getChestId(), loc);

        // 2) Remember this location so getChestIdAtLocation(loc) will still work:
        spawnedChests.put(data.getChestId(), loc.getBlock().getLocation());

        // 3) Start the particle task (handles particle effects based on player proximity)
        startParticleTask(data.getChestId(), loc);
    }

    private void spawnChestModel(int chestId, Location chestLoc) {
        removeChestModel(chestId, chestLoc);
        if (!Bukkit.getPluginManager().isPluginEnabled("ModelEngine")) {
            return;
        }

        Location anchorLoc = LocationUtils.centerOnBlock(chestLoc).add(0.0, 0.01, 0.0);
        ArmorStand anchor = chestLoc.getWorld().spawn(anchorLoc, ArmorStand.class, stand -> {
            stand.setInvisible(true);
            stand.setMarker(false);
            stand.setSmall(true);
            stand.setGravity(false);
            stand.setSilent(true);
            stand.setInvulnerable(true);
            stand.addScoreboardTag(MODEL_TAG);
            stand.addScoreboardTag(MODEL_TAG + ":" + chestId);
        });

        ModelEngineUtil.ModelApplyResult result = ModelEngineUtil.applyFirstAvailableModel(
                anchor,
                ModelEngineUtil.buildModelCandidates(DEFAULT_CRATE_ID),
                plugin
        );
        if (result.applied().isEmpty()) {
            if (!missingCrateModelLogged) {
                plugin.getLogger().warning("[LootChestManager] Could not apply ModelEngine model for loot chest id "
                        + chestId + " using token '" + DEFAULT_CRATE_ID + "'.");
                missingCrateModelLogged = true;
            }
            scheduleCrateModelRetry();
            anchor.remove();
            return;
        }
        missingCrateModelLogged = false;
        spawnedChestModels.put(chestId, anchor.getUniqueId());
        chestIdleMoveState.put(chestId, false);
        ModelEngineUtil.playBestAnimation(anchor, List.of("idle"), true, false);
    }

    private void removeChestModel(int chestId, Location chestLoc) {
        UUID entityId = spawnedChestModels.remove(chestId);
        chestIdleMoveState.remove(chestId);
        chestIdleAnimationLockUntilMs.remove(chestId);
        if (entityId != null) {
            Entity entity = Bukkit.getEntity(entityId);
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
        }
        if (chestLoc == null || chestLoc.getWorld() == null || !chestLoc.getChunk().isLoaded()) {
            return;
        }
        for (Entity entity : chestLoc.getChunk().getEntities()) {
            if (!(entity instanceof ArmorStand stand)) {
                continue;
            }
            if (!stand.getScoreboardTags().contains(MODEL_TAG)
                    || !stand.getScoreboardTags().contains(MODEL_TAG + ":" + chestId)) {
                continue;
            }
            if (stand.getLocation().distanceSquared(LocationUtils.centerOnBlock(chestLoc)) <= 4.0) {
                stand.remove();
            }
        }
    }

    /**
     * Convenience for dynamic chests. Generates a new ID, adds the data list
     * and spawns the crate at the provided location.
     */
    public int createAndSpawnChest(Location loc) {
        return createAndSpawnChest(loc, BlockFace.NORTH);
    }

    public int createAndSpawnChest(Location loc, BlockFace facing) {
        return createAndSpawnChest(loc, facing, false);
    }

    public int createAndSpawnChest(Location loc, BlockFace facing, boolean nonRespawning) {
        int id = chestDataList.stream().mapToInt(ChestData::getChestId).max().orElse(0) + 1;
        ChestData data = new ChestData(id, normalizeWorldName(loc.getWorld().getName()), loc.getX(), loc.getY(), loc.getZ(), facing);
        chestDataList.add(data);
        if (nonRespawning) {
            nonRespawningChestIds.add(id);
        }
        spawnChest(data);
        return id;
    }

    public boolean isNonRespawningChest(int chestId) {
        return nonRespawningChestIds.contains(chestId);
    }

    public boolean isStrongholdWorld(World world) {
        if (world == null || world.getName() == null) {
            return false;
        }
        String worldName = world.getName().toLowerCase(Locale.ROOT);
        return worldName.startsWith("stronghold_debug_") || worldName.contains("stronghold");
    }

    public ChestProgress recordStrongholdChestOpen(UUID playerId, int chestId, World world) {
        if (playerId == null || world == null || !isStrongholdWorld(world)) {
            return null;
        }
        String worldKey = world.getName().toLowerCase(Locale.ROOT);
        Map<String, Set<Integer>> byWorld = openedChestProgressByPlayer.computeIfAbsent(playerId, ignored -> new HashMap<>());
        Set<Integer> openedIds = byWorld.computeIfAbsent(worldKey, ignored -> new HashSet<>());
        openedIds.add(chestId);
        int opened = openedIds.size();
        int total = Math.max(opened, countChestsInWorld(world));
        return new ChestProgress(opened, total);
    }

    private int countChestsInWorld(World world) {
        if (world == null) {
            return 0;
        }
        String worldName = world.getName();
        int count = 0;
        for (ChestData data : chestDataList) {
            if (data != null && worldName.equalsIgnoreCase(data.getWorldName())) {
                count++;
            }
        }
        return count;
    }


    public Location getLocationForChestId(int chestId) {
        return spawnedChests.get(chestId);
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
            return Bukkit.createInventory(null, INVENTORY_SIZE, "Loot Chest");
        }

        Inventory inv = Bukkit.createInventory(null, INVENTORY_SIZE, "Loot Chest");
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

        CoinStreakBonus coinBonus = applyCoinStreakBonus(player.getUniqueId(), loot.coinReward());
        LootSession session = new LootSession(
                chestId,
                inv,
                coinBonus.totalCoins(),
                gearScore,
                coinBonus.baseCoins(),
                coinBonus.bonusCoins(),
                coinBonus.streak()
        );
        openChestSessions.put(player.getUniqueId(), session);
        return inv;
    }

    private CoinStreakBonus applyCoinStreakBonus(UUID playerId, int baseCoins) {
        if (playerId == null || baseCoins <= 0) {
            return new CoinStreakBonus(Math.max(0, baseCoins), Math.max(0, baseCoins), 0);
        }
        long now = System.currentTimeMillis();
        LootStreakState state = lootStreaks.get(playerId);
        int streak = 1;
        if (state != null && now - state.lastOpenAt() <= LOOT_STREAK_WINDOW_MS) {
            streak = state.streak() + 1;
        }
        lootStreaks.put(playerId, new LootStreakState(streak, now));
        if (streak <= 1) {
            return new CoinStreakBonus(baseCoins, baseCoins, streak);
        }
        double bonusMultiplier = computeStreakBonusMultiplier(streak);
        int bonusCoins = (int) Math.round(baseCoins * bonusMultiplier);
        int totalCoins = baseCoins + bonusCoins;
        return new CoinStreakBonus(baseCoins, totalCoins, streak);
    }

    private double computeStreakBonusMultiplier(int streak) {
        if (streak <= 1) {
            return 0.0;
        }
        return Math.min(LOOT_STREAK_BONUS_CAP, (streak - 1) * LOOT_STREAK_BONUS_PER_STEP);
    }

    public int getCurrentLootStreak(UUID playerId) {
        if (playerId == null) {
            return 0;
        }
        LootStreakState state = lootStreaks.get(playerId);
        if (state == null) {
            return 0;
        }
        long elapsed = System.currentTimeMillis() - state.lastOpenAt();
        if (elapsed > LOOT_STREAK_WINDOW_MS) {
            lootStreaks.remove(playerId);
            return 0;
        }
        return Math.max(0, state.streak());
    }

    public int getNextStreakBonusPercent(UUID playerId) {
        int nextStreak = Math.max(1, getCurrentLootStreak(playerId) + 1);
        return (int) Math.round(computeStreakBonusMultiplier(nextStreak) * 100.0);
    }

    private void startParticleTask(int chestId, Location loc) {
        cancelParticleTask(chestId);

        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(
            plugin,
            () -> {
                boolean playerNearby = loc.getWorld().getPlayers().stream()
                        .anyMatch(p -> p.getLocation().distanceSquared(loc) <= 20 * 20);
                boolean playerVeryNear = loc.getWorld().getPlayers().stream()
                        .anyMatch(p -> p.getLocation().distanceSquared(loc) <= 7 * 7);

                if (playerNearby) {
                    ParticleUtils.displayChestParticles(loc);
                }
                updateIdleMoveAnimation(chestId, playerVeryNear);
            },
            0L,
            20L
        );
        chestParticleTasks.put(chestId, task);
    }

    public void playOpeningAnimation(int chestId, Inventory lootInventory) {
        lockIdleAnimation(chestId, 40L);
        playChestSound(chestId, Sound.BLOCK_CHEST_OPEN, 0.95f, 1.05f);
        if (containsRareLoot(lootInventory)) {
            playChestAnimation(chestId, List.of("opening_rare", "opening", "open"), false);
            return;
        }
        playChestAnimation(chestId, List.of("opening", "open"), false);
    }

    public void playClosingAnimation(int chestId) {
        lockIdleAnimation(chestId, 40L);
        playChestSound(chestId, Sound.BLOCK_CHEST_CLOSE, 0.95f, 1.0f);
        Location chestLoc = spawnedChests.get(chestId);
        if (chestLoc == null || chestLoc.getWorld() == null) {
            playChestAnimation(chestId, List.of("closing", "close"), false);
            return;
        }

        removeChestModel(chestId, chestLoc);
        if (!Bukkit.getPluginManager().isPluginEnabled("ModelEngine")) {
            return;
        }
        Location anchorLoc = LocationUtils.centerOnBlock(chestLoc).add(0.0, 0.01, 0.0);
        ArmorStand closingStand = chestLoc.getWorld().spawn(anchorLoc, ArmorStand.class, stand -> {
            stand.setInvisible(true);
            stand.setMarker(false);
            stand.setSmall(true);
            stand.setGravity(false);
            stand.setSilent(true);
            stand.setInvulnerable(true);
            stand.addScoreboardTag(MODEL_TAG);
            stand.addScoreboardTag(MODEL_TAG + ":" + chestId);
            stand.addScoreboardTag(MODEL_TAG + ":closing");
        });
        ModelEngineUtil.ModelApplyResult result = ModelEngineUtil.applyFirstAvailableModel(
                closingStand,
                ModelEngineUtil.buildModelCandidates(getCrateModelId()),
                plugin
        );
        if (result.applied().isEmpty()) {
            closingStand.remove();
            return;
        }
        ModelEngineUtil.playBestAnimationResolved(closingStand, List.of("closing", "close"), false, false);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (closingStand.isValid()) {
                closingStand.remove();
            }
        }, 40L);
    }

    private void updateIdleMoveAnimation(int chestId, boolean playerVeryNear) {
        if (isIdleAnimationLocked(chestId)) {
            return;
        }
        Boolean previous = chestIdleMoveState.get(chestId);
        if (previous != null && previous == playerVeryNear) {
            return;
        }
        chestIdleMoveState.put(chestId, playerVeryNear);
        if (playerVeryNear) {
            playChestAnimation(chestId, List.of("idle_mouve", "idle_move", "idle", "move"), true);
            return;
        }
        playChestAnimation(chestId, List.of("idle"), true);
    }

    private boolean playChestAnimation(int chestId, List<String> keywords, boolean loop) {
        UUID entityId = spawnedChestModels.get(chestId);
        if (entityId == null) {
            return false;
        }
        Entity modelEntity = Bukkit.getEntity(entityId);
        if (modelEntity == null || !modelEntity.isValid()) {
            return false;
        }
        String resolved = ModelEngineUtil.playBestAnimationResolved(modelEntity, keywords, loop, false);
        return resolved != null && !resolved.isBlank();
    }

    private void playChestSound(int chestId, Sound sound, float volume, float pitch) {
        if (sound == null) {
            return;
        }
        Location chestLoc = spawnedChests.get(chestId);
        if (chestLoc == null || chestLoc.getWorld() == null) {
            return;
        }
        chestLoc.getWorld().playSound(chestLoc, sound, volume, pitch);
    }

    private void lockIdleAnimation(int chestId, long ticks) {
        long durationMs = Math.max(0L, ticks) * 50L;
        chestIdleAnimationLockUntilMs.put(chestId, System.currentTimeMillis() + durationMs);
    }

    private boolean isIdleAnimationLocked(int chestId) {
        Long unlockAt = chestIdleAnimationLockUntilMs.get(chestId);
        if (unlockAt == null) {
            return false;
        }
        if (System.currentTimeMillis() >= unlockAt) {
            chestIdleAnimationLockUntilMs.remove(chestId);
            return false;
        }
        return true;
    }

    private boolean containsRareLoot(Inventory inventory) {
        if (inventory == null) {
            return false;
        }
        for (ItemStack item : inventory.getContents()) {
            ItemRarity rarity = ItemUtil.getItemRarity(item);
            if (rarity != null && rarity.ordinal() >= ItemRarity.RARE.ordinal()) {
                return true;
            }
        }
        return false;
    }

    private void removeExistingLootHolograms() {
        for (org.bukkit.World world : plugin.getServer().getWorlds()) {
            for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
                purgeTaggedHolograms(Arrays.asList(chunk.getEntities()));
            }
        }
    }

    private void removeTaggedHologramsAt(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return;
        }
        purgeTaggedHolograms(Arrays.asList(loc.getChunk().getEntities()));
    }

    private void purgeTaggedHolograms(Iterable<? extends org.bukkit.entity.Entity> entities) {
        for (org.bukkit.entity.Entity entity : entities) {
            if (entity instanceof org.bukkit.entity.ArmorStand stand
                    && stand.getScoreboardTags().contains("loot_hologram")) {
                stand.remove();
            }
        }
    }



    private void cancelParticleTask(int chestId) {
        if (chestParticleTasks.containsKey(chestId)) {
            chestParticleTasks.get(chestId).cancel();
            chestParticleTasks.remove(chestId);
        }
    }

    public boolean removeChest(int chestId) {
        // 1) Look up the stored Location for this chestId. If we never spawned it
        // this session, fall back to the config-backed ChestData.
        Location loc = spawnedChests.get(chestId);
        if (loc == null) {
            ChestData data = getChestData(chestId);
            loc = data != null ? data.toLocation() : null;
        }
        if (loc == null) {
            plugin.getLogger().warning("[LootChestManager] No spawned chest found for ID " + chestId);
            return false;
        }

        ensureChunkIsLoaded(loc);

        // 2) Attempt to remove the Nexo furniture at that location
        //    The remove(...) call will find the barrier entity/display entity combo and delete them.
        boolean removed = NexoFurniture.remove(loc);
        removeChestModel(chestId, loc);
        if (loc.getBlock().getType() == Material.CHEST || loc.getBlock().getType() == Material.TRAPPED_CHEST) {
            loc.getBlock().setType(Material.AIR, false);
            removed = true;
        }
        if (!removed) {
            plugin.getLogger().fine("[LootChestManager] Could not remove Nexo furniture at " + loc +
                " (ID " + chestId + "). Maybe it's already gone?");
        }

        // 3) Cancel its particle task, if one is running
        cancelParticleTask(chestId);

        // 4) Remove from our spawned‐map so that future lookups no longer think it exists
        spawnedChests.remove(chestId);

        // Clean up any lingering holograms from older versions
        removeTaggedHologramsAt(loc);

        plugin.getLogger().fine("[LootChestManager] Removed crate with ID " + chestId + " at " + loc);
        return true;
    }

    public void removeInactiveChestsInChunk(Chunk chunk) {
        if (chunk == null) {
            return;
        }

        ChunkKey key = chunkKeyFromChunk(chunk);
        if (key == null) {
            return;
        }

        List<ChestData> chestsInChunk = chunkChestIndex.get(key);
        if (chestsInChunk == null || chestsInChunk.isEmpty()) {
            return;
        }

        int removed = 0;
        int configuredInChunk = 0;
        int removedEntities = 0;

        for (ChestData data : chestsInChunk) {
            configuredInChunk++;

            if (spawnedChests.containsKey(data.getChestId())) {
                continue; // active this session
            }

            Location location = data.toLocation();
            if (location == null) {
                continue;
            }

            removeChestModel(data.getChestId(), location);
            boolean removedModel = false;
            if (location.getBlock().getType() == Material.CHEST || location.getBlock().getType() == Material.TRAPPED_CHEST) {
                location.getBlock().setType(Material.AIR, false);
                removedModel = true;
            }

            removedEntities += removeNearbyFurnitureEntities(chunk, location);

            if (removedModel) {
                removeTaggedHologramsAt(location);
                removed++;
                plugin.getLogger().info("[LootChestManager] Removed inactive crate model for chest "
                        + data.getChestId() + " in chunk " + chunk.getX() + "," + chunk.getZ());
            } else {
                plugin.getLogger().info("[LootChestManager] Chunk " + chunk.getX() + "," + chunk.getZ()
                        + " contains configured chest #" + data.getChestId()
                        + " but no crate entity was removed (none found at stored location).");
            }
        }

        removed += removeUntrackedCratesInChunk(chunk);

//        if (configuredInChunk > 0 || removed > 0 || removedEntities > 0) {
//            plugin.getLogger().info("[LootChestManager] Chunk " + chunk.getX() + "," + chunk.getZ()
//                    + " scan complete: configured=" + configuredInChunk + ", removed=" + removed
//                    + ", entitiesCleared=" + removedEntities + ".");
//        }
    }

    private int removeNearbyFurnitureEntities(Chunk chunk, Location target) {
        return FurnitureCleanupUtil.clearNearbyFurnitureEntities(plugin, target, 4.0,
                "[LootChestManager]");
    }

    public boolean deleteChest(int chestId) {
        ChestData data = getChestData(chestId);
        Location loc = data != null ? data.toLocation() : null;

        boolean removed = removeChest(chestId);

        chestDataList.removeIf(cd -> cd.getChestId() == chestId);
        removeChestFromIndex(chestId);
        spawnedChests.remove(chestId);
        nonRespawningChestIds.remove(chestId);
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
        addChestToIndex(data);
    }

    private ChestData getChestData(int chestId) {
        for (ChestData data : chestDataList) {
            if (data.getChestId() == chestId) {
                return data;
            }
        }
        return null;
    }

    public ChestData getChestDataById(int chestId) {
        return getChestData(chestId);
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

    public Integer getChestIdFromModelEntity(Entity entity) {
        if (entity == null) {
            return null;
        }
        String prefix = MODEL_TAG + ":";
        for (String tag : entity.getScoreboardTags()) {
            if (tag == null || !tag.startsWith(prefix)) {
                continue;
            }
            String suffix = tag.substring(prefix.length());
            try {
                return Integer.parseInt(suffix);
            } catch (NumberFormatException ignored) {
                return null;
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
        return resolveCrateModelId();
    }

    public boolean isLootChestMechanic(FurnitureMechanic mechanic) {
        return mechanic != null && isLootChestModelId(mechanic.getItemID());
    }

    public void removeAllChests() {
        if (crateModelRetryTask != null) {
            crateModelRetryTask.cancel();
            crateModelRetryTask = null;
        }
        Set<Integer> ids = new HashSet<>();
        for (ChestData data : chestDataList) {
            ids.add(data.getChestId());
        }
        ids.addAll(spawnedChests.keySet());
        for (int chestId : ids) {
            removeChest(chestId);
        }
        nonRespawningChestIds.clear();
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

    private void purgeStaleFurniture() {
        for (ChestData data : chestDataList) {
            Location loc = data.toLocation();
            if (loc == null) continue;
            ensureChunkIsLoaded(loc);
            NexoFurniture.remove(loc);
            removeChestModel(data.getChestId(), loc);
            if (loc.getBlock().getType() == Material.CHEST || loc.getBlock().getType() == Material.TRAPPED_CHEST) {
                loc.getBlock().setType(Material.AIR, false);
            }
        }
    }

    private void ensureChunkIsLoaded(Location loc) {
        if (loc.getWorld() == null) {
            return;
        }
        org.bukkit.Chunk chunk = loc.getChunk();
        if (!chunk.isLoaded()) {
            chunk.load();
        }
    }

    private boolean isChestInChunk(ChestData data, Chunk chunk) {
        Location loc = data.toLocation();
        if (loc == null || loc.getWorld() == null) {
            return false;
        }
        return loc.getWorld().equals(chunk.getWorld())
                && loc.getBlockX() >> 4 == chunk.getX()
                && loc.getBlockZ() >> 4 == chunk.getZ();
    }

    private void rebuildChunkIndex() {
        chunkChestIndex.clear();
        for (ChestData data : chestDataList) {
            addChestToIndex(data);
        }
    }

    private void addChestToIndex(ChestData data) {
        ChunkKey key = chunkKeyFromChest(data);
        if (key == null) {
            return;
        }
        chunkChestIndex.computeIfAbsent(key, k -> new ArrayList<>()).add(data);
    }

    private void removeChestFromIndex(int chestId) {
        chunkChestIndex.values().forEach(list -> list.removeIf(cd -> cd.getChestId() == chestId));
        chunkChestIndex.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    private ChunkKey chunkKeyFromChunk(Chunk chunk) {
        if (chunk == null || chunk.getWorld() == null) {
            return null;
        }
        String worldName = chunk.getWorld().getName();
        return new ChunkKey(worldName.toLowerCase(Locale.ROOT), chunk.getX(), chunk.getZ());
    }

    private ChunkKey chunkKeyFromChest(ChestData data) {
        if (data == null) {
            return null;
        }
        String worldName = normalizeWorldName(data.getWorldName());
        if (worldName == null || worldName.isBlank()) {
            return null;
        }

        int chunkX = ((int) Math.floor(data.getX())) >> 4;
        int chunkZ = ((int) Math.floor(data.getZ())) >> 4;
        return new ChunkKey(worldName.toLowerCase(Locale.ROOT), chunkX, chunkZ);
    }

    private int removeUntrackedCratesInChunk(Chunk chunk) {
        if (chunk == null || chunk.getWorld() == null) {
            return 0;
        }

        int removed = 0;
        Set<Location> activeChestBlocks = new HashSet<>();
        for (Location loc : spawnedChests.values()) {
            if (loc != null) {
                activeChestBlocks.add(loc.getBlock().getLocation());
            }
        }
        for (Entity entity : chunk.getEntities()) {
            if (!(entity instanceof ArmorStand stand)) {
                continue;
            }
            if (!stand.getScoreboardTags().contains(MODEL_TAG)) {
                continue;
            }
            Location blockLoc = stand.getLocation().getBlock().getLocation();
            if (activeChestBlocks.contains(blockLoc)) {
                continue;
            }
            stand.remove();
            removed++;
        }

        if (removed > 0) {
            plugin.getLogger().info("[LootChestManager] Purged " + removed + " untracked crate models in chunk "
                    + chunk.getX() + "," + chunk.getZ());
        }

        return removed;
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
        // Guaranteed structure to make each chest feel meaningful:
        // 1) always one gear piece
        // 2) always one progression utility drop (material or potion)
        // 3) one wildcard roll for variety
        List<LootType> plannedRolls = new ArrayList<>(LOOT_ROLLS);
        plannedRolls.add(LootType.GEAR);
        plannedRolls.add(random.nextBoolean() ? LootType.MATERIAL : LootType.POTION);
        while (plannedRolls.size() < LOOT_ROLLS) {
            plannedRolls.add(rollLootCategory());
        }

        for (LootType type : plannedRolls) {
            ItemStack rolled = rollLootByType(type, gearScore);
            if (rolled == null || rolled.getType().isAir()) {
                // Soft fallback keeps loot count healthy if a category cannot produce.
                rolled = rollLootByType(LootType.GEAR, gearScore);
            }
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

    private ItemStack rollLootByType(LootType type, int gearScore) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case GEAR -> rollGearLoot(gearScore);
            case POTION -> generatePotionForGearScore(gearScore);
            case GIFT -> rollGift();
            case MATERIAL -> rollUpgradeMaterial(gearScore);
        };
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
        return getRandomLootForCombatPower(combatPower, null, mobType, modelSet, true);
    }

    public ItemStack getRandomLootForCombatPower(double combatPower, Integer levelRequirement, String mobType, String modelSet) {
        return getRandomLootForCombatPower(combatPower, levelRequirement, mobType, modelSet, true);
    }

    public ItemStack getRandomLootForCombatPower(double combatPower, String mobType, String modelSet, boolean allowTemplates) {
        return getRandomLootForCombatPower(combatPower, null, mobType, modelSet, allowTemplates);
    }

    public ItemStack getRandomLootForCombatPower(double combatPower, Integer levelRequirement, String mobType, String modelSet, boolean allowTemplates) {
        CombatRewardCalculator.GearTarget target = CombatRewardCalculator.rollGearTarget((int) Math.round(combatPower));
        return generateLootForTarget(target, mobType, modelSet, levelRequirement, allowTemplates);
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

    private ItemStack generateLootForTarget(CombatRewardCalculator.GearTarget target, String mobType, String modelSet, Integer levelRequirement, boolean allowTemplates) {
        if (target == null) {
            return null;
        }

        if (!allowTemplates) {
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

    public record LootSession(int chestId,
                              Inventory inventory,
                              int coinReward,
                              int gearScore,
                              int baseCoinReward,
                              int bonusCoinReward,
                              int streak) {}

    public record ChestProgress(int opened, int total) {}

    private record LootResult(List<ItemStack> items, int coinReward) {}
    private record LootStreakState(int streak, long lastOpenAt) {}
    private record CoinStreakBonus(int baseCoins, int totalCoins, int streak) {
        int bonusCoins() {
            return Math.max(0, totalCoins - baseCoins);
        }
    }

    private record ChunkKey(String worldName, int chunkX, int chunkZ) {}
}
