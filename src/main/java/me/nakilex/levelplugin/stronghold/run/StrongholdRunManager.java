package me.nakilex.levelplugin.stronghold.run;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.classes.managers.PlayerClassManager;
import me.nakilex.levelplugin.pet.PetEffectType;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.npc.system.NpcApi;
import me.nakilex.levelplugin.doublejump.listeners.DoubleJumpListener;
import me.nakilex.levelplugin.spells.SpellCastManager;
import me.nakilex.levelplugin.spells.SpellDefinition;
import me.nakilex.levelplugin.spells.SpellProgression;
import me.nakilex.levelplugin.spells.SpellRegistry;
import me.nakilex.levelplugin.spells.progression.SpellProgressionManager;
import me.nakilex.levelplugin.spells.SpellAccessUtil;
import me.nakilex.levelplugin.stronghold.StrongholdShrineManager;
import me.nakilex.levelplugin.stronghold.StrongholdStartupProfiler;
import me.nakilex.levelplugin.stronghold.utils.StrongholdMobSpawnUtil;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.StrongholdWorldUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.ChatFormatter;
import me.nakilex.levelplugin.utils.MultiLineHologram;
import me.nakilex.levelplugin.waypoints.api.pathing.result.Path;
import me.nakilex.levelplugin.waypoints.bukkit.BukkitPathfindingService;
import me.nakilex.levelplugin.waypoints.bukkit.PathLocationUtils;
import me.nakilex.levelplugin.waypoints.engine.result.PathUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.data.Openable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import org.bukkit.util.Vector;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import static me.nakilex.levelplugin.utils.ChatMessageUtil.send;
import me.nakilex.levelplugin.stronghold.run.RunSpellCastUtil.ManualCastTrigger;

public class StrongholdRunManager implements Listener {
    private static final String UPGRADE_GUI_TITLE = ChatColor.DARK_PURPLE + "Stronghold Upgrades";
    private static final int UPGRADE_GUI_ROWS = 5;
    private static final int UPGRADE_GUI_SIZE = UPGRADE_GUI_ROWS * 9;
    private static final int UPGRADE_REROLL_SLOT = 31;
    private static final String RESULTS_GUI_TITLE = ChatColor.DARK_PURPLE + "Stronghold Results";
    private static final String RESULTS_CONFIRM_GUI_TITLE = ChatColor.DARK_RED + "Exit Stronghold Results?";
    private static final String STRONGHOLD_KEY_NAME = "Stronghold Key";
    private static final int SHRINES_PER_RUN = 1;
    private static final int FIRST_WAVE_DELAY_SECONDS = 3;
    private static final int WAVE_INTERVAL_SECONDS = 5;
    private static final int WAVES_PER_STAGE = 30;
    private static final int MAX_ABSOLUTE_WAVE = 600;
    private static final int AUTOCAST_TICK_INTERVAL = 4;
    private static final int BASE_XP_REQUIRED = 160;
    private static final double XP_RANK_GROWTH = 1.55D;
    private static final double XP_STAGE_REQUIREMENT_GROWTH = 1.18D;
    public static final String STRONGHOLD_MAGE_METEOR_RADIUS_TAG = "lp_stronghold_mage_meteor_x3";
    private static final int MAX_ACTIVE_STRONGHOLD_SPELLS = 4;
    private static final double MIN_ENEMY_SPAWN_RADIUS = 5.0;
    private static final double MIN_EXIT_PORTAL_DISTANCE = 10.0;
    private static final long BASE_AUTOCAST_COOLDOWN_MS = 1_400L;
    private static final long STUCK_PULL_DELAY_MS = 4_000L;
    private static final long RUN_ENTRY_DEATH_GRACE_MS = 15_000L;
    private static final double STUCK_MOVE_EPSILON_SQ = 0.20 * 0.20;
    private static final double STUCK_PULL_DISTANCE = 6.0;
    private static final long MOB_RELOCATE_COOLDOWN_MS = 2_500L;
    private static final long MOBILITY_CHARGE_REFILL_MS = 4_000L;
    private static final double MOB_RELOCATE_TRIGGER_DISTANCE = 30.0;
    private static final double MOB_RELOCATE_MIN_RADIUS = 4.0;
    private static final double MOB_RELOCATE_MAX_RADIUS = 10.0;
    private static final double MOB_RELOCATE_AXIS_OFFSET = 5.0;
    private static final double MOB_RELOCATE_AXIS_JITTER = 1.5;
    private static final String MINIBOSS_MOB_ID = "slime_king";
    private static final String BOSS_MOB_ID = "giant_zombie";
    private static final Particle.DustOptions EXIT_PORTAL_GUIDE_DUST = new Particle.DustOptions(Color.fromRGB(120, 255, 120), 1.1f);
    private static final double EXIT_GUIDE_MIN_RENDER_DISTANCE = 1.75;
    private static final int PORTAL_SRC_MIN_X = -1986;
    private static final int PORTAL_SRC_MIN_Y = -60;
    private static final int PORTAL_SRC_MIN_Z = 3668;
    private static final int PORTAL_SRC_MAX_X = -1963;
    private static final int PORTAL_SRC_MAX_Y = -33;
    private static final int PORTAL_SRC_MAX_Z = 3680;
    private static final double KEY_DROP_CHANCE = 0.03;
    private static final long MANUAL_CAST_DEBOUNCE_MS = 80L;
    private static final double DEFAULT_STAGE_HEALTH_GROWTH = 0.35;
    private static final double DEFAULT_STAGE_DAMAGE_GROWTH = 0.14;
    private static final double DEFAULT_WAVE_HEALTH_GROWTH = 0.005;
    private static final double EXTRA_WAVE_HEALTH_SCALING = 0.02;
    private static final double DEFAULT_WAVE_DAMAGE_GROWTH = 0.005;
    private static final double DEFAULT_WAVE_MOVE_SPEED_GROWTH = 0.003;
    private static final int MINIBOSS_SLIME_SIZE = 2;
    private static final int BOSS_SLIME_SIZE = 3;
    private static final List<String> DEFAULT_MOBILITY_BASE_SPELLS = List.of(
            "mage_blink",
            "archer_skybound",
            "rogue_razor_dash",
            "warrior_titan_vault"
    );

    private final Main plugin;
    private final StrongholdShrineManager shrineManager;
    private final Map<UUID, ActiveRun> activeRuns = new HashMap<>();
    private final Map<UUID, StrongholdResultsStorageGUI> pendingResultInventories = new HashMap<>();
    private final Map<UUID, StrongholdResultsStorageGUI> openResultInventories = new HashMap<>();
    private final Set<UUID> confirmedResultExit = new HashSet<>();
    private final Map<UUID, Location> returnLocations = new HashMap<>();
    private final List<String> waveMobPool = List.of("forest_slime");
    private final Set<String> autoCastBasePool = new HashSet<>();
    private final Set<String> mobilityBasePool = new HashSet<>();
    private final Map<UUID, Integer> highestAbsoluteWaveByPlayer = new HashMap<>();
    private final Map<UUID, Integer> highestCompletedStageByPlayer = new HashMap<>();
    private final BukkitPathfindingService pathfindingService = new BukkitPathfindingService();
    private final Map<UUID, Integer> queuedStartingStageByPlayer = new HashMap<>();
    private final List<PortalTemplateBlock> strongholdExitPortalTemplate = new ArrayList<>();
    private final List<Location> activePortalRatingMarkers = new ArrayList<>();
    private final List<org.bukkit.entity.TextDisplay> activeStageResultDisplays = new ArrayList<>();
    private File progressionFile;
    private YamlConfiguration progressionConfig;
    private StageScalingConfig stageScalingConfig = new StageScalingConfig(DEFAULT_STAGE_HEALTH_GROWTH, DEFAULT_STAGE_DAMAGE_GROWTH, DEFAULT_WAVE_HEALTH_GROWTH, DEFAULT_WAVE_DAMAGE_GROWTH, DEFAULT_WAVE_MOVE_SPEED_GROWTH);

    public StrongholdRunManager(Main plugin, StrongholdShrineManager shrineManager) {
        this.plugin = plugin;
        this.shrineManager = shrineManager;
        loadProgressionData();
    }

    private void loadProgressionData() {
        progressionFile = new File(plugin.getDataFolder(), "stronghold_progression.yml");
        progressionConfig = YamlConfiguration.loadConfiguration(progressionFile);
        stageScalingConfig = new StageScalingConfig(
                Math.max(0.0, progressionConfig.getDouble("scaling.stage-health-growth", DEFAULT_STAGE_HEALTH_GROWTH)),
                Math.max(0.0, progressionConfig.getDouble("scaling.stage-damage-growth", DEFAULT_STAGE_DAMAGE_GROWTH)),
                Math.max(0.0, progressionConfig.getDouble("scaling.wave-health-growth", DEFAULT_WAVE_HEALTH_GROWTH)),
                Math.max(0.0, progressionConfig.getDouble("scaling.wave-damage-growth", DEFAULT_WAVE_DAMAGE_GROWTH)),
                Math.max(0.0, progressionConfig.getDouble("scaling.wave-speed-growth", DEFAULT_WAVE_MOVE_SPEED_GROWTH)));
        if (progressionConfig.isConfigurationSection("players")) {
            for (String key : progressionConfig.getConfigurationSection("players").getKeys(false)) {
                try { highestAbsoluteWaveByPlayer.put(UUID.fromString(key), Math.max(0, progressionConfig.getInt("players."+key+".highest-absolute-wave", 0))); } catch (Exception ignored) {}
                try { highestCompletedStageByPlayer.put(UUID.fromString(key), Math.max(0, progressionConfig.getInt("players."+key+".highest-completed-stage", 0))); } catch (Exception ignored) {}
            }
        }
    }

    private void saveProgressionData() {
        if (progressionConfig == null || progressionFile == null) return;
        progressionConfig.set("scaling.stage-health-growth", stageScalingConfig.stageHealthGrowth());
        progressionConfig.set("scaling.stage-damage-growth", stageScalingConfig.stageDamageGrowth());
        progressionConfig.set("scaling.wave-health-growth", stageScalingConfig.waveHealthGrowth());
        progressionConfig.set("scaling.wave-damage-growth", stageScalingConfig.waveDamageGrowth());
        progressionConfig.set("scaling.wave-speed-growth", stageScalingConfig.waveSpeedGrowth());
        for (var e: highestAbsoluteWaveByPlayer.entrySet()) progressionConfig.set("players."+e.getKey()+".highest-absolute-wave", e.getValue());
        for (var e: highestCompletedStageByPlayer.entrySet()) progressionConfig.set("players."+e.getKey()+".highest-completed-stage", e.getValue());
        try { progressionConfig.save(progressionFile); } catch (IOException ex) { ex.printStackTrace(); }
    }

    public StageScalingConfig getStageScalingConfig() { return stageScalingConfig; }
    public void updateStageScalingConfig(StageScalingConfig config) { if (config==null) return; this.stageScalingConfig=config; saveProgressionData(); }
    public StageProgress getHighestStageProgress(UUID playerId) { int w=Math.max(0, highestAbsoluteWaveByPlayer.getOrDefault(playerId,0)); return toStageProgress(w); }
    public int getHighestUnlockedStage(UUID playerId) {
        int highestWave = Math.max(0, highestAbsoluteWaveByPlayer.getOrDefault(playerId, 0));
        int attemptedStage = highestWave <= 0 ? 1 : toStageProgress(highestWave).stage();
        int completedStage = Math.max(0, highestCompletedStageByPlayer.getOrDefault(playerId, 0));
        return Math.min(maxSelectableStage(), Math.max(1, Math.max(attemptedStage, completedStage + 1)));
    }
    public void queueStartingStage(Player player, int stage) {
        if (player == null) return;
        queuedStartingStageByPlayer.put(player.getUniqueId(), clampStageSelection(stage));
    }
    public Integer consumeQueuedStartingStage(Player player) {
        return player == null ? null : queuedStartingStageByPlayer.remove(player.getUniqueId());
    }

    private StageProgress toStageProgress(int absoluteWave) { int safe=Math.max(1, absoluteWave); int stage=((safe-1)/WAVES_PER_STAGE)+1; int waveIn=((safe-1)%WAVES_PER_STAGE)+1; return new StageProgress(stage,waveIn,safe); }
    private int maxSelectableStage() {
        return ((MAX_ABSOLUTE_WAVE - 1) / WAVES_PER_STAGE) + 1;
    }
    private int clampStageSelection(int stage) {
        return Math.max(1, Math.min(maxSelectableStage(), stage));
    }

    public void startSoloRun(Player player) {
        startSoloRun(player, null, null);
    }

    public void startSoloRun(Player player, Integer startingStage) {
        startSoloRun(player, startingStage, null);
    }

    public void startSoloRun(Player player, Integer startingStage, StrongholdStartupProfiler profiler) {
        if (player == null || !player.isOnline()) {
            return;
        }
        World world = player.getWorld();
        if (!StrongholdWorldUtil.isStrongholdWorld(world)) {
            return;
        }
        UUID worldId = world.getUID();
        long stepStart = profiler == null ? 0L : profiler.stepStarted("Stop existing run state");
        stopRun(worldId);
        if (profiler != null) {
            profiler.stepFinished("Stop existing run state", stepStart);
        }

        Location origin = player.getLocation().clone();
        stepStart = profiler == null ? 0L : profiler.stepStarted("Spawn random shrines");
        StrongholdShrineManager.ShrineSpawnDiagnostics randomDiag =
                shrineManager.spawnRandomShrinesWithDiagnostics(origin, SHRINES_PER_RUN, 72, 250.0);
        if (profiler != null) {
            profiler.stepFinished("Spawn random shrines", stepStart);
        }
        int shrines = randomDiag.spawned();
        StrongholdShrineManager.ShrineSpawnDiagnostics fallbackDiag = null;
        if (shrines < SHRINES_PER_RUN) {
            stepStart = profiler == null ? 0L : profiler.stepStarted("Spawn fallback shrines");
            fallbackDiag = shrineManager.spawnFallbackShrinesWithDiagnostics(origin, SHRINES_PER_RUN - shrines, 128, 250.0);
            if (profiler != null) {
                profiler.stepFinished("Spawn fallback shrines", stepStart);
            }
            shrines += fallbackDiag.spawned();
        }
        if (shrines <= 0) {
            plugin.getLogger().fine("[Stronghold] No shrines spawned near origin "
                    + "[" + origin.getBlockX() + ", " + origin.getBlockY() + ", " + origin.getBlockZ() + "]"
                    + " (random candidates=" + randomDiag.candidateCount()
                    + ", fallback=" + (fallbackDiag == null ? 0 : fallbackDiag.candidateCount()) + ").");
        }

        stepStart = profiler == null ? 0L : profiler.stepStarted("Initialize ActiveRun and begin waves");
        ActiveRun run = new ActiveRun(worldId, origin, startingStage);
        activeRuns.put(worldId, run);
        run.start();
        if (profiler != null) {
            profiler.stepFinished("Initialize ActiveRun and begin waves", stepStart);
        }
        send(player, MessageType.SUCCESS, "Stronghold waves started.");
        if (plugin.getQuestManager() != null) {
            stepStart = profiler == null ? 0L : profiler.stepStarted("Quest hook: handleStrongholdEnter");
            plugin.getQuestManager().handleStrongholdEnter(player);
            if (profiler != null) {
                profiler.stepFinished("Quest hook: handleStrongholdEnter", stepStart);
            }
        }
        if (profiler != null) {
            profiler.summary();
        }
    }

    public void stopAll() {
        for (UUID worldId : new ArrayList<>(activeRuns.keySet())) {
            stopRun(worldId);
        }
    }

    public boolean addDebugXp(Player player, int amount) {
        if (player == null || amount <= 0) {
            return false;
        }
        ActiveRun run = activeRuns.get(player.getWorld().getUID());
        if (run == null) {
            return false;
        }
        return run.grantDebugXp(player, amount);
    }

    public boolean forceEndSession(Player target) {
        if (target == null || !target.isOnline()) {
            return false;
        }
        ActiveRun run = activeRuns.get(target.getWorld().getUID());
        if (run == null) {
            return false;
        }
        return run.forceEndAndShowRewards(target);
    }

    public boolean forceWaveSkip(Player target, int desiredWave) {
        if (target == null || !target.isOnline() || desiredWave <= 0) {
            return false;
        }
        ActiveRun run = activeRuns.get(target.getWorld().getUID());
        if (run == null) {
            return false;
        }
        return run.forceWaveSkip(desiredWave);
    }

    public void captureReturnLocation(Player player) {
        if (player == null) {
            return;
        }
        returnLocations.put(player.getUniqueId(), player.getLocation().clone());
    }

    public boolean tryConsumeStrongholdKey(Player player) {
        boolean consumed = consumeFirstMatchingItem(player, this::isStrongholdKey);
        if (consumed && player != null && plugin.getQuestManager() != null) {
            plugin.getQuestManager().handleStrongholdKeyUse(player);
        }
        return consumed;
    }

    public boolean storeLootToResultStorage(Player player, ItemStack stack) {
        if (player == null || stack == null || stack.getType().isAir()) {
            return false;
        }
        ActiveRun run = activeRuns.get(player.getWorld().getUID());
        if (run == null) {
            return false;
        }
        return run.captureLootToStash(player, stack);
    }

    public StageStatus getStageStatus(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            return null;
        }
        ActiveRun run = activeRuns.get(player.getWorld().getUID());
        if (run == null) {
            return null;
        }
        return run.getStageStatus(playerId);
    }

    public Location getExitPortalGuideTarget(Player player) {
        if (player == null || player.getWorld() == null) {
            return null;
        }
        ActiveRun run = activeRuns.get(player.getWorld().getUID());
        if (run == null || !run.completed || run.exitPortalBounds == null) {
            return null;
        }
        return run.exitPortalBounds.guideTarget(player.getWorld());
    }

    public List<String> getSpellPossibilities(Player player) {
        if (player == null || !player.isOnline()) {
            return List.of();
        }
        ActiveRun run = activeRuns.get(player.getWorld().getUID());
        if (run == null) {
            return List.of();
        }
        return run.getSpellPossibilities(player.getUniqueId());
    }

    private void stopRun(UUID worldId) {
        ActiveRun existing = activeRuns.remove(worldId);
        if (existing != null) {
            existing.stop();
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event == null || event.getEntity() == null) {
            return;
        }
        LivingEntity entity = event.getEntity();
        World world = entity.getWorld();
        if (world == null) {
            return;
        }
        ActiveRun run = activeRuns.get(world.getUID());
        if (run == null) {
            return;
        }
        run.onEntityDeath(entity);
    }

    @EventHandler(ignoreCancelled = true)
    public void onStrongholdExitPortalStep(PlayerMoveEvent event) {
        if (event == null || event.getPlayer() == null || event.getTo() == null) {
            return;
        }
        Player player = event.getPlayer();
        ActiveRun run = activeRuns.get(player.getWorld().getUID());
        if (run == null || !run.completed || !run.isInsideExitPortal(event.getTo())) {
            return;
        }
        run.forceEndAndShowRewards(player);
    }

    @EventHandler
    public void onUpgradeGuiClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getView() == null) {
            return;
        }
        String title = event.getView().getTitle();
        if (RESULTS_CONFIRM_GUI_TITLE.equals(title)) {
            event.setCancelled(true);
            handleResultsConfirmClick(player, event.getRawSlot());
            return;
        }
        if (isResultsGuiTitle(title)) {
            return;
        }
        if (!UPGRADE_GUI_TITLE.equals(title)) {
            return;
        }
        event.setCancelled(true);
        ActiveRun run = activeRuns.get(player.getWorld().getUID());
        if (run == null) {
            player.closeInventory();
            return;
        }
        run.handleUpgradeClick(player, event.getRawSlot());
    }

    @EventHandler(ignoreCancelled = true)
    public void onStrongholdDoorInteract(PlayerInteractEvent event) {
        if (event == null || event.getPlayer() == null || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        ActiveRun run = activeRuns.get(player.getWorld().getUID());
        Block clicked = event.getClickedBlock();
        if (!isLockedStrongholdDoor(clicked)) {
            return;
        }
        Openable openable = (Openable) clicked.getBlockData();
        if (openable.isOpen()) {
            return;
        }
        event.setCancelled(true);
        if (!tryConsumeStrongholdKey(player)) {
            showStrongholdDoorLockedMessage(player, clicked);
            return;
        }
        openable.setOpen(true);
        clicked.setBlockData(openable);
        showStrongholdDoorOpenedMessage(player, clicked);
        if (run != null) {
            run.recordDoorOpened(player.getUniqueId());
        }
        if (plugin.getQuestManager() != null) {
            plugin.getQuestManager().handleStrongholdKeyUse(player);
        }
    }

    private void showStrongholdDoorLockedMessage(Player player, Block doorBlock) {
        if (player == null) {
            return;
        }
        showTemporaryDoorHologram(doorBlock,
                ChatColor.RED + "🔒 " + ChatColor.WHITE + "Locked",
                ChatColor.GRAY + "Requires " + ChatColor.GOLD + "Stronghold Key");
        send(player, MessageType.WARNING, ChatColor.GOLD + "You need a Stronghold Key to open this gate.");
    }

    private void showStrongholdDoorOpenedMessage(Player player, Block doorBlock) {
        if (player == null) {
            return;
        }
        showTemporaryDoorHologram(doorBlock,
                ChatColor.GREEN + "🔓 " + ChatColor.WHITE + "Unlocked",
                ChatColor.DARK_GRAY + "Stronghold gate opened");
        send(player, MessageType.SUCCESS, ChatColor.GOLD + "Stronghold Key used. Gate opened.");
    }

    private void showTemporaryDoorHologram(Block doorBlock, String titleLine, String detailLine) {
        if (doorBlock == null || doorBlock.getWorld() == null) {
            return;
        }
        Location base = doorBlock.getLocation().add(0.5, 2.1, 0.5);
        String tag = "stronghold_door_hint_" + doorBlock.getX() + "_" + doorBlock.getY() + "_" + doorBlock.getZ();
        MultiLineHologram.removeAll(base, 1.2, tag);
        MultiLineHologram hologram = new MultiLineHologram(base, tag);
        hologram.spawn(java.util.List.of(titleLine, detailLine));
        Bukkit.getScheduler().runTaskLater(plugin, hologram::despawn, 40L);
    }

    @EventHandler(ignoreCancelled = true)
    public void onStrongholdChestInteract(PlayerInteractEvent event) {
        if (event == null || event.getPlayer() == null || event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof Container)) return;
        ActiveRun run = activeRuns.get(event.getPlayer().getWorld().getUID());
        if (run == null) return;
        run.recordChestOpened(event.getPlayer().getUniqueId(), block.getLocation());
    }

    @EventHandler(ignoreCancelled = true)
    public void onStrongholdRunDamageTaken(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ActiveRun run = activeRuns.get(player.getWorld().getUID());
        if (run == null) return;
        run.recordDamageTaken(player.getUniqueId(), event.getFinalDamage());
    }

    @EventHandler
    public void onStrongholdMobilityInteract(PlayerInteractEvent event) {
        if (event == null || event.getPlayer() == null) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND || !isRightClickAction(event.getAction())) {
            return;
        }
        if (isContainerOpenInteraction(event)) {
            return;
        }
        if (plugin.getDialogManager() != null && plugin.getDialogManager().isDialogLockActive(event.getPlayer())) {
            return;
        }
        if (!tryHandleStrongholdManualInput(event.getPlayer(), ManualCastTrigger.RIGHT_CLICK)) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onStrongholdMobilityEntityInteract(PlayerInteractEntityEvent event) {
        if (event == null || event.getPlayer() == null || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (NpcApi.getRegistry().isNPC(event.getRightClicked())
                || (plugin.getDialogManager() != null && plugin.getDialogManager().isDialogLockActive(event.getPlayer()))) {
            return;
        }
        if (!tryHandleStrongholdManualInput(event.getPlayer(), ManualCastTrigger.RIGHT_CLICK)) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onStrongholdRogueArcInteract(PlayerInteractEvent event) {
        if (event == null || event.getPlayer() == null) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND || !isLeftClickAction(event.getAction())) {
            return;
        }
        if (!tryHandleStrongholdManualInput(event.getPlayer(), ManualCastTrigger.LEFT_CLICK_BASIC)) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onStrongholdRogueArcBasicAttack(EntityDamageByEntityEvent event) {
        if (event == null || !(event.getDamager() instanceof Player player)) {
            return;
        }
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            return;
        }
        if (!tryHandleStrongholdManualInput(player, ManualCastTrigger.LEFT_CLICK_BASIC)) {
            return;
        }
        event.setCancelled(true);
    }

    private boolean isRightClickAction(Action action) {
        return action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
    }

    private boolean isLeftClickAction(Action action) {
        return action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;
    }

    private boolean isContainerOpenInteraction(PlayerInteractEvent event) {
        if (event == null || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return false;
        }
        Block clicked = event.getClickedBlock();
        if (clicked == null) {
            return false;
        }
        if (clicked.getState() instanceof Container) {
            return true;
        }
        Material type = clicked.getType();
        String typeName = type == null ? "" : type.name();
        return typeName.contains("CHEST") || typeName.contains("BARREL") || typeName.endsWith("SHULKER_BOX");
    }

    private boolean tryHandleStrongholdManualInput(Player player, ManualCastTrigger trigger) {
        if (player == null) {
            return false;
        }
        ActiveRun run = activeRuns.get(player.getWorld().getUID());
        if (run == null) {
            return false;
        }
        return run.tryManualCastSpell(player, trigger);
    }

    @EventHandler
    public void onUpgradeGuiClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (event.getView() != null && isResultsGuiTitle(event.getView().getTitle())) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) {
                    return;
                }
                if (player.getOpenInventory() != null && isResultsGuiTitle(player.getOpenInventory().getTitle())) {
                    return;
                }
                handleResultsGuiClose(player);
            });
            return;
        }
        if (event.getView() == null || !UPGRADE_GUI_TITLE.equals(event.getView().getTitle())) {
            return;
        }
        ActiveRun run = activeRuns.get(player.getWorld().getUID());
        if (run != null) {
            run.handleUpgradeClose(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (event == null || event.getPlayer() == null) {
            return;
        }
        Player player = event.getPlayer();
        ActiveRun run = activeRuns.get(player.getWorld().getUID());
        if (run != null) {
            run.hideProgressBar(player.getUniqueId());
        }
    }

    @EventHandler
    public void onRunPlayerDeath(PlayerDeathEvent event) {
        if (event == null || event.getEntity() == null) {
            return;
        }
        Player player = event.getEntity();
        ActiveRun run = activeRuns.get(player.getWorld().getUID());
        if (run != null) {
            run.handlePlayerDeath(player);
        }
    }

    @EventHandler
    public void onRunPlayerRespawn(PlayerRespawnEvent event) {
        if (event == null || event.getPlayer() == null) {
            return;
        }
        Player player = event.getPlayer();
        StrongholdResultsStorageGUI pending = pendingResultInventories.remove(player.getUniqueId());
        if (pending == null) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                openResultInventories.put(player.getUniqueId(), pending);
                pending.open(player);
            }
        }, 2L);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSelectionDamageImmunity(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ActiveRun run = activeRuns.get(player.getWorld().getUID());
        if (run == null || !run.isUpgradePaused(player.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSelectionRegenPause(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ActiveRun run = activeRuns.get(player.getWorld().getUID());
        if (run == null || !run.isUpgradePaused(player.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPausedPlayerTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getTarget() instanceof Player player)) {
            return;
        }
        ActiveRun run = activeRuns.get(player.getWorld().getUID());
        if (run == null || !run.isUpgradePaused(player.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        if (event.getEntity() instanceof Mob mob) {
            mob.setTarget(null);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onRunLootPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ActiveRun run = activeRuns.get(player.getWorld().getUID());
        if (run == null) {
            return;
        }
        if (run.captureLootToStash(player, event.getItem().getItemStack())) {
            event.setCancelled(true);
            event.getItem().remove();
        }
    }

    private void initializeAutoCastPool() {
        autoCastBasePool.clear();
        mobilityBasePool.clear();
        SpellProgressionManager progressionManager = SpellProgressionManager.getInstance();
        autoCastBasePool.addAll(progressionManager.getUpgradeableBaseSpells(false));
        mobilityBasePool.addAll(progressionManager.getUpgradeableBaseSpells(true));
        SpellRegistry registry = SpellRegistry.getInstance();
        for (String mobilityBase : DEFAULT_MOBILITY_BASE_SPELLS) {
            SpellRegistry.SpellEntry entry = registry.getSpell(mobilityBase);
            if (entry == null || entry.definition() == null || !entry.definition().movementSpell()) {
                continue;
            }
            mobilityBasePool.add(mobilityBase.toLowerCase(Locale.ROOT));
        }
        if (autoCastBasePool.isEmpty()) {
            autoCastBasePool.add("warrior_execution_arc");
            autoCastBasePool.add("warrior_earthquake");
            autoCastBasePool.add("warrior_rupture_cyclone");
            autoCastBasePool.add("warrior_guarded_resolve");
        }
    }

    private int xpRequiredForLevel(int level) {
        int safeLevel = Math.max(1, level);
        double required = BASE_XP_REQUIRED * Math.pow(XP_RANK_GROWTH, safeLevel - 1);
        return (int) Math.min(2_000_000_000D, Math.round(required));
    }

    private int xpRequiredForLevelAtStage(int level, int stage) {
        int safeStage = Math.max(1, stage);
        double baseRequired = xpRequiredForLevel(level);
        double stageMultiplier = Math.pow(XP_STAGE_REQUIREMENT_GROWTH, safeStage - 1);
        return (int) Math.min(2_000_000_000D, Math.round(baseRequired * stageMultiplier));
    }

    private final class ActiveRun {
        private final UUID worldId;
        private final Location origin;
        private final List<UUID> spawned = new ArrayList<>();
        private final List<UUID> currentWaveSpawned = new ArrayList<>();
        private final Map<UUID, MobMotionState> mobMotionStates = new HashMap<>();
        private final Map<UUID, SurvivorState> playerStates = new HashMap<>();
        private final Set<UUID> pausedPlayers = new HashSet<>();
        private final Map<UUID, Long> lastManualCastAttemptAt = new HashMap<>();
        private final Map<UUID, Location> resultSideReference = new HashMap<>();

        private BukkitTask task;
        private BukkitTask autoCastTask;
        private int wave = 0;
        private int lastSpawnedWave = 0;
        private final Integer selectedStartingStage;
        private int stageAnchor = 1;
        private int secondsUntilNextWave = FIRST_WAVE_DELAY_SECONDS;
        private boolean completed = false;
        private PlacedPortalBounds exitPortalBounds;
        private UUID waveBossId;
        private boolean portalPlacementPendingNotified = false;
        private long nextPortalGuideAt = 0L;
        private long startedAtMs = 0L;
        private long stageStartedAtMs = 0L;
        private final java.util.Set<String> openedChestLocations = new java.util.HashSet<>();

        private ActiveRun(UUID worldId, Location origin, Integer selectedStartingStage) {
            this.worldId = worldId;
            this.origin = origin;
            this.selectedStartingStage = selectedStartingStage;
        }

        private void start() {
            startedAtMs = System.currentTimeMillis();
            stageStartedAtMs = startedAtMs;
            World runWorld = plugin.getServer().getWorld(worldId);
            if (runWorld != null) {
                initializePlayers(runWorld);
                int checkpoint = playersInWorld(runWorld).stream()
                        .mapToInt(p -> ((Math.max(1, getHighestUnlockedStage(p.getUniqueId())) - 1) * WAVES_PER_STAGE) + 1)
                        .max().orElse(1);
                if (selectedStartingStage != null && selectedStartingStage > 0) {
                    checkpoint = ((clampStageSelection(selectedStartingStage) - 1) * WAVES_PER_STAGE) + 1;
                }
                stageAnchor = Math.max(1, toStageProgress(checkpoint).stage());
                wave = Math.max(0, checkpoint - 1);
            }
            this.task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
                World world = plugin.getServer().getWorld(worldId);
                if (world == null || !StrongholdWorldUtil.isStrongholdWorld(world)) {
                    stopRun(worldId);
                    return;
                }
                syncRunPlayers(world);
                cleanupDeadSpawned();
                updateStuckMobPull(world);
                if (countAliveCurrentWave() > 0) {
                    return;
                }
                StageProgress liveProgress = toStageProgress(Math.max(1, wave));
                if (!completed && wave == lastSpawnedWave && liveProgress.wave() == WAVES_PER_STAGE) {
                    concludeRunAndSpawnExitPortal();
                    return;
                }
                if (completed) {
                    if (exitPortalBounds == null) {
                        concludeRunAndSpawnExitPortal();
                        return;
                    }
                    tickExitPortalGuidance(world);
                    return;
                }
                if (wave >= MAX_ABSOLUTE_WAVE) {
                    endRunAndShowRewardsForAllPlayers(ChatColor.GREEN + "Stage cap reached. Stronghold run complete.");
                    return;
                }
                if (secondsUntilNextWave > 0) {
                    secondsUntilNextWave--;
                    return;
                }
                secondsUntilNextWave = WAVE_INTERVAL_SECONDS;
                int waveStep = computeWaveAdvance(playersInWorld(world));
                int previousWave = wave;
                wave = Math.min(MAX_ABSOLUTE_WAVE, wave + waveStep);
                boolean spawned = spawnWave(world, wave);
                if (!spawned) {
                    wave = previousWave;
                    secondsUntilNextWave = 2;
                    return;
                }
                lastSpawnedWave = wave;
            }, 20L, 20L);
            this.autoCastTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickAutoCast, 20L, AUTOCAST_TICK_INTERVAL);
        }

        private List<Player> playersInWorld(World world) {
            if (world == null) {
                return List.of();
            }
            return world.getPlayers().stream().filter(Player::isOnline).toList();
        }

        private int computeWaveAdvance(List<Player> players) {
            int bonus = 0;
            if (plugin.getPetManager() != null && players != null) {
                for (Player player : players) {
                    if (player == null) {
                        continue;
                    }
                    double effectValue = plugin.getPetManager()
                            .getActiveEffectValue(player.getUniqueId(), PetEffectType.STRONGHOLD_WAVE_RIDER);
                    bonus = Math.max(bonus, Math.max(0, (int) Math.floor(effectValue)));
                }
            }
            return Math.max(1, 1 + bonus);
        }

        private void stop() {
            if (task != null) {
                task.cancel();
                task = null;
            }
            if (autoCastTask != null) {
                autoCastTask.cancel();
                autoCastTask = null;
            }
            for (UUID id : spawned) {
                var e = plugin.getServer().getEntity(id);
                if (e instanceof LivingEntity living && !living.isDead()) {
                    living.remove();
                }
            }
            spawned.clear();
            currentWaveSpawned.clear();
            mobMotionStates.clear();
            lastManualCastAttemptAt.clear();
            for (Map.Entry<UUID, SurvivorState> entry : new HashMap<>(playerStates).entrySet()) {
                restorePlayerAfterRun(entry.getKey(), entry.getValue());
            }
            playerStates.clear();
        }

        private void onEntityDeath(LivingEntity entity) {
            if (entity == null) {
                return;
            }
            UUID deadId = entity.getUniqueId();
            spawned.remove(deadId);
            currentWaveSpawned.remove(deadId);
            mobMotionStates.remove(deadId);

            Player killer = entity.getKiller();
            if (killer != null && killer.isOnline()) {
                handleMobKillXp(killer, entity);
                maybeDropStrongholdKey(entity.getLocation(), killer);
            }
            if (waveBossId != null && waveBossId.equals(deadId)) {
                waveBossId = null;
            }
        }

        private boolean forceWaveSkip(int desiredWave) {
            if (completed) {
                return false;
            }
            int clampedWaveInStage = Math.max(1, Math.min(WAVES_PER_STAGE, desiredWave));
            int activeStage = Math.max(stageAnchor, toStageProgress(Math.max(1, wave)).stage());
            int absoluteWave = ((activeStage - 1) * WAVES_PER_STAGE) + clampedWaveInStage;
            wave = Math.min(MAX_ABSOLUTE_WAVE, Math.max(0, absoluteWave - 1));
            secondsUntilNextWave = 1;
            currentWaveSpawned.clear();
            for (Player player : playersInWorld(plugin.getServer().getWorld(worldId))) {
                send(player, MessageType.INFO, "Debug waveskip set to " + ChatColor.WHITE + activeStage + "-" + clampedWaveInStage + ChatColor.GRAY + ".");
            }
            return true;
        }

        private void recordDamageTaken(UUID playerId, double amount) {
            if (amount <= 0) return;
            SurvivorState state = playerStates.get(playerId);
            if (state != null) state.damageTaken += amount;
        }

        private void recordDoorOpened(UUID playerId) {
            SurvivorState state = playerStates.get(playerId);
            if (state != null) state.doorsOpened++;
        }

        private void recordChestOpened(UUID playerId, Location loc) {
            if (loc == null) return;
            String key = loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ() + ":" + playerId;
            if (!openedChestLocations.add(key)) return;
            SurvivorState state = playerStates.get(playerId);
            if (state != null) state.chestsOpened++;
        }

        private ScoreResult calculateStageRating(SurvivorState state, long elapsedMs) {
            if (state == null) return new ScoreResult(0,0,0,0,"F");
            double secs = Math.max(1.0, elapsedMs / 1000.0);
            int objectiveScore = (int) Math.max(0, Math.min(40, Math.round(Math.min(20.0, state.doorsOpened * 2.5) + Math.min(20.0, state.chestsOpened * 5.0))));
            int damageScore = (int) Math.max(0, Math.min(30, Math.round(Math.max(0, 30.0 - (state.damageTaken / 40.0)))));
            int timeScore = (int) Math.max(0, Math.min(30, Math.round(Math.max(0, 30.0 - (secs / 10.0)))));
            int total = objectiveScore + damageScore + timeScore;
            String rank = total >= 85 ? "S" : total >= 72 ? "A" : total >= 60 ? "B" : total >= 48 ? "C" : total >= 36 ? "D" : total >= 24 ? "E" : "F";
            return new ScoreResult(total, objectiveScore, damageScore, timeScore, rank);
        }

        private java.util.List<Location> resolveFixedResultsLocations(Player player) {
            java.util.List<Location> out = new java.util.ArrayList<>();
            if (!activePortalRatingMarkers.isEmpty()) {
                out.addAll(activePortalRatingMarkers);
                return out;
            }
            World world = player == null ? null : player.getWorld();
            if (world != null && exitPortalBounds != null) {
                out.add(exitPortalBounds.guideTarget(world));
            }
            return out;
        }

        private String formatResultMetricLine(String label, String value) {
            String safeLabel = label == null ? "" : label.trim();
            String safeValue = value == null ? "" : value.trim();
            String left = ChatColor.GRAY + safeLabel;
            String right = ChatColor.WHITE + safeValue;
            int targetPx = 145;
            int used = ChatFormatter.pixelLength(safeLabel) + ChatFormatter.pixelLength(safeValue);
            int gapPx = Math.max(4, targetPx - used);
            int spaces = Math.max(1, gapPx / Math.max(1, ChatFormatter.pixelLength(" ")));
            return left + ChatColor.DARK_GRAY + " " + " ".repeat(spaces) + right;
        }

        private void showStageRating(Player player, ScoreResult result, long elapsedMs, SurvivorState state) {
            if (player == null || result == null || state == null) return;
            Location ref = resultSideReference.get(player.getUniqueId());
            logResultPlacementDebug("Attempting stage result render for " + player.getName() + " rating=" + result.rank()
                    + " ref=" + (ref==null?"none":(ref.getBlockX()+","+ref.getBlockY()+","+ref.getBlockZ())));
            String title = ChatColor.GOLD + "SCORE " + ChatColor.WHITE + result.total();

            java.util.List<Location> fixedLocations = resolveFixedResultsLocations(player);
            if (fixedLocations.isEmpty()) {
                logResultPlacementDebug("No fixed results location resolved for " + player.getName());
                return;
            }
                String tag = "stronghold_rating_marker_" + player.getUniqueId();
            java.util.List<String> lines = java.util.List.of(
                    ChatColor.LIGHT_PURPLE + "Stronghold Results",
                    title,
                    formatResultMetricLine("Objectives", String.valueOf(result.objectives())),
                    formatResultMetricLine("Damage Taken", String.valueOf(result.damage())),
                    formatResultMetricLine("Time Cleared", (elapsedMs / 1000) + "s (" + result.time() + ")"),
                    formatResultMetricLine("Rank", result.rank())
            );
            for (Location fixed : fixedLocations) {
                if (fixed == null) continue;
                logResultPlacementDebug("Rendering results at " + fixed.getBlockX()+","+fixed.getBlockY()+","+fixed.getBlockZ());
                spawnFixedResultScreen(fixed, lines, tag);
            }
        }

        private void concludeRunAndSpawnExitPortal() {
            completed = true;
            World world = plugin.getServer().getWorld(worldId);
            if (world == null) {
                return;
            }
            int clearedStage = toStageProgress(Math.max(1, wave)).stage();
            for (Player player : playersInWorld(world)) {
                highestCompletedStageByPlayer.merge(player.getUniqueId(), Math.max(1, clearedStage), Math::max);
                SurvivorState state = playerStates.get(player.getUniqueId());
                resultSideReference.put(player.getUniqueId(), player.getLocation().clone());
                if (state != null) {
                    long elapsed = Math.max(1000L, System.currentTimeMillis() - stageStartedAtMs);
                    ScoreResult result = calculateStageRating(state, elapsed);
                    state.lastStageRating = result.rank();
                }
                int nextStageWave = Math.min(MAX_ABSOLUTE_WAVE, (clearedStage * WAVES_PER_STAGE) + 1);
                highestAbsoluteWaveByPlayer.merge(player.getUniqueId(), nextStageWave, Math::max);
            }
            stageStartedAtMs = System.currentTimeMillis();
            saveProgressionData();
            if (strongholdExitPortalTemplate.isEmpty()) {
                loadPortalTemplateIfNeeded();
            }
            for (Player player : playersInWorld(world)) {
                exitPortalBounds = tryPlaceExitPortalNearPlayer(player);
                if (exitPortalBounds != null) {
                    portalPlacementPendingNotified = false;
                    logResultPlacementDebug("Exit portal placed. Markers=" + activePortalRatingMarkers.size());
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        logResultPlacementDebug("Delayed result render pass. Markers=" + activePortalRatingMarkers.size());
                        for (Player online : playersInWorld(world)) {
                            SurvivorState state = playerStates.get(online.getUniqueId());
                            if (state != null && state.lastStageRating != null) {
                                long elapsed = Math.max(1000L, System.currentTimeMillis() - stageStartedAtMs);
                                showStageRating(online, calculateStageRating(state, elapsed), elapsed, state);
                            }
                        }
                    }, 30L);
                    ChatFormatter.constructDivider(player, "§a§l-", 45);
                    ChatFormatter.sendCenteredMessage(player, "§a§lSTRONGHOLD STAGE CLEARED");
                    ChatFormatter.sendCenteredMessage(player, "");
                    ChatFormatter.sendCenteredMessage(player,
                            ChatColor.GRAY + "You conquered Stage " + ChatColor.GREEN + clearedStage + ChatColor.GRAY + "!");
                    ChatFormatter.constructDivider(player, "§a§l-", 45);
                    return;
                }
            }
            if (!portalPlacementPendingNotified) {
                portalPlacementPendingNotified = true;
                logResultPlacementDebug("Portal placement pending fallback branch. Markers=" + activePortalRatingMarkers.size());
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    logResultPlacementDebug("Delayed fallback result render pass. Markers=" + activePortalRatingMarkers.size());
                    for (Player player : playersInWorld(world)) {
                        SurvivorState state = playerStates.get(player.getUniqueId());
                resultSideReference.put(player.getUniqueId(), player.getLocation().clone());
                        if (state != null && state.lastStageRating != null) {
                            long elapsed = Math.max(1000L, System.currentTimeMillis() - stageStartedAtMs);
                            showStageRating(player, calculateStageRating(state, elapsed), elapsed, state);
                        }
                    }
                }, 30L);
                for (Player player : playersInWorld(world)) {
                    ChatFormatter.constructDivider(player, "§a§l-", 45);
                    ChatFormatter.sendCenteredMessage(player, "§a§lSTRONGHOLD STAGE CLEARED");
                    ChatFormatter.sendCenteredMessage(player, "");
                    ChatFormatter.sendCenteredMessage(player, ChatColor.GRAY + "You conquered Stage " + ChatColor.GREEN + clearedStage + ChatColor.GRAY + "!");
                    ChatFormatter.constructDivider(player, "§a§l-", 45);
                }
            }
        }

        private boolean isInsideExitPortal(Location location) {
            return exitPortalBounds != null && exitPortalBounds.contains(location);
        }

        private void tickExitPortalGuidance(World world) {
            if (world == null || exitPortalBounds == null) {
                return;
            }
            long now = System.currentTimeMillis();
            if (now < nextPortalGuideAt) {
                return;
            }
            nextPortalGuideAt = now + 600L;
            Location portalCenter = exitPortalBounds.guideTarget(world);
            for (Player player : playersInWorld(world)) {
                if (player == null || !player.isOnline() || player.getWorld() != world) {
                    continue;
                }
                Location playerLoc = player.getLocation();
                if (playerLoc.distanceSquared(portalCenter) <= (EXIT_GUIDE_MIN_RENDER_DISTANCE * EXIT_GUIDE_MIN_RENDER_DISTANCE)) {
                    continue;
                }
                List<Location> points = buildPortalPathPoints(playerLoc, portalCenter);
                int stride = points.size() > 80 ? 3 : points.size() > 30 ? 2 : 1;
                PathLocationUtils.renderDustTrailToPlayer(player, points, stride, 2, 0.1, EXIT_PORTAL_GUIDE_DUST);
                if (!points.isEmpty()) {
                    Location last = points.get(points.size() - 1);
                    player.spawnParticle(Particle.HAPPY_VILLAGER, last, 6, 0.35, 0.4, 0.35, 0.0);
                }
            }
        }

        private List<Location> buildPortalPathPoints(Location start, Location target) {
            Optional<Path> pathResult = pathfindingService.findPath(start, target);
            if (pathResult.isEmpty()) {
                return List.of(target.clone().add(0, 1.0, 0));
            }
            Path path = PathUtils.interpolate(pathResult.get(), 0.5);
            return PathLocationUtils.toLocations(start.getWorld(), path, 1.0, true, 0);
        }

        private void handlePlayerDeath(Player player) {
            if (player == null) {
                return;
            }
            if (!completed && (System.currentTimeMillis() - startedAtMs) < RUN_ENTRY_DEATH_GRACE_MS) {
                send(player, MessageType.WARNING, "Stronghold entry protection is active. You will not lose your run yet.");
                return;
            }
            SurvivorState state = playerStates.get(player.getUniqueId());
            if (state == null) {
                return;
            }
            highestAbsoluteWaveByPlayer.merge(player.getUniqueId(), Math.max(1, wave), Math::max);
            saveProgressionData();
            StrongholdResultsStorageGUI result = createSessionResultGui(player, state);
            pendingResultInventories.put(player.getUniqueId(), result);
            stopRun(worldId);
        }

        private boolean spawnWave(World world, int waveNumber) {
            List<Player> players = world.getPlayers().stream().filter(Player::isOnline).toList();
            if (players.isEmpty()) {
                return false;
            }
            currentWaveSpawned.clear();
            int spawnedCount = 0;
            int spawnCount = computeWaveSpawnCount(waveNumber, players.size());
            for (int i = 0; i < spawnCount; i++) {
                Player target = players.get(ThreadLocalRandom.current().nextInt(players.size()));
                Location spawn = findSpawnNear(target.getLocation(), origin, 14.0, 30.0);
                if (spawn == null) {
                    continue;
                }
                LivingEntity mob = StrongholdMobSpawnUtil.spawnStrongholdHostile(plugin.getCustomMobManager(), waveMobPool, spawn);
                if (mob == null) {
                    continue;
                }
                applyWaveMobScaling(mob, waveNumber, false);
                spawned.add(mob.getUniqueId());
                currentWaveSpawned.add(mob.getUniqueId());
                spawnedCount++;
                mobMotionStates.put(mob.getUniqueId(), new MobMotionState(
                        mob.getLocation().clone(),
                        System.currentTimeMillis(),
                        System.currentTimeMillis()));
                if (mob instanceof Mob hostile) {
                    if (pausedPlayers.contains(target.getUniqueId())) {
                        hostile.setTarget(null);
                        hostile.setAI(false);
                    } else {
                        hostile.setTarget(target);
                    }
                }
                world.spawnParticle(Particle.SMOKE, spawn, 10, 0.2, 0.2, 0.2, 0.01);
            }
            spawnedCount += spawnMilestoneBossIfNeeded(world, players, waveNumber);
            if (spawnedCount <= 0) {
                return false;
            }
            for (Player player : players) {
                StageProgress progress = toStageProgress(waveNumber);
                send(player, MessageType.INFO, "Stage " + ChatColor.WHITE + progress.stage() + ChatColor.GRAY + "-" + ChatColor.WHITE + progress.wave() + ChatColor.GRAY + " started.");
                if (plugin.getQuestManager() != null) {
                    plugin.getQuestManager().handleStrongholdWaveClear(player, waveNumber);
                    if (progress.wave() == WAVES_PER_STAGE) {
                        plugin.getQuestManager().handleStrongholdStageComplete(player, progress.stage());
                    }
                }
            }
            return true;
        }

        private int computeWaveSpawnCount(int waveNumber, int playerCount) {
            int safeWave = Math.max(1, waveNumber);
            int safePlayers = Math.max(1, playerCount);
            int waveScaling = safeWave + safeWave + (safeWave / 2);
            int partyBonus = (safePlayers - 1) * 4;
            return Math.min(52, 6 + waveScaling + partyBonus);
        }

        private int spawnMilestoneBossIfNeeded(World world, List<Player> players, int waveNumber) {
            if (waveNumber != 15 && waveNumber != 30) {
                return 0;
            }
            String mobId = waveNumber == 30 ? BOSS_MOB_ID : MINIBOSS_MOB_ID;
            String mobDisplay = resolveMobDisplayName(mobId);
            Player target = players.get(ThreadLocalRandom.current().nextInt(players.size()));
            Location spawn = findSpawnNear(target.getLocation(), origin, 12.0, 24.0);
            if (spawn == null) {
                return 0;
            }
            LivingEntity boss = StrongholdMobSpawnUtil.spawnStrongholdHostile(plugin.getCustomMobManager(), List.of(mobId), spawn);
            if (boss == null) {
                return 0;
            }
            applyWaveMobScaling(boss, waveNumber, true);
            applyKingSlimeSize(boss, waveNumber == 30 ? BOSS_SLIME_SIZE : MINIBOSS_SLIME_SIZE);
            waveBossId = boss.getUniqueId();
            spawned.add(boss.getUniqueId());
            currentWaveSpawned.add(boss.getUniqueId());
            mobMotionStates.put(boss.getUniqueId(), new MobMotionState(
                    boss.getLocation().clone(),
                    System.currentTimeMillis(),
                    System.currentTimeMillis()));
            if (boss instanceof Mob hostile) {
                hostile.setTarget(target);
            }
            for (Player player : players) {
                String title = waveNumber == 30 ? ChatColor.DARK_RED + "Boss Appeared" : ChatColor.RED + "Mini-Boss Appeared";
                player.sendTitle(title, ChatColor.WHITE + mobDisplay, 5, 50, 10);
            }
            return 1;
        }

        private void applyWaveMobScaling(LivingEntity mob, int waveNumber, boolean boss) {
            if (mob == null) {
                return;
            }
            StageProgress progress = toStageProgress(waveNumber);
            double healthMultiplier = Math.pow(1.0 + stageScalingConfig.stageHealthGrowth(), progress.stage() - 1)
                    * Math.pow(1.0 + stageScalingConfig.waveHealthGrowth(), progress.wave() - 1)
                    * Math.pow(1.0 + EXTRA_WAVE_HEALTH_SCALING, progress.wave() - 1)
                    * 1.10;
            if (boss) {
                healthMultiplier *= 1.35;
            }
            scaleAttributeBase(mob, Attribute.MAX_HEALTH, Math.min(20.0, healthMultiplier));
            AttributeInstance maxHealth = mob.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealth != null) {
                mob.setHealth(Math.min(maxHealth.getValue(), maxHealth.getBaseValue()));
            }
            double speedMultiplier = 1.0 + ((progress.stage() - 1) * stageScalingConfig.waveSpeedGrowth()) + ((progress.wave() - 1) * (stageScalingConfig.waveSpeedGrowth() * 0.5));
            scaleAttributeBase(mob, Attribute.MOVEMENT_SPEED, Math.min(1.5, speedMultiplier));
        }

        private void scaleAttributeBase(LivingEntity mob, Attribute attribute, double multiplier) {
            if (mob == null || attribute == null) {
                return;
            }
            AttributeInstance instance = mob.getAttribute(attribute);
            if (instance == null) {
                return;
            }
            double safeMultiplier = Math.max(0.01, multiplier);
            instance.setBaseValue(Math.max(0.01, instance.getBaseValue() * safeMultiplier));
        }

        private void applyKingSlimeSize(LivingEntity entity, int size) {
            if (!(entity instanceof Slime slime)) {
                return;
            }
            slime.setSize(Math.max(1, size));
            scaleAttributeBase(slime, Attribute.MAX_HEALTH, 10.0);
            AttributeInstance maxHealth = slime.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealth != null) {
                slime.setHealth(Math.min(maxHealth.getValue(), maxHealth.getBaseValue()));
            }
        }

        private String resolveMobDisplayName(String mobId) {
            if (mobId == null || mobId.isBlank()) {
                return "Unknown";
            }
            return plugin.getCustomMobManager().getDefinition(mobId)
                    .map(def -> ChatColor.translateAlternateColorCodes('&', def.displayName()))
                    .orElse(me.nakilex.levelplugin.utils.TextUtil.beautifyWords(mobId));
        }

        private void maybeDropStrongholdKey(Location at, Player killer) {
            if (at == null || at.getWorld() == null || killer == null || ThreadLocalRandom.current().nextDouble() > KEY_DROP_CHANCE) {
                return;
            }
            ItemStack key = createStrongholdKey();
            at.getWorld().dropItemNaturally(at, key);
            SurvivorState state = playerStates.get(killer.getUniqueId());
            if (state != null) {
                state.keysCollected++;
            }
            send(killer, MessageType.REWARD, "Rare drop: " + ChatColor.GOLD + "Stronghold Key");
        }

        private ItemStack createStrongholdKey() {
            Material keyType = Material.matchMaterial("TRIAL_KEY");
            if (keyType == null) {
                keyType = Material.TRIPWIRE_HOOK;
            }
            ItemStack item = new ItemStack(keyType);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GOLD + "Stronghold Key");
                List<String> lore = new ArrayList<>();
                lore.addAll(TooltipUtil.bulletList("Opens sealed gates in the stronghold."));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            ItemUtil.setSoulbound(item, true);
            return item;
        }

        private boolean forceEndAndShowRewards(Player target) {
            if (target == null || !target.isOnline()) {
                return false;
            }
            SurvivorState state = playerStates.get(target.getUniqueId());
            if (state == null) {
                return false;
            }
            highestAbsoluteWaveByPlayer.merge(target.getUniqueId(), Math.max(1, wave), Math::max);
            saveProgressionData();
            pendingResultInventories.put(target.getUniqueId(), createSessionResultGui(target, state));
            stopRun(worldId);
            return true;
        }

        private void endRunAndShowRewardsForAllPlayers(String completionMessage) {
            for (Map.Entry<UUID, SurvivorState> entry : new HashMap<>(playerStates).entrySet()) {
                UUID playerId = entry.getKey();
                SurvivorState state = entry.getValue();
                highestAbsoluteWaveByPlayer.merge(playerId, Math.max(1, wave), Math::max);
                saveProgressionData();
                Player player = Bukkit.getPlayer(playerId);
                if (player == null || !player.isOnline()) {
                    continue;
                }
                pendingResultInventories.put(playerId, createSessionResultGui(player, state));
                if (completionMessage != null && !completionMessage.isBlank()) {
                    send(player, MessageType.SUCCESS, completionMessage);
                }
            }
            stopRun(worldId);
        }

        private void openSessionResultGui(Player player, SurvivorState state) {
            if (player == null || state == null) {
                return;
            }
            StrongholdResultsStorageGUI gui = createSessionResultGui(player, state);
            openResultInventories.put(player.getUniqueId(), gui);
            gui.open(player);
        }

        private StrongholdResultsStorageGUI createSessionResultGui(Player player, SurvivorState state) {
            ItemStack summary = new ItemStack(Material.BOOK);
            ItemMeta meta = summary.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GOLD + "Run Summary");
                meta.setLore(List.of(
                        ChatColor.GRAY + "Reached Wave: " + ChatColor.WHITE + wave,
                        ChatColor.GRAY + "Run Rank: " + ChatColor.WHITE + state.level,
                        ChatColor.GRAY + "Stage Rating: " + ChatColor.WHITE + (state.lastStageRating == null ? "N/A" : state.lastStageRating),
                        ChatColor.GRAY + "Keys Found: " + ChatColor.WHITE + state.keysCollected,
                        ChatColor.GRAY + "Loot Stash: " + ChatColor.WHITE + state.lootStash.size() + " item(s)"
                ));
                summary.setItemMeta(meta);
            }
            return new StrongholdResultsStorageGUI(
                    player.getUniqueId().toString(),
                    plugin.getStorageEvents(),
                    summary,
                    state.lootStash
            );
        }

        private void initializePlayers(World world) {
            for (Player player : world.getPlayers()) {
                registerPlayer(player);
            }
        }

        private void syncRunPlayers(World world) {
            for (Player player : world.getPlayers()) {
                registerPlayer(player);
            }
            for (UUID playerId : new HashSet<>(playerStates.keySet())) {
                Player online = Bukkit.getPlayer(playerId);
                if (online == null || !online.isOnline() || !Objects.equals(online.getWorld().getUID(), worldId)) {
                    hideProgressBar(playerId);
                } else {
                    updateProgressBar(online, playerStates.get(playerId));
                }
            }
        }

        private void cleanupDeadSpawned() {
            spawned.removeIf(id -> {
                var entity = plugin.getServer().getEntity(id);
                return !(entity instanceof LivingEntity living) || living.isDead();
            });
            currentWaveSpawned.removeIf(id -> {
                var entity = plugin.getServer().getEntity(id);
                return !(entity instanceof LivingEntity living) || living.isDead();
            });
            mobMotionStates.entrySet().removeIf(entry -> {
                var entity = plugin.getServer().getEntity(entry.getKey());
                return !(entity instanceof LivingEntity living) || living.isDead();
            });
        }

        private void updateStuckMobPull(World world) {
            if (world == null) {
                return;
            }
            List<Player> players = world.getPlayers().stream().filter(Player::isOnline).toList();
            if (players.isEmpty()) {
                return;
            }
            long now = System.currentTimeMillis();
            for (Map.Entry<UUID, MobMotionState> entry : mobMotionStates.entrySet()) {
                UUID mobId = entry.getKey();
                var entity = plugin.getServer().getEntity(mobId);
                if (!(entity instanceof Mob mob) || mob.isDead()) {
                    continue;
                }
                MobMotionState state = entry.getValue();
                Location current = mob.getLocation();
                boolean movedRecently = state.lastLocation != null && current.distanceSquared(state.lastLocation) > STUCK_MOVE_EPSILON_SQ;
                if (movedRecently) {
                    state.lastLocation = current.clone();
                    state.lastMovedAtMs = now;
                }
                Player nearest = players.stream()
                        .min(java.util.Comparator.comparingDouble(p -> p.getLocation().distanceSquared(current)))
                        .orElse(null);
                if (nearest == null) {
                    continue;
                }
                boolean stuckLongEnough = now - state.lastMovedAtMs >= STUCK_PULL_DELAY_MS;
                double distanceSqToNearest = nearest.getLocation().distanceSquared(current);
                boolean tooFarFromPlayer = distanceSqToNearest >= MOB_RELOCATE_TRIGGER_DISTANCE * MOB_RELOCATE_TRIGGER_DISTANCE;
                if (!shouldRelocateMob(stuckLongEnough, tooFarFromPlayer, now, state.lastTeleportAtMs)) {
                    continue;
                }
                Location pulled = tooFarFromPlayer
                        ? findRelocationNearPlayer(nearest.getLocation())
                        : pullTowardPlayer(current, nearest.getLocation(), world);
                if (pulled == null) {
                    continue;
                }
                mob.teleport(pulled);
                mob.setTarget(nearest);
                state.lastLocation = pulled.clone();
                state.lastMovedAtMs = now;
                state.lastTeleportAtMs = now;
            }
        }

        private boolean shouldRelocateMob(boolean stuckLongEnough, boolean tooFarFromPlayer, long now, long lastTeleportAtMs) {
            if (!stuckLongEnough && !tooFarFromPlayer) {
                return false;
            }
            return now - lastTeleportAtMs >= MOB_RELOCATE_COOLDOWN_MS;
        }

        private Location findRelocationNearPlayer(Location playerLocation) {
            if (playerLocation == null || playerLocation.getWorld() == null) {
                return null;
            }
            World world = playerLocation.getWorld();
            for (int attempt = 0; attempt < 8; attempt++) {
                double xOffset = randomAxisOffset();
                double zOffset = randomAxisOffset();
                Location sample = playerLocation.clone().add(xOffset, 0.0, zOffset);
                Location spawn = resolveSurfaceSpawn(world, sample, playerLocation, true);
                if (spawn != null) {
                    return spawn;
                }
            }
            return findSpawnNear(playerLocation, playerLocation, MOB_RELOCATE_MIN_RADIUS, MOB_RELOCATE_MAX_RADIUS);
        }

        private Location pullTowardPlayer(Location mobLocation, Location playerLocation, World world) {
            if (mobLocation == null || playerLocation == null || world == null) {
                return null;
            }
            Vector toward = playerLocation.toVector().subtract(mobLocation.toVector());
            toward.setY(0.0);
            if (toward.lengthSquared() <= 0.0001) {
                return null;
            }
            double horizontalDistance = Math.sqrt(toward.lengthSquared());
            double pullDistance = Math.min(STUCK_PULL_DISTANCE, Math.max(0.5, horizontalDistance - 1.0));
            Vector offset = toward.normalize().multiply(pullDistance);
            Location destination = mobLocation.clone().add(offset);
            int y = world.getHighestBlockYAt(destination);
            destination.setY(y + 1.0);
            if (!destination.getBlock().getType().isAir()) {
                destination.add(0.0, 1.0, 0.0);
            }
            return destination;
        }

        private int countAliveCurrentWave() {
            int alive = 0;
            for (UUID id : currentWaveSpawned) {
                var entity = plugin.getServer().getEntity(id);
                if (entity instanceof LivingEntity living && !living.isDead()) {
                    alive++;
                }
            }
            return alive;
        }

        private int countAliveAllSpawned() {
            int alive = 0;
            for (UUID id : spawned) {
                var entity = plugin.getServer().getEntity(id);
                if (entity instanceof LivingEntity living && !living.isDead()) {
                    alive++;
                }
            }
            return alive;
        }

        private void registerPlayer(Player player) {
            if (player == null || !player.isOnline()) {
                return;
            }
            SurvivorState existing = playerStates.get(player.getUniqueId());
            if (existing != null) {
                if (existing.progressBar != null && !existing.progressBar.getPlayers().contains(player)) {
                    existing.progressBar.addPlayer(player);
                }
                updateProgressBar(player, existing);
                return;
            }
            PlayerClass currentClass = PlayerClassManager.getInstance().getPlayerClass(player);
            SurvivorState state = new SurvivorState(currentClass);
            playerStates.put(player.getUniqueId(), state);
            returnLocations.putIfAbsent(player.getUniqueId(), player.getLocation().clone());
            state.savedStorageContents = cloneItemArray(player.getInventory().getStorageContents());
            state.savedArmorContents = cloneItemArray(player.getInventory().getArmorContents());
            state.savedExtraContents = cloneItemArray(player.getInventory().getExtraContents());
            state.savedOffHand = player.getInventory().getItemInOffHand() == null ? null : player.getInventory().getItemInOffHand().clone();
            if (plugin.getGemsManager() != null) {
                int gems = plugin.getGemsManager().getTotalUnits(player);
                state.startingGems = gems;
                state.maxGemsDuringRun = gems;
            }

            PlayerClassManager.getInstance().setPlayerClass(player, PlayerClass.VILLAGER);
            state.progressBar = Bukkit.createBossBar("", BarColor.PURPLE, BarStyle.SOLID);
            state.progressBar.addPlayer(player);
            state.progressBar.setVisible(true);
            state.pendingUpgradeSelections = 1;
            state.pendingUpgrades = rollUpgradeChoices(state, 3);
            updateProgressBar(player, state);
            openUpgradeGui(player, state);
        }

        private void handleMobKillXp(Player killer, LivingEntity entity) {
            SurvivorState state = playerStates.get(killer.getUniqueId());
            if (state == null) {
                return;
            }
            int xpGain = 12 + Math.min(28, wave * 2);
            if (entity instanceof Mob) {
                xpGain += 3;
            }
            grantXp(killer, state, xpGain);
        }

        private void grantXp(Player player, SurvivorState state, int amount) {
            if (amount <= 0) {
                return;
            }
            state.xp += amount;
            int activeStage = Math.max(stageAnchor, toStageProgress(Math.max(1, wave)).stage());
            int required = xpRequiredForLevelAtStage(state.level, activeStage);
            int levelsGained = 0;
            while (state.xp >= required) {
                state.xp -= required;
                state.level++;
                levelsGained++;
                required = xpRequiredForLevelAtStage(state.level, activeStage);
            }
            updateProgressBar(player, state);
            if (levelsGained > 0) {
                send(player, MessageType.SUCCESS,
                        "Rank up! " + ChatColor.WHITE + "Run Rank " + state.level + ChatColor.GRAY + " reached.");
                state.pendingUpgradeSelections += levelsGained;
                state.pendingUpgrades = rollUpgradeChoices(state, 3);
                openUpgradeGui(player, state);
            }
        }

        private boolean grantDebugXp(Player player, int amount) {
            SurvivorState state = playerStates.get(player.getUniqueId());
            if (state == null) {
                return false;
            }
            grantXp(player, state, amount);
            return true;
        }

        private StageStatus getStageStatus(UUID playerId) {
            if (playerId == null || !playerStates.containsKey(playerId)) {
                return null;
            }
            int checkpointWave = ((Math.max(1, stageAnchor) - 1) * WAVES_PER_STAGE) + 1;
            StageProgress progress = toStageProgress(Math.max(checkpointWave, Math.max(1, wave)));
            SurvivorState state = playerStates.get(playerId);
            return new StageStatus(progress.stage(), progress.wave(), countAliveAllSpawned(), state == null ? "None" : state.activeArchetypeBuff);
        }

        private void updateProgressBar(Player player, SurvivorState state) {
            if (player == null || state == null || state.progressBar == null) {
                return;
            }
            int activeStage = Math.max(stageAnchor, toStageProgress(Math.max(1, wave)).stage());
            int required = xpRequiredForLevelAtStage(state.level, activeStage);
            double progress = required <= 0 ? 1.0 : Math.min(1.0, Math.max(0.0, state.xp / (double) required));
            state.progressBar.setTitle(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Stronghold Rank " + state.level
                    + ChatColor.DARK_GRAY + " | " + ChatColor.WHITE + state.xp + ChatColor.GRAY + "/" + ChatColor.WHITE + required + " XP");
            state.progressBar.setProgress(progress);
            state.progressBar.setVisible(true);
        }

        private void hideProgressBar(UUID playerId) {
            SurvivorState state = playerStates.get(playerId);
            if (state == null || state.progressBar == null) {
                return;
            }
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                state.progressBar.removePlayer(player);
            }
            state.progressBar.setVisible(false);
        }

        private void openUpgradeGui(Player player, SurvivorState state) {
            if (player == null || state == null) {
                return;
            }
            if (state.pendingUpgrades == null || state.pendingUpgrades.isEmpty()) {
                state.pendingUpgrades = rollUpgradeChoices(state, 3);
            }
            state.awaitingUpgradeSelection = true;
            setUpgradePausedState(player, state, true);
            Inventory inv = Bukkit.createInventory(player, UPGRADE_GUI_SIZE, UPGRADE_GUI_TITLE);
            populateUpgradeInventory(inv, state);
            player.openInventory(inv);
        }

        private void populateUpgradeInventory(Inventory inv, SurvivorState state) {
            if (state == null) {
                return;
            }
            RunSpellUpgradeGuiUtil.populateChoices(inv, state.pendingUpgrades, choice -> upgradeItem(choice, state), true, UPGRADE_REROLL_SLOT);
        }

        private ItemStack upgradeItem(UpgradeChoice choice, SurvivorState state) {
            ItemStack item;
            int spellCurrentRank = state == null || choice == null || choice.baseSpellId == null
                    ? 0
                    : state.ownedSpellRanks.getOrDefault(choice.baseSpellId, 0);
            if (choice.type == UpgradeType.SPELL_UNLOCK || choice.type == UpgradeType.SPELL_UPGRADE) {
                return RunSpellUpgradeGuiUtil.createSpellUpgradeChoiceItem(new RunSpellUpgradeGuiUtil.SpellUpgradeView(
                        choice.displayName,
                        choice.description,
                        choice.baseSpellId,
                        choice.resultSpellId,
                        spellCurrentRank,
                        choice.type == UpgradeType.SPELL_UNLOCK,
                        List.of(),
                        "to choose this upgrade"));
            }
            Material material = choice.type == UpgradeType.STAT ? Material.NETHER_STAR : Material.ENCHANTED_BOOK;
            item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                if (meta.getDisplayName() == null || meta.getDisplayName().isBlank()) {
                    meta.setDisplayName(ChatColor.GOLD + choice.displayName);
                }
                List<String> lore = new ArrayList<>();
                appendWrappedBulletBlock(lore, choice.description);
                lore.add(" ");
                if (choice.type == UpgradeType.GLOBAL_COOLDOWN) {
                    lore.add(TooltipUtil.sectionHeader("Global Cooldown"));
                    lore.add(TooltipUtil.iconLabelValueLine("✣", ChatColor.GOLD, ChatColor.GRAY, "Cooldown Tier",
                            ChatColor.WHITE, String.valueOf(state.cooldownUpgradeTier)));
                    lore.add(TooltipUtil.iconLabelValueLine("✦", ChatColor.AQUA, ChatColor.GRAY, "Effect",
                            ChatColor.GREEN, "-10% global skill cooldown"));
                } else if (choice.statType != null) {
                    lore.add(TooltipUtil.sectionHeader("Temporary Bonus"));
                    lore.add(TooltipUtil.iconLabelValueLine("✦", ChatColor.AQUA, ChatColor.GRAY, "Temporary Bonus",
                            ChatColor.GREEN, "+" + choice.statAmount + "% " + choice.statType.getDisplayName()));
                }
                lore.add(TooltipUtil.sectionDividerByPixels(150));
                lore.addAll(TooltipUtil.clickInstructions("to choose this upgrade", null));
                meta.setLore(lore);
                item.setItemMeta(meta);
                TooltipUtil.centerItemName(item);
            }
            return item;
        }

        private void appendWrappedBulletBlock(List<String> lore, String description) {
            if (lore == null || description == null || description.isBlank()) {
                return;
            }
            List<String> wrapped = TooltipUtil.wrapLoreLine(ChatColor.GRAY + description.trim(), 210,
                    ChatColor.DARK_GRAY + "  " + ChatColor.GRAY);
            if (wrapped.isEmpty()) {
                return;
            }
            lore.add(TooltipUtil.bulletLine(wrapped.get(0)));
            for (int i = 1; i < wrapped.size(); i++) {
                lore.add(wrapped.get(i));
            }
        }

        private void handleUpgradeClick(Player player, int slot) {
            SurvivorState state = playerStates.get(player.getUniqueId());
            if (state == null || state.pendingUpgrades == null || state.pendingUpgrades.isEmpty()) {
                player.closeInventory();
                return;
            }
            if (state.rerollAnimating) {
                send(player, MessageType.WARNING, "Reroll animation in progress.");
                return;
            }
            if (slot == UPGRADE_REROLL_SLOT) {
                playRerollAnimation(player, state);
                return;
            }
            int idx = RunSpellUpgradeGuiUtil.choiceIndex(slot);
            if (idx < 0 || idx >= state.pendingUpgrades.size()) {
                return;
            }
            UpgradeChoice selected = state.pendingUpgrades.get(idx);
            applyUpgrade(player, state, selected);
            completeUpgradeSelection(player, state);
        }

        private void completeUpgradeSelection(Player player, SurvivorState state) {
            state.pendingUpgrades = List.of();
            state.pendingUpgradeSelections = Math.max(0, state.pendingUpgradeSelections - 1);
            state.skipNextUpgradeReopen = true;
            if (state.pendingUpgradeSelections > 0) {
                state.awaitingUpgradeSelection = true;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        openUpgradeGui(player, state);
                    }
                });
                return;
            }
            state.awaitingUpgradeSelection = false;
            setUpgradePausedState(player, state, false);
            player.closeInventory();
        }

        private void playRerollAnimation(Player player, SurvivorState state) {
            if (player == null || state == null || state.rerollAnimating) {
                return;
            }
            state.rerollAnimating = true;
            final int cycles = 10;
            final long intervalTicks = 1L;
            new org.bukkit.scheduler.BukkitRunnable() {
                private int cycle;

                @Override
                public void run() {
                    Player online = Bukkit.getPlayer(player.getUniqueId());
                    if (online == null || !online.isOnline()) {
                        state.rerollAnimating = false;
                        cancel();
                        return;
                    }
                    if (cycle < cycles) {
                        state.pendingUpgrades = rollUpgradeChoices(state, 3);
                        Inventory top = online.getOpenInventory() == null ? null : online.getOpenInventory().getTopInventory();
                        if (top != null && top.getSize() == UPGRADE_GUI_SIZE) {
                            populateUpgradeInventory(top, state);
                            online.updateInventory();
                        }
                        cycle++;
                        online.playSound(online.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.40f, 1.85f);
                        return;
                    }
                    state.pendingUpgrades = rollUpgradeChoices(state, 3);
                    Inventory top = online.getOpenInventory() == null ? null : online.getOpenInventory().getTopInventory();
                    if (top != null && top.getSize() == UPGRADE_GUI_SIZE) {
                        populateUpgradeInventory(top, state);
                        online.updateInventory();
                    }
                    state.rerollAnimating = false;
                    send(online, MessageType.INFO, "Rerolled Stronghold upgrades.");
                    cancel();
                }
            }.runTaskTimer(plugin, 0L, intervalTicks);
        }

        private void handleUpgradeClose(Player player) {
            SurvivorState state = playerStates.get(player.getUniqueId());
            if (state == null) {
                return;
            }
            if (state.skipNextUpgradeReopen) {
                state.skipNextUpgradeReopen = false;
                return;
            }
            if (!state.awaitingUpgradeSelection || state.pendingUpgrades == null || state.pendingUpgrades.isEmpty()) {
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player online = Bukkit.getPlayer(player.getUniqueId());
                if (online == null || !online.isOnline()) {
                    return;
                }
                SurvivorState onlineState = playerStates.get(online.getUniqueId());
                if (onlineState == null || !onlineState.awaitingUpgradeSelection || onlineState.pendingUpgrades.isEmpty()) {
                    return;
                }
                openUpgradeGui(online, onlineState);
            });
        }

        private void setUpgradePausedState(Player player, SurvivorState state, boolean paused) {
            if (player == null || state == null) {
                return;
            }
            state.upgradePaused = paused;
            if (paused) {
                pausedPlayers.add(player.getUniqueId());
                player.setInvisible(true);
                setEnemyFreezeState(true);
                return;
            }
            pausedPlayers.remove(player.getUniqueId());
            player.setInvisible(false);
            if (pausedPlayers.isEmpty()) {
                setEnemyFreezeState(false);
            }
        }

        private void setEnemyFreezeState(boolean frozen) {
            for (UUID id : spawned) {
                var entity = plugin.getServer().getEntity(id);
                if (!(entity instanceof LivingEntity living) || living.isDead()) {
                    continue;
                }
                if (living instanceof Mob mob) {
                    mob.setTarget(null);
                    mob.setAware(!frozen);
                    mob.setAI(!frozen);
                }
                if (frozen) {
                    living.setVelocity(new Vector(0.0, 0.0, 0.0));
                    living.setNoDamageTicks(10);
                }
            }
        }

        private boolean isUpgradePaused(UUID playerId) {
            return playerId != null && pausedPlayers.contains(playerId);
        }

        private void applyUpgrade(Player player, SurvivorState state, UpgradeChoice choice) {
            if (choice.type == UpgradeType.STAT && choice.statType != null && choice.statAmount > 0) {
                state.tempStatBonuses.merge(choice.statType, choice.statAmount, Integer::sum);
                applyTempStatDelta(player.getUniqueId(), choice.statType, choice.statAmount);
                send(player, MessageType.SUCCESS, "Stronghold buff: " + ChatColor.GREEN + "+" + choice.statAmount
                        + "% " + choice.statType.getDisplayName() + ChatColor.GRAY + " (temporary).");
                return;
            }
            if (choice.type == UpgradeType.GLOBAL_COOLDOWN) {
                state.cooldownUpgradeTier++;
                send(player, MessageType.SUCCESS, "Skill upgrade applied: "
                        + ChatColor.GREEN + "-10% global cooldown" + ChatColor.GRAY + " (Tier "
                        + ChatColor.WHITE + state.cooldownUpgradeTier + ChatColor.GRAY + ").");
                return;
            }
            if (choice.baseSpellId == null || choice.resultSpellId == null) {
                return;
            }
            String base = choice.baseSpellId.toLowerCase(Locale.ROOT);
            if (isMobilityBaseSpell(base) && state.ownedSpellRanks.getOrDefault(base, 0) <= 0) {
                Set<String> ownedMobility = ownedMobilityBaseIds(state);
                ownedMobility.remove(base);
                if (!ownedMobility.isEmpty()) {
                    send(player, MessageType.WARNING, "You can only have one mobility spell during a Stronghold run.");
                    return;
                }
            }
            int nextRank = Math.max(1, state.ownedSpellRanks.getOrDefault(base, 0) + 1);
            state.ownedSpellRanks.put(base, nextRank);
            state.activeSpellByBase.put(base, choice.resultSpellId.toLowerCase(Locale.ROOT));
            if (isMobilityBaseSpell(base)) {
                addSpellCharges(state, base, 1);
            }
            send(player, MessageType.SUCCESS, "Skill upgrade unlocked: "
                    + ChatColor.WHITE + choice.displayName + ChatColor.GRAY + ".");
        }

        private List<UpgradeChoice> rollUpgradeChoices(SurvivorState state, int count) {
            refreshAutoCastPoolIfNeeded();
            List<UpgradeChoice> spellCandidates = new ArrayList<>();
            for (String baseId : autoCastBasePool) {
                UpgradeChoice spellChoice = spellUpgradeChoiceFor(state, baseId);
                if (spellChoice != null) {
                    spellCandidates.add(spellChoice);
                }
            }
            int mobilityOwnedCount = ownedMobilityBaseIds(state).size();
            for (String baseId : mobilityBasePool) {
                UpgradeChoice mobilityChoice = spellUpgradeChoiceFor(state, baseId);
                if (mobilityChoice == null) {
                    continue;
                }
                int rank = state.ownedSpellRanks.getOrDefault(baseId.toLowerCase(Locale.ROOT), 0);
                if (rank <= 0 && mobilityOwnedCount > 0) {
                    continue;
                }
                spellCandidates.add(mobilityChoice);
            }

            List<UpgradeChoice> rolled = new ArrayList<>();
            ThreadLocalRandom rng = ThreadLocalRandom.current();
            while (!spellCandidates.isEmpty() && rolled.size() < count) {
                int pick = rng.nextInt(spellCandidates.size());
                rolled.add(spellCandidates.remove(pick));
            }

            if (rolled.size() < count) {
                List<UpgradeChoice> statCandidates = new ArrayList<>(List.of(
                        new UpgradeChoice(UpgradeType.STAT, "Power Surge", "Temporary Strength boost (+2%) for this run only.", null, null, StatsManager.StatType.STR, 2),
                        new UpgradeChoice(UpgradeType.STAT, "Swiftfoot", "Temporary Agility boost (+2%) for this run only.", null, null, StatsManager.StatType.AGI, 2),
                        new UpgradeChoice(UpgradeType.STAT, "Arcane Focus", "Temporary Intelligence boost (+2%) for this run only.", null, null, StatsManager.StatType.INT, 2),
                        new UpgradeChoice(UpgradeType.STAT, "Vital Reserve", "Temporary Vitality boost (+2%) for this run only.", null, null, StatsManager.StatType.VIT, 2),
                        new UpgradeChoice(UpgradeType.GLOBAL_COOLDOWN, "Arcane Tempo", "Reduce all loadout skill cooldowns globally by 10%.", null, null, null, 0)
                ));
                while (!statCandidates.isEmpty() && rolled.size() < count) {
                    int pick = rng.nextInt(statCandidates.size());
                    rolled.add(statCandidates.remove(pick));
                }
            }

            while (rolled.size() < count) {
                rolled.add(new UpgradeChoice(UpgradeType.STAT, "Vital Reserve", "Temporary Vitality boost (+2%) for this run only.", null, null, StatsManager.StatType.VIT, 2));
            }
            return rolled;
        }

        private void refreshAutoCastPoolIfNeeded() {
            if (!autoCastBasePool.isEmpty() || !mobilityBasePool.isEmpty()) {
                return;
            }
            initializeAutoCastPool();
        }

        private Set<String> ownedMobilityBaseIds(SurvivorState state) {
            Set<String> owned = new HashSet<>();
            if (state == null) {
                return owned;
            }
            for (Map.Entry<String, Integer> entry : state.ownedSpellRanks.entrySet()) {
                if (entry.getValue() == null || entry.getValue() <= 0) {
                    continue;
                }
                if (isMobilityBaseSpell(entry.getKey())) {
                    owned.add(entry.getKey().toLowerCase(Locale.ROOT));
                }
            }
            return owned;
        }

        private boolean isMobilityBaseSpell(String baseSpellId) {
            if (baseSpellId == null || baseSpellId.isBlank()) {
                return false;
            }
            return mobilityBasePool.contains(baseSpellId.toLowerCase(Locale.ROOT));
        }

        private UpgradeChoice spellUpgradeChoiceFor(SurvivorState state, String baseSpellId) {
            SpellRegistry registry = SpellRegistry.getInstance();
            String base = baseSpellId.toLowerCase(Locale.ROOT);
            int rank = state.ownedSpellRanks.getOrDefault(base, 0);
            int ownedCount = (int) state.ownedSpellRanks.values().stream().filter(value -> value != null && value > 0).count();
            if (rank <= 0) {
                if (ownedCount >= MAX_ACTIVE_STRONGHOLD_SPELLS) {
                    return null;
                }
                SpellRegistry.SpellEntry baseEntry = registry.getSpell(base);
                if (baseEntry == null || baseEntry.definition() == null) {
                    return null;
                }
                return new UpgradeChoice(UpgradeType.SPELL_UNLOCK, "Unlock " + baseEntry.definition().displayName(),
                        "Adds this spell to your loadout.", base, baseEntry.definition().id(), null, 0);
            }
            SpellProgression progression = registry.getProgression(base);
            if (progression == null || progression.upgradeSpellIds() == null || progression.upgradeSpellIds().isEmpty()) {
                return null;
            }
            int nextIndex = rank - 1;
            if (nextIndex < 0 || nextIndex >= progression.upgradeSpellIds().size()) {
                return null;
            }
            String nextSpell = progression.upgradeSpellIds().get(nextIndex);
            SpellRegistry.SpellEntry upgraded = registry.getSpell(nextSpell);
            if (upgraded == null || upgraded.definition() == null) {
                return null;
            }
            return new UpgradeChoice(UpgradeType.SPELL_UPGRADE, "Upgrade: " + upgraded.definition().displayName(),
                    "Improves an already owned loadout skill.", base, upgraded.definition().id(), null, 0);
        }

        private List<String> getSpellPossibilities(UUID playerId) {
            SurvivorState state = playerStates.get(playerId);
            if (state == null) {
                return List.of();
            }
            refreshAutoCastPoolIfNeeded();
            List<String> lines = new ArrayList<>();
            autoCastBasePool.stream().sorted().forEach(baseId -> {
                UpgradeChoice choice = spellUpgradeChoiceFor(state, baseId);
                if (choice == null) {
                    return;
                }
                int currentRank = state.ownedSpellRanks.getOrDefault(baseId, 0);
                String status = currentRank <= 0 ? "unlock" : "upgrade";
                lines.add(baseId + " -> " + choice.resultSpellId + " (" + status + ", rank " + currentRank + ")");
            });
            mobilityBasePool.stream().sorted().forEach(baseId -> {
                UpgradeChoice choice = spellUpgradeChoiceFor(state, baseId);
                if (choice == null) {
                    return;
                }
                int currentRank = state.ownedSpellRanks.getOrDefault(baseId, 0);
                String status = currentRank <= 0 ? "unlock" : "upgrade";
                lines.add(baseId + " -> " + choice.resultSpellId + " (" + status + ", rank " + currentRank + ", mobility)");
            });
            return lines;
        }

        private void tickAutoCast() {
            World world = plugin.getServer().getWorld(worldId);
            if (world == null) {
                return;
            }
            long now = System.currentTimeMillis();
            for (Map.Entry<UUID, SurvivorState> entry : playerStates.entrySet()) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player == null || !player.isOnline() || !Objects.equals(player.getWorld().getUID(), worldId)) {
                    continue;
                }
                if (pausedPlayers.contains(player.getUniqueId())) {
                    continue;
                }
                SurvivorState state = entry.getValue();
                if (plugin.getGemsManager() != null) {
                    state.maxGemsDuringRun = Math.max(state.maxGemsDuringRun, plugin.getGemsManager().getTotalUnits(player));
                }
                applyArchetypeBuff(player, state);
                refillMobilityCharges(state, now);
                for (String spellId : state.activeSpellByBase.values()) {
                    SpellRegistry.SpellEntry spellEntry = SpellRegistry.getInstance().getSpell(spellId);
                    if (spellEntry == null || spellEntry.definition() == null) {
                        continue;
                    }
                    SpellDefinition definition = spellEntry.definition();
                    if (RunSpellCastUtil.manualCastTrigger(definition) != ManualCastTrigger.NONE) {
                        continue;
                    }
                    if (!RunSpellCastUtil.shouldAutoCastSpellNow(player, definition.id())) {
                        continue;
                    }
                    long cooldown = RunSpellCastUtil.computeAutoCastCooldownMs(player, definition, state == null ? 0 : state.cooldownUpgradeTier, BASE_AUTOCAST_COOLDOWN_MS);
                    long last = state.lastCastAtBySpell.getOrDefault(spellId, 0L);
                    if (now - last < cooldown) {
                        continue;
                    }
                    RunSpellCastUtil.castSpell(plugin, player, spellEntry, RunSpellCastUtil.createSyntheticInputEvent(player, "AUTO"), false, p -> hasValidStrongholdWeapon(p, false));
                    state.lastCastAtBySpell.put(spellId, now);
                }
            }
        }

        private String normalizeBaseSpellId(String spellId) {
            if (spellId == null || spellId.isBlank()) {
                return "";
            }
            String normalized = spellId.toLowerCase(Locale.ROOT);
            SpellRegistry registry = SpellRegistry.getInstance();
            SpellProgression progression = registry.getProgression(normalized);
            if (progression != null) {
                return progression.baseSpellId().toLowerCase(Locale.ROOT);
            }
            for (SpellProgression candidate : registry.getAllProgressions()) {
                if (candidate == null || candidate.upgradeSpellIds() == null) {
                    continue;
                }
                for (String upgradeId : candidate.upgradeSpellIds()) {
                    if (upgradeId != null && upgradeId.equalsIgnoreCase(normalized)) {
                        return candidate.baseSpellId().toLowerCase(Locale.ROOT);
                    }
                }
            }
            return normalized;
        }

        private int getSpellCharges(SurvivorState state, String baseSpellId) {
            if (state == null || baseSpellId == null || baseSpellId.isBlank()) {
                return 0;
            }
            return Math.max(0, state.spellChargesByBase.getOrDefault(baseSpellId.toLowerCase(Locale.ROOT), 0));
        }

        private int getMaxSpellCharges(SurvivorState state, String baseSpellId) {
            if (state == null || baseSpellId == null || baseSpellId.isBlank()) {
                return 0;
            }
            return Math.max(0, state.ownedSpellRanks.getOrDefault(baseSpellId.toLowerCase(Locale.ROOT), 0));
        }

        private void addSpellCharges(SurvivorState state, String baseSpellId, int amount) {
            if (state == null || baseSpellId == null || baseSpellId.isBlank() || amount <= 0) {
                return;
            }
            String base = baseSpellId.toLowerCase(Locale.ROOT);
            int maxCharges = getMaxSpellCharges(state, base);
            int current = getSpellCharges(state, base);
            state.spellChargesByBase.put(base, Math.min(maxCharges, current + amount));
        }

        private void refillMobilityCharges(SurvivorState state, long nowMs) {
            if (state == null || nowMs - state.lastMobilityChargeRefillAt < MOBILITY_CHARGE_REFILL_MS) {
                return;
            }
            state.lastMobilityChargeRefillAt = nowMs;
            for (Map.Entry<String, String> entry : state.activeSpellByBase.entrySet()) {
                String base = entry.getKey() == null ? "" : entry.getKey().toLowerCase(Locale.ROOT);
                String spellId = entry.getValue();
                SpellRegistry.SpellEntry spellEntry = spellId == null ? null : SpellRegistry.getInstance().getSpell(spellId);
                if (base.isBlank() || spellEntry == null || spellEntry.definition() == null || !spellEntry.definition().movementSpell()) {
                    continue;
                }
                if (getMaxSpellCharges(state, base) <= 0) {
                    continue;
                }
                addSpellCharges(state, base, 1);
            }
        }

        private boolean consumeSpellCharge(SurvivorState state, String baseSpellId) {
            if (state == null || baseSpellId == null || baseSpellId.isBlank()) {
                return false;
            }
            String base = baseSpellId.toLowerCase(Locale.ROOT);
            int current = getSpellCharges(state, base);
            if (current <= 0) {
                return false;
            }
            state.spellChargesByBase.put(base, current - 1);
            return true;
        }

        private boolean hasValidStrongholdWeapon(Player player, boolean notifyFailure) {
            if (player == null) {
                return false;
            }
            if (!SpellAccessUtil.isHoldingValidClassWeapon(player)) {
                return false;
            }
            String requirementFailure = SpellAccessUtil.getHeldWeaponRequirementFailure(player);
            if (requirementFailure != null) {
                if (notifyFailure) {
                    send(player, MessageType.WARNING, requirementFailure);
                }
                return false;
            }
            return true;
        }

        private boolean tryManualCastSpell(Player player, ManualCastTrigger trigger) {
            if (trigger == null || trigger == ManualCastTrigger.NONE) {
                return false;
            }
            if (player == null || pausedPlayers.contains(player.getUniqueId())) {
                return false;
            }
            if (!hasValidStrongholdWeapon(player, true)) {
                return false;
            }
            long now = System.currentTimeMillis();
            long lastAttemptAt = lastManualCastAttemptAt.getOrDefault(player.getUniqueId(), 0L);
            if (now - lastAttemptAt < MANUAL_CAST_DEBOUNCE_MS) {
                return false;
            }
            lastManualCastAttemptAt.put(player.getUniqueId(), now);
            SurvivorState state = playerStates.get(player.getUniqueId());
            SpellRegistry.SpellEntry manualSpell = RunSpellCastUtil.resolveOwnedManualSpell(state == null ? null : state.activeSpellByBase, trigger);
            if (manualSpell == null) {
                return false;
            }
            SpellDefinition definition = manualSpell.definition();
            String baseSpellId = normalizeBaseSpellId(definition.id());
            if (definition.movementSpell() && getSpellCharges(state, baseSpellId) <= 0) {
                long elapsedMs = Math.max(0L, now - state.lastMobilityChargeRefillAt);
                long remainingMs = Math.max(0L, MOBILITY_CHARGE_REFILL_MS - elapsedMs);
                int seconds = Math.max(1, (int) Math.ceil(remainingMs / 1000.0));
                send(player, MessageType.WARNING, definition.displayName() + " has no charges left. Next charge in " + seconds + "s.");
                return true;
            }
            SpellCastManager castManager = SpellCastManager.getInstance();
            long remainingCooldown = castManager.getRemainingCooldownMs(player, definition);
            if (SpellCastManager.areCooldownsEnabled() && remainingCooldown > 0L) {
                int seconds = (int) Math.ceil(remainingCooldown / 1000.0);
                send(player, MessageType.WARNING, definition.displayName() + " is on cooldown for " + seconds + "s.");
                return true;
            }
            int manaCost = castManager.getManaCost(player, definition);
            StatsManager.PlayerStats stats = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
            if (SpellCastManager.areManaCostsEnabled() && manaCost > 0 && stats.getCurrentMana() < manaCost) {
                send(player, MessageType.WARNING, "Not enough mana for " + definition.displayName() + " (" + manaCost + ").");
                return true;
            }
            boolean casted = RunSpellCastUtil.castSpell(plugin, player, manualSpell, RunSpellCastUtil.createSyntheticInputEvent(player, trigger == ManualCastTrigger.RIGHT_CLICK
                    ? "STRONGHOLD_MOBILITY" : "STRONGHOLD_BASIC"), true, p -> hasValidStrongholdWeapon(p, false));
            if (casted && definition.movementSpell()) {
                consumeSpellCharge(state, baseSpellId);
            }
            return casted;
        }


        private String spellArchetypeKey(String spellId) {
            if (spellId == null || spellId.isBlank()) {
                return "";
            }
            String id = spellId.toLowerCase(Locale.ROOT);
            if (id.startsWith("mage_") || id.startsWith("meteor") || id.startsWith("blackhole")) {
                return "mage";
            }
            int idx = id.indexOf('_');
            return idx <= 0 ? "" : id.substring(0, idx);
        }

        private void applyArchetypeBuff(Player player, SurvivorState state) {
            if (player == null || state == null) {
                return;
            }
            Map<String, Integer> classCounts = new HashMap<>();
            for (String spellId : state.activeSpellByBase.values()) {
                if (spellId == null) continue;
                String clazz = spellArchetypeKey(spellId);
                if (clazz == null || clazz.isBlank()) continue;
                classCounts.merge(clazz, 1, Integer::sum);
            }
            String buff = "None";
            player.removeScoreboardTag(STRONGHOLD_MAGE_METEOR_RADIUS_TAG);
            if (classCounts.getOrDefault("rogue", 0) >= 3) {
                DoubleJumpListener.setExternalBonusJumps(player.getUniqueId(), 1);
                DoubleJumpListener.setExternalArcSlashOnJump(player.getUniqueId(), true);
                buff = "Rogue Trinity: +1 Air Jump Arc";
            } else {
                DoubleJumpListener.setExternalBonusJumps(player.getUniqueId(), 0);
                DoubleJumpListener.setExternalArcSlashOnJump(player.getUniqueId(), false);
            }
            if (classCounts.getOrDefault("warrior", 0) >= 3) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 0, true, false, false));
                buff = "Warrior Trinity: Resist";
            }
            if (classCounts.getOrDefault("mage", 0) >= 3) {
                player.addScoreboardTag(STRONGHOLD_MAGE_METEOR_RADIUS_TAG);
                buff = "Mage Trinity: Meteor AoE x3";
            }
            if (classCounts.getOrDefault("archer", 0) >= 3) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0, true, false, false));
                buff = "Archer Trinity: Haste";
            }
            if (classCounts.getOrDefault("rogue", 0) >= 1 && classCounts.getOrDefault("warrior", 0) >= 1
                    && classCounts.getOrDefault("mage", 0) >= 1 && classCounts.getOrDefault("archer", 0) >= 1) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 60, 0, true, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0, true, false, false));
                buff = "Prismatic Surge: Speed+Power";
            }
            state.activeArchetypeBuff = buff;
        }

        private void applyTempStatDelta(UUID playerId, StatsManager.StatType statType, int delta) {
            if (playerId == null || statType == null || delta == 0) {
                return;
            }
            StatsManager.PlayerStats stats = StatsManager.getInstance().getPlayerStats(playerId);
            switch (statType) {
                case STR -> stats.bonusStrength += delta;
                case AGI -> stats.bonusAgility += delta;
                case INT -> stats.bonusIntelligence += delta;
                case DEX -> stats.bonusDexterity += delta;
                case VIT -> stats.bonusVitality += delta;
                case WIL -> stats.bonusWill += delta;
                case TEC -> stats.bonusTechnique += delta;
            }
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                StatsManager.getInstance().recalcDerivedStats(player);
            }
        }

        private boolean captureLootToStash(Player player, ItemStack pickedUp) {
            if (player == null || pickedUp == null || pickedUp.getType().isAir()) {
                return false;
            }
            SurvivorState state = playerStates.get(player.getUniqueId());
            if (state == null) {
                return false;
            }
            if (isStrongholdKey(pickedUp)) {
                ItemStack keyCopy = pickedUp.clone();
                Map<Integer, ItemStack> overflow = player.getInventory().addItem(keyCopy);
                if (!overflow.isEmpty()) {
                    for (ItemStack leftover : overflow.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                    }
                }
                send(player, MessageType.REWARD,
                        ChatColor.GREEN + "+ " + ChatColor.GOLD + STRONGHOLD_KEY_NAME + ChatColor.GRAY + " added to inventory");
                return true;
            }
            state.lootStash.add(pickedUp.clone());
            String itemName = pickedUp.hasItemMeta() && pickedUp.getItemMeta() != null
                    && pickedUp.getItemMeta().hasDisplayName()
                    ? pickedUp.getItemMeta().getDisplayName()
                    : me.nakilex.levelplugin.utils.TextUtil.beautifyWords(pickedUp.getType().name());
            itemName = normalizeStorageItemLabel(itemName);
            send(player, MessageType.REWARD,
                    ChatColor.GREEN + "+ " + ChatColor.WHITE + itemName + ChatColor.GRAY + " added to storage");
            return true;
        }

        private ItemStack[] cloneItemArray(ItemStack[] source) {
            if (source == null) {
                return new ItemStack[0];
            }
            ItemStack[] out = new ItemStack[source.length];
            for (int i = 0; i < source.length; i++) {
                out[i] = source[i] == null ? null : source[i].clone();
            }
            return out;
        }

        private void restorePlayerAfterRun(UUID playerId, SurvivorState state) {
            if (state == null) {
                return;
            }
            state.awaitingUpgradeSelection = false;
            state.skipNextUpgradeReopen = true;
            state.upgradePaused = false;
            pausedPlayers.remove(playerId);
            if (pausedPlayers.isEmpty()) {
                setEnemyFreezeState(false);
            }
            for (Map.Entry<StatsManager.StatType, Integer> buff : state.tempStatBonuses.entrySet()) {
                applyTempStatDelta(playerId, buff.getKey(), -Math.max(0, buff.getValue()));
            }
            Player online = Bukkit.getPlayer(playerId);
            if (online != null && online.isOnline()) {
                online.setInvisible(false);
                DoubleJumpListener.setExternalBonusJumps(playerId, 0);
                DoubleJumpListener.setExternalArcSlashOnJump(playerId, false);
                online.setAllowFlight(false);
                online.setFlying(false);
                PlayerClassManager.getInstance().setPlayerClass(online, state.originalClass);
                online.getInventory().setStorageContents(cloneItemArray(state.savedStorageContents));
                online.getInventory().setArmorContents(cloneItemArray(state.savedArmorContents));
                online.getInventory().setExtraContents(cloneItemArray(state.savedExtraContents));
                online.getInventory().setItemInOffHand(state.savedOffHand == null ? null : state.savedOffHand.clone());
                if (state.progressBar != null) {
                    state.progressBar.removePlayer(online);
                    state.progressBar.setVisible(false);
                }
                online.closeInventory();
                if (!online.isDead()) {
                    Location back = consumeReturnLocation(playerId, online.getLocation());
                    if (back != null && back.getWorld() != null) {
                        online.teleport(back);
                    }
                    openPendingResultsAfterTeleport(online);
                }
                if (plugin.getGemsManager() != null) {
                    int current = plugin.getGemsManager().getTotalUnits(online);
                    int persisted = Math.max(current, Math.max(state.startingGems, state.maxGemsDuringRun));
                    if (persisted > current) {
                        plugin.getGemsManager().setTotalUnits(online, persisted);
                    }
                }
            }
        }

        private Location findSpawnNear(Location playerLoc, Location fallbackOrigin, double minRadius, double maxRadius) {
            World world = playerLoc.getWorld();
            if (world == null) {
                return null;
            }
            double safeMinRadius = Math.max(MIN_ENEMY_SPAWN_RADIUS, minRadius);
            double safeMaxRadius = Math.max(safeMinRadius + 0.5, maxRadius);
            Location grassSpawn = findSpawnNearWithGroundRule(world, playerLoc, fallbackOrigin, safeMinRadius, safeMaxRadius, true);
            if (grassSpawn != null) {
                return grassSpawn;
            }
            return findSpawnNearWithGroundRule(world, playerLoc, fallbackOrigin, safeMinRadius, safeMaxRadius, false);
        }

        private Location findSpawnNearWithGroundRule(World world,
                                                     Location playerLoc,
                                                     Location fallbackOrigin,
                                                     double minRadius,
                                                     double maxRadius,
                                                     boolean grassOnly) {
            for (int attempt = 0; attempt < 20; attempt++) {
                double angle = ThreadLocalRandom.current().nextDouble(0, Math.PI * 2);
                double dist = ThreadLocalRandom.current().nextDouble(minRadius, maxRadius);
                Vector offset = new Vector(Math.cos(angle) * dist, 0.0, Math.sin(angle) * dist);
                Location base = playerLoc.clone().add(offset);
                Location spawn = resolveSurfaceSpawn(world, base, fallbackOrigin, grassOnly);
                if (spawn != null) {
                    return spawn;
                }
            }
            return null;
        }

        private Location resolveSurfaceSpawn(World world, Location base, Location fallbackOrigin, boolean grassOnly) {
            if (world == null || base == null || fallbackOrigin == null) {
                return null;
            }
            int y = world.getHighestBlockYAt(base);
            Material groundType = world.getBlockAt(base.getBlockX(), y, base.getBlockZ()).getType();
            if (!isAllowedSpawnGround(groundType, grassOnly)) {
                return null;
            }
            int spawnY = (int) Math.round(fallbackOrigin.getY());
            Location spawn = new Location(world, base.getX(), spawnY, base.getZ());
            Block below = world.getBlockAt(base.getBlockX(), spawnY - 1, base.getBlockZ());
            if (!below.getType().isSolid()) {
                return null;
            }
            if (spawn.getBlock().getType().isAir() && spawn.clone().add(0, 1, 0).getBlock().getType().isAir()) {
                return spawn;
            }
            return null;
        }

        private double randomAxisOffset() {
            double magnitude = MOB_RELOCATE_AXIS_OFFSET
                    + ThreadLocalRandom.current().nextDouble(-MOB_RELOCATE_AXIS_JITTER, MOB_RELOCATE_AXIS_JITTER);
            return ThreadLocalRandom.current().nextBoolean() ? magnitude : -magnitude;
        }

        private boolean isAllowedSpawnGround(Material groundType, boolean grassOnly) {
            if (groundType == null || !groundType.isSolid()) {
                return false;
            }
            if (grassOnly) {
                return groundType == Material.GRASS_BLOCK;
            }
            return !groundType.name().contains("LEAVES");
        }
    }

    private void loadPortalTemplateIfNeeded() {
        if (!strongholdExitPortalTemplate.isEmpty()) return;
        World sourceWorld = Bukkit.getWorld("flatland");
        if (sourceWorld == null) return;
        for (int x = PORTAL_SRC_MIN_X; x <= PORTAL_SRC_MAX_X; x++) {
            for (int y = PORTAL_SRC_MIN_Y; y <= PORTAL_SRC_MAX_Y; y++) {
                for (int z = PORTAL_SRC_MIN_Z; z <= PORTAL_SRC_MAX_Z; z++) {
                    Block block = sourceWorld.getBlockAt(x, y, z);
                    if (block.getType().isAir()) {
                        continue;
                    }
                    strongholdExitPortalTemplate.add(new PortalTemplateBlock(
                            x - PORTAL_SRC_MIN_X,
                            y - PORTAL_SRC_MIN_Y,
                            z - PORTAL_SRC_MIN_Z,
                            block.getBlockData().clone()));
                }
            }
        }
    }

    private void clearActiveStageResultDisplays() {
        for (org.bukkit.entity.TextDisplay display : new java.util.ArrayList<>(activeStageResultDisplays)) {
            if (display != null && !display.isDead()) display.remove();
        }
        activeStageResultDisplays.clear();
    }

    private void spawnFixedResultScreen(Location base, java.util.List<String> lines, String tag) {
        if (base == null || base.getWorld() == null || lines == null || lines.isEmpty()) return;
        double y = 0.0;
        for (String line : lines) {
            Location loc = base.clone().add(0, y, 0);
            org.bukkit.entity.TextDisplay display = (org.bukkit.entity.TextDisplay) base.getWorld().spawnEntity(loc, org.bukkit.entity.EntityType.TEXT_DISPLAY);
            display.setBillboard(org.bukkit.entity.Display.Billboard.FIXED);
            display.setText(line);
            display.setShadowRadius(0f);
            display.setShadowStrength(0f);
            display.setBackgroundColor(org.bukkit.Color.fromARGB(0,0,0,0));
            if (tag != null && !tag.isBlank()) display.addScoreboardTag(tag);
            activeStageResultDisplays.add(display);
            y -= 0.28;
        }
    }

    private void logResultPlacementDebug(String message) {
        if (message == null) return;
        plugin.getLogger().info("[Stronghold][ResultScreen] " + message);
    }

    private Location nearestPortalRatingMarker(Player player, Location reference) {
        if (player == null || activePortalRatingMarkers.isEmpty()) return null;
        Location from = reference != null ? reference : player.getLocation();
        Location best = null;
        double bestDist = Double.MAX_VALUE;
        for (Location marker : activePortalRatingMarkers) {
            if (marker == null || marker.getWorld() == null || from.getWorld() == null) continue;
            if (!marker.getWorld().getUID().equals(from.getWorld().getUID())) continue;
            double d = marker.distanceSquared(from);
            if (d < bestDist) {
                bestDist = d;
                best = marker;
            }
        }
        if (player != null) {
            logResultPlacementDebug("Nearest marker for " + player.getName() + ": "
                    + (best == null ? "none" : (best.getBlockX()+","+best.getBlockY()+","+best.getBlockZ()))
                    + " (markers=" + activePortalRatingMarkers.size() + ")");
        }
        return best;
    }

    private PlacedPortalBounds tryPlaceExitPortalNearPlayer(Player player) {
        Location center = player.getLocation();
        World world = center.getWorld();
        if (world == null || strongholdExitPortalTemplate.isEmpty()) return null;
        for (int radius = 4; radius <= 160; radius += 4) {
            for (int dx = -radius; dx <= radius; dx += 2) {
                for (int dz = -radius; dz <= radius; dz += 2) {
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue;
                    }
                    int sampleX = center.getBlockX() + dx;
                    int sampleZ = center.getBlockZ() + dz;
                    double distanceSq = (dx * dx) + (dz * dz);
                    if (distanceSq < (MIN_EXIT_PORTAL_DISTANCE * MIN_EXIT_PORTAL_DISTANCE)) {
                        continue;
                    }
                    Location anchor = new Location(world, sampleX, PORTAL_SRC_MIN_Y, sampleZ);
                    if (!canPlacePortalAt(anchor)) continue;
                    placePortalAt(anchor);
                    return new PlacedPortalBounds(anchor.getBlockX(), anchor.getBlockY(), anchor.getBlockZ(), PORTAL_SRC_MAX_X - PORTAL_SRC_MIN_X + 1, PORTAL_SRC_MAX_Y - PORTAL_SRC_MIN_Y + 1, PORTAL_SRC_MAX_Z - PORTAL_SRC_MIN_Z + 1);
                }
            }
        }
        return null;
    }

    private boolean canPlacePortalAt(Location anchor) {
        World world = anchor.getWorld();
        for (PortalTemplateBlock block : strongholdExitPortalTemplate) {
            Block target = world.getBlockAt(anchor.getBlockX() + block.dx, anchor.getBlockY() + block.dy, anchor.getBlockZ() + block.dz);
            Material current = target.getType();
            if (!current.isAir() && current != Material.SHORT_GRASS && current != Material.TALL_GRASS) return false;
        }
        return true;
    }

    private void placePortalAt(Location anchor) {
        World world = anchor.getWorld();
        activePortalRatingMarkers.clear();
        clearActiveStageResultDisplays();
        Map<Integer, List<PortalTemplateBlock>> byLayer = new java.util.TreeMap<>();
        List<PortalTemplateBlock> portalBlocks = new ArrayList<>();
        for (PortalTemplateBlock block : strongholdExitPortalTemplate) {
            if (block.data.getMaterial() == Material.NETHER_PORTAL) {
                portalBlocks.add(block);
                continue;
            }
            byLayer.computeIfAbsent(block.dy, ignored -> new ArrayList<>()).add(block);
        }
        List<List<PortalTemplateBlock>> layers = new ArrayList<>(byLayer.values());
        new BukkitRunnable() {
            int index = 0;
            @Override
            public void run() {
                if (world == null) {
                    cancel();
                    return;
                }
                if (index < layers.size()) {
                    for (PortalTemplateBlock block : layers.get(index)) {
                        Block target = world.getBlockAt(anchor.getBlockX() + block.dx, anchor.getBlockY() + block.dy, anchor.getBlockZ() + block.dz);
                        if (block.data.getMaterial() == Material.WHITE_WOOL) {
                            Location marker = target.getLocation().add(0.5, 1.0, 0.5);
                            boolean exists = activePortalRatingMarkers.stream().anyMatch(existing -> existing.distanceSquared(marker) < 0.01);
                            if (!exists && activePortalRatingMarkers.size() < 2) {
                                activePortalRatingMarkers.add(marker);
                                logResultPlacementDebug("Captured portal result marker at " + marker.getBlockX()+","+marker.getBlockY()+","+marker.getBlockZ());
                            }
                            target.setType(Material.AIR, false);
                            continue;
                        }
                        target.setBlockData(block.data, false);
                        world.spawnParticle(Particle.BLOCK, target.getLocation().add(0.5, 0.5, 0.5), 8, 0.2, 0.2, 0.2, 0.01, block.data);
                    }
                    world.playSound(anchor, org.bukkit.Sound.BLOCK_STONE_PLACE, 0.7f, 1.0f + (index * 0.03f));
                    world.playSound(anchor, org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.35f, 1.4f);
                    index++;
                    return;
                }
                for (PortalTemplateBlock block : portalBlocks) {
                    Block target = world.getBlockAt(anchor.getBlockX() + block.dx, anchor.getBlockY() + block.dy, anchor.getBlockZ() + block.dz);
                    target.setBlockData(block.data, false);
                }
                world.spawnParticle(Particle.PORTAL, anchor.clone().add(0.5, 1.2, 0.5), 80, 1.2, 1.0, 1.2, 0.08);
                world.playSound(anchor, org.bukkit.Sound.BLOCK_PORTAL_TRIGGER, 0.8f, 1.0f);
                world.playSound(anchor, org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.3f);
                cancel();
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }

    private record PortalTemplateBlock(int dx, int dy, int dz, org.bukkit.block.data.BlockData data) {}
    private record PlacedPortalBounds(int minX, int minY, int minZ, int width, int height, int depth) {
        private boolean contains(Location loc) {
            return loc.getBlockX() >= minX && loc.getBlockX() < minX + width
                    && loc.getBlockY() >= minY && loc.getBlockY() < minY + height
                    && loc.getBlockZ() >= minZ && loc.getBlockZ() < minZ + depth
                    && loc.getBlock().getType() == Material.NETHER_PORTAL;
        }

        private Location center(World world) {
            return new Location(
                    world,
                    minX + (width / 2.0),
                    minY + Math.max(1.0, (height / 2.0)),
                    minZ + (depth / 2.0));
        }

        private Location guideTarget(World world) {
            Location centered = center(world);
            return new Location(world, centered.getX(), minY + height + 1.25, centered.getZ());
        }
    }

    private boolean isLockedStrongholdDoor(Block block) {
        if (block == null || block.getType() == null) {
            return false;
        }
        String typeName = block.getType().name();
        return typeName.endsWith("_DOOR") && block.getBlockData() instanceof Openable;
    }

    private boolean isStrongholdKey(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return false;
        }
        String typeName = stack.getType().name();
        if (!"TRIAL_KEY".equals(typeName) && stack.getType() != Material.TRIPWIRE_HOOK) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }
        String plainName = ChatColor.stripColor(meta.getDisplayName());
        return plainName != null && STRONGHOLD_KEY_NAME.equalsIgnoreCase(plainName.trim());
    }

    private boolean consumeFirstMatchingItem(Player player, java.util.function.Predicate<ItemStack> matcher) {
        if (player == null || matcher == null) {
            return false;
        }
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack stack = contents[slot];
            if (!matcher.test(stack)) {
                continue;
            }
            if (stack.getAmount() <= 1) {
                player.getInventory().setItem(slot, null);
            } else {
                stack.setAmount(stack.getAmount() - 1);
                player.getInventory().setItem(slot, stack);
            }
            player.updateInventory();
            return true;
        }
        return false;
    }

    private String normalizeStorageItemLabel(String rawLabel) {
        if (rawLabel == null || rawLabel.isBlank()) {
            return "Unknown Item";
        }
        String label = rawLabel;
        int i = 0;
        while (i + 1 < label.length() && label.charAt(i) == ChatColor.COLOR_CHAR) {
            i += 2;
        }
        int ws = i;
        while (ws < label.length() && Character.isWhitespace(label.charAt(ws))) {
            ws++;
        }
        if (ws > i) {
            label = label.substring(0, i) + label.substring(ws);
        }
        return label.trim();
    }

    private Location consumeReturnLocation(UUID playerId, Location fallback) {
        Location stored = playerId == null ? null : returnLocations.remove(playerId);
        if (stored != null && stored.getWorld() != null) {
            return stored.clone();
        }
        if (fallback != null && fallback.getWorld() != null) {
            return fallback.clone();
        }
        World world = Bukkit.getWorld("world");
        return world == null ? null : world.getSpawnLocation();
    }

    private void openPendingResultsAfterTeleport(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        StrongholdResultsStorageGUI pending = pendingResultInventories.remove(player.getUniqueId());
        if (pending == null) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                openResultInventories.put(player.getUniqueId(), pending);
                pending.open(player);
            }
        }, 2L);
    }

    private boolean isResultsGuiTitle(String title) {
        if (title == null) {
            return false;
        }
        return ChatColor.stripColor(title).startsWith("Stronghold Results");
    }

    private void handleResultsGuiClose(Player player) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        StrongholdResultsStorageGUI resultsGui = openResultInventories.get(playerId);
        if (confirmedResultExit.remove(playerId)) {
            openResultInventories.remove(playerId);
            return;
        }
        if (resultsGui == null || !resultsGui.hasRemainingItems()) {
            openResultInventories.remove(playerId);
            return;
        }
        openResultInventories.put(playerId, resultsGui);
        Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(createResultsConfirmGui()));
    }

    private Inventory createResultsConfirmGui() {
        Inventory confirm = Bukkit.createInventory(null, 27, RESULTS_CONFIRM_GUI_TITLE);
        ItemStack confirmItem = GuiUtil.getNexoItem("check", ChatColor.GREEN + "Confirm Exit");
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        if (confirmMeta != null) {
            List<String> lore = new ArrayList<>();
            lore.addAll(TooltipUtil.bulletList("You still have items in the results GUI."));
            lore.addAll(TooltipUtil.bulletList("Are you sure you wanna exit?"));
            lore.addAll(TooltipUtil.clickInstructions("to salvage remaining items and exit", null));
            confirmMeta.setLore(lore);
            confirmItem.setItemMeta(confirmMeta);
        }
        confirm.setItem(11, confirmItem);
        confirm.setItem(15, GuiUtil.getNexoItem("cross", ChatColor.RED + "Cancel"));
        return confirm;
    }

    private void handleResultsConfirmClick(Player player, int rawSlot) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        StrongholdResultsStorageGUI results = openResultInventories.get(playerId);
        if (rawSlot == 11) {
            if (results != null) {
                results.salvageRemaining(player);
            }
            confirmedResultExit.add(playerId);
            openResultInventories.remove(playerId);
            player.closeInventory();
            return;
        }
        if (rawSlot == 15 && results != null) {
            Bukkit.getScheduler().runTask(plugin, () -> results.open(player));
        }
    }

    private static final class SurvivorState {
        private final PlayerClass originalClass;
        private final Map<String, Integer> ownedSpellRanks = new HashMap<>();
        private final Map<String, String> activeSpellByBase = new HashMap<>();
        private final Map<String, Integer> spellChargesByBase = new HashMap<>();
        private long lastMobilityChargeRefillAt;
        private final Map<String, Long> lastCastAtBySpell = new HashMap<>();
        private final Map<StatsManager.StatType, Integer> tempStatBonuses = new EnumMap<>(StatsManager.StatType.class);
        private BossBar progressBar;
        private int level = 1;
        private int xp = 0;
        private List<UpgradeChoice> pendingUpgrades = List.of();
        private boolean awaitingUpgradeSelection;
        private boolean skipNextUpgradeReopen;
        private boolean rerollAnimating;
        private int pendingUpgradeSelections;
        private String activeArchetypeBuff = "None";
        private boolean upgradePaused;
        private int cooldownUpgradeTier;
        private int keysCollected;
        private double damageTaken;
        private int doorsOpened;
        private int chestsOpened;
        private String lastStageRating;
        private int startingGems;
        private int maxGemsDuringRun;
        private final List<ItemStack> lootStash = new ArrayList<>();
        private ItemStack[] savedStorageContents = new ItemStack[0];
        private ItemStack[] savedArmorContents = new ItemStack[0];
        private ItemStack[] savedExtraContents = new ItemStack[0];
        private ItemStack savedOffHand;

        private SurvivorState(PlayerClass originalClass) {
            this.originalClass = originalClass == null ? PlayerClass.VILLAGER : originalClass;
        }
    }

    private enum UpgradeType {
        SPELL_UNLOCK,
        SPELL_UPGRADE,
        STAT,
        GLOBAL_COOLDOWN
    }

    private record UpgradeChoice(UpgradeType type,
                                 String displayName,
                                 String description,
                                 String baseSpellId,
                                 String resultSpellId,
                                 StatsManager.StatType statType,
                                 int statAmount) {
    }

    public record ScoreResult(int total, int objectives, int damage, int time, String rank) {}

    public record StageStatus(int stage, int wave, int enemiesRemaining, String archetypeBuff) {
    }

    public record StageProgress(int stage, int wave, int absoluteWave) {}
    public record StageScalingConfig(double stageHealthGrowth, double stageDamageGrowth, double waveHealthGrowth, double waveDamageGrowth, double waveSpeedGrowth) {}

    private static final class MobMotionState {
        private Location lastLocation;
        private long lastMovedAtMs;
        private long lastTeleportAtMs;

        private MobMotionState(Location lastLocation, long lastMovedAtMs, long lastTeleportAtMs) {
            this.lastLocation = lastLocation;
            this.lastMovedAtMs = lastMovedAtMs;
            this.lastTeleportAtMs = lastTeleportAtMs;
        }
    }
}
