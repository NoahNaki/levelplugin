package me.nakilex.levelplugin.tower;

import io.lumine.mythic.core.mobs.ActiveMob;
import me.nakilex.levelplugin.dungeon.Dungeon;
import me.nakilex.levelplugin.dungeon.DungeonManager;
import me.nakilex.levelplugin.dungeon.RoomTemplate;
import me.nakilex.levelplugin.mob.config.MobRewardsConfig;
import me.nakilex.levelplugin.mob.utils.MobNameUtil;
import me.nakilex.levelplugin.mob.utils.MythicMobModifier;
import me.nakilex.levelplugin.utils.FileUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Foundation for an infinite tower that chains together dungeon templates into
 * combat arenas and lobby intermissions. Players clear waves of MythicMobs per
 * floor, visit a lobby for breathing room, and then advance deeper into the
 * catacombs.
 */
public class TowerManager implements Listener, Runnable {

    private static final int NEXT_FLOOR_DELAY_SECONDS = 10;
    private static final int BASE_TIME_LIMIT = 75;

    private final Plugin plugin;
    private final DungeonManager dungeonManager;
    private final Map<UUID, TowerRun> activeRuns = new HashMap<>();
    private final Map<UUID, Integer> playerStages = new HashMap<>();
    private BukkitTask ticker;
    private final Random random = new Random();
    private final List<MobTemplate> mobPool = new ArrayList<>();
    private final List<MobTemplate> bossPool = new ArrayList<>();
    private int maxMobTier = 1;
    private int maxBossTier = 1;

    private final MobRewardsConfig mobRewardsConfig;

    public TowerManager(Plugin plugin, DungeonManager dungeonManager, MobRewardsConfig mobRewardsConfig) {
        this.plugin = plugin;
        this.dungeonManager = dungeonManager;
        this.mobRewardsConfig = mobRewardsConfig;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        loadMobPools();
        startTicker();
    }

    public void startTicker() {
        stopTicker();
        ticker = plugin.getServer().getScheduler().runTaskTimer(plugin, this, 20L, 20L);
    }

    public void stopTicker() {
        if (ticker != null) {
            ticker.cancel();
            ticker = null;
        }
    }

    public boolean isInRun(UUID playerId) {
        return activeRuns.containsKey(playerId);
    }

    public int getTrackedStage(UUID playerId) {
        return Math.max(1, playerStages.getOrDefault(playerId, 1));
    }

    public TowerStatus getStatus(UUID playerId) {
        TowerRun run = activeRuns.get(playerId);
        if (run == null) return null;
        long now = System.currentTimeMillis();
        long remaining = Math.max(0, (run.awaitingNext ? run.nextStageAt : run.deadline) - now);
        int remainingSeconds = (int) Math.ceil(remaining / 1000.0);
        long nextIn = run.awaitingNext ? Math.max(0, (run.nextStageAt - now) / 1000) : 0;
        return new TowerStatus(run.stage, remainingSeconds, nextIn, run.mobs.size(), run.timeLimitSeconds, run.awaitingNext);
    }

    public void enter(Player player) {
        if (isInRun(player.getUniqueId())) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "You are already in the tower.");
            return;
        }
        TowerRun run = createRun(player.getUniqueId());
        if (run == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "The tower realm failed to generate. Please try again.");
            return;
        }
        int stage = playerStages.getOrDefault(player.getUniqueId(), 1);
        run.stage = stage;
        activeRuns.put(player.getUniqueId(), run);
        teleportToArena(player, run);
        startStage(player, run);
    }

    private TowerRun createRun(UUID playerId) {
        String worldName = "tower_" + playerId.toString().substring(0, 8) + "_" + System.currentTimeMillis();
        org.bukkit.World world = dungeonManager.createVoidWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("[TowerDebug] Failed to create tower world for " + playerId);
            return null;
        }
        Dungeon dungeon = new Dungeon(world, worldName);
        return new TowerRun(playerId, dungeon, world, dungeonManager.getStep());
    }

    public void exit(Player player) {
        exit(player, null, false);
    }

    private void exit(Player player, TowerRun run, boolean timedOut) {
        UUID playerId = player != null ? player.getUniqueId() : (run != null ? run.playerId : null);
        TowerRun current = playerId != null ? activeRuns.remove(playerId) : null;
        if (current == null) {
            current = run;
        }
        if (current == null) {
            if (player != null) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "You are not inside the tower.");
            }
            return;
        }
        saveProgress(current.playerId);
        Location fallback = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0).getSpawnLocation();
        if (fallback != null && player != null && player.isOnline()) {
            player.teleport(fallback);
        }
        destroyRunWorld(current);
        if (!timedOut && player != null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO, "You leave the tower.");
        }
    }

    private void destroyRunWorld(TowerRun run) {
        if (run == null || run.world == null) return;
        Bukkit.unloadWorld(run.world, false);
        File folder = new File(plugin.getServer().getWorldContainer(), run.world.getName());
        FileUtil.deleteDirectory(folder);
    }

    private void teleportToArena(Player player, TowerRun run) {
        ensureRooms(run, run.stage, isBossStage(run.stage));
        Location spawn = run.stageCenters.getOrDefault(run.stage, run.origin.clone());
        player.teleport(spawn.clone().add(0.5, 1.5, 0.5));
    }

    private void startStage(Player player, TowerRun run) {
        run.awaitingNext = false;
        run.mobs.clear();
        run.timeLimitSeconds = computeTimeLimit(run.stage);
        run.deadline = System.currentTimeMillis() + run.timeLimitSeconds * 1000L;
        boolean boss = isBossStage(run.stage);
        ensureRooms(run, run.stage, boss);
        spawnWave(run, boss);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                ChatColor.YELLOW + "Floor " + run.stage + (boss ? ChatColor.DARK_RED + " (Boss)" : "")
                        + ChatColor.GRAY + " started. Clear all Mythic mobs before the timer expires.");
    }

    private void spawnWave(TowerRun run, boolean boss) {
        int mobCount = boss ? 1 : Math.min(6, 3 + run.stage / 3);
        Location center = run.stageCenters.getOrDefault(run.stage, run.origin).clone();
        MobTemplate template = chooseTemplate(boss, run.stage);
        if (template == null) {
            Player player = Bukkit.getPlayer(run.playerId);
            if (player != null) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                        "No valid MythicMobs are configured for the tower. Please contact staff.");
            }
            plugin.getLogger().warning("[TowerDebug] No mob template found for boss=" + boss + " stage=" + run.stage
                    + " mobPool=" + mobPool.size() + " bossPool=" + bossPool.size());
            exit(player, run, true);
            return;
        }

        plugin.getLogger().info(String.format(Locale.US,
                "[TowerDebug] Spawning wave stage=%d boss=%s mobs=%d template=%s center=%s",
                run.stage, boss, mobCount, template.mobId(), formatLoc(center)));

        for (int i = 0; i < mobCount; i++) {
            Location spawn = center.clone().add(randomOffset(3.5));
            ActiveMob mob = spawnModifiedMob(template, spawn, run.stage, boss);
            if (mob != null && mob.getEntity() != null) {
                run.mobs.add(mob.getEntity().getUniqueId());
            } else {
                plugin.getLogger().warning(String.format(Locale.US,
                        "[TowerDebug] Failed to spawn Mythic mob id=%s at %s (stage=%d)",
                        template.mobId(), formatLoc(spawn), run.stage));
            }
        }
    }

    private Vector randomOffset(double radius) {
        double x = (random.nextDouble() - 0.5) * 2 * radius;
        double z = (random.nextDouble() - 0.5) * 2 * radius;
        return new Vector(x, 0, z);
    }

    private int computeTimeLimit(int stage) {
        return BASE_TIME_LIMIT + Math.min(90, stage * 3);
    }

    private boolean isBossStage(int stage) {
        return stage > 0 && stage % 10 == 0;
    }

    private void ensureRooms(TowerRun run, int stage, boolean boss) {
        if (run.stageCenters.containsKey(stage)) return;

        int offset = (stage - 1) * (run.spacing * 2);
        Location combatCenter = run.origin.clone().add(offset, 0, 0);
        RoomTemplate combatTemplate = boss ? dungeonManager.getBoss() : pickCombatTemplate();
        RoomTemplate lobbyTemplate = pickLobbyTemplate();
        int combatRotation = random.nextInt(4);
        int lobbyRotation = random.nextInt(4);

        if (combatTemplate != null) {
            var result = dungeonManager.pasteRoom(run.dungeon, combatTemplate, combatRotation, combatCenter, null, true);
            if (!result.success()) {
                plugin.getLogger().warning(String.format(Locale.US,
                        "[TowerDebug] Combat room overlap=%.3f prevented paste for stage=%d at %s", result.overlap(), stage,
                        formatLoc(combatCenter)));
            }
            dungeonManager.pasteRoom(run.dungeon, combatTemplate, combatRotation, combatCenter, null, false);
            run.stageCenters.put(stage, combatCenter);
            plugin.getLogger().info(String.format(Locale.US,
                    "[TowerDebug] Built combat room template=%s rotation=%d at %s for stage=%d", identify(combatTemplate),
                    combatRotation, formatLoc(combatCenter), stage));
        } else {
            plugin.getLogger().warning("[TowerDebug] Missing combat template for stage " + stage);
        }

        if (lobbyTemplate != null) {
            Location lobbyCenter = combatCenter.clone().add(run.spacing, 0, 0);
            var result = dungeonManager.pasteRoom(run.dungeon, lobbyTemplate, lobbyRotation, lobbyCenter, null, true);
            if (!result.success()) {
                plugin.getLogger().warning(String.format(Locale.US,
                        "[TowerDebug] Lobby room overlap=%.3f prevented paste for stage=%d at %s", result.overlap(), stage,
                        formatLoc(lobbyCenter)));
            }
            dungeonManager.pasteRoom(run.dungeon, lobbyTemplate, lobbyRotation, lobbyCenter, null, false);
            run.lobbyCenters.put(stage, lobbyCenter);
            plugin.getLogger().info(String.format(Locale.US,
                    "[TowerDebug] Built lobby room template=%s rotation=%d at %s for stage=%d", identify(lobbyTemplate),
                    lobbyRotation, formatLoc(lobbyCenter), stage));
        } else {
            plugin.getLogger().warning("[TowerDebug] Missing lobby template for stage " + stage);
        }
    }

    private ActiveMob spawnModifiedMob(MobTemplate template, Location loc, int stage, boolean boss) {
        double tierScale = 1.0 + (template.tier() - 1) * 0.35;
        double stageScale = 1.0 + stage * 0.12;
        double health = (boss ? 240 : 110) * tierScale * stageScale * (boss ? 1.8 : 1.0);
        double damage = (boss ? 18 : 8) * tierScale * (1 + stage * 0.08) * (boss ? 1.75 : 1.0);

        ActiveMob mob = MythicMobModifier.spawnModifiedMob(template.mobId(), loc, health, damage, null, null);
        if (mob != null && mob.getEntity() != null) {
            LivingEntity entity = (LivingEntity) mob.getEntity().getBukkitEntity();
            String name = MobNameUtil.getDisplayName(template.mobId());
            entity.setCustomName(ChatColor.RED + name + ChatColor.GRAY + " [F" + stage + (boss ? " Boss" : "") + "]");
            entity.setCustomNameVisible(true);
        }
        return mob;
    }

    private MobTemplate chooseTemplate(boolean boss, int stage) {
        List<MobTemplate> pool = boss ? bossPool : mobPool;
        if (pool.isEmpty()) return null;

        int maxTier = boss ? maxBossTier : maxMobTier;
        int targetTier = Math.min(maxTier, Math.max(1, (stage + 2) / 3));

        double totalWeight = 0.0;
        List<Double> weights = new ArrayList<>(pool.size());
        for (MobTemplate template : pool) {
            int distance = Math.abs(template.tier() - targetTier);
            double weight = 1.0 / (1 + distance);
            weights.add(weight);
            totalWeight += weight;
        }

        double roll = random.nextDouble() * totalWeight;
        double cursor = 0.0;
        for (int i = 0; i < pool.size(); i++) {
            cursor += weights.get(i);
            if (roll <= cursor) {
                return pool.get(i);
            }
        }
        return pool.get(pool.size() - 1);
    }

    private RoomTemplate pickCombatTemplate() {
        List<RoomTemplate> options = new ArrayList<>();
        if (dungeonManager.getCombatLeft() != null) options.add(dungeonManager.getCombatLeft());
        if (dungeonManager.getCombatRight() != null) options.add(dungeonManager.getCombatRight());
        if (dungeonManager.getHallway() != null) options.add(dungeonManager.getHallway());
        if (dungeonManager.getLibrary() != null) options.add(dungeonManager.getLibrary());
        return options.isEmpty() ? null : options.get(random.nextInt(options.size()));
    }

    private RoomTemplate pickLobbyTemplate() {
        List<RoomTemplate> options = new ArrayList<>();
        if (dungeonManager.getLibrary() != null) options.add(dungeonManager.getLibrary());
        if (dungeonManager.getTreasureLeft() != null) options.add(dungeonManager.getTreasureLeft());
        if (dungeonManager.getTreasureTRight() != null) options.add(dungeonManager.getTreasureTRight());
        if (dungeonManager.getDecorChest() != null) options.add(dungeonManager.getDecorChest());
        if (dungeonManager.getDecorStone() != null) options.add(dungeonManager.getDecorStone());
        return options.isEmpty() ? null : options.get(random.nextInt(options.size()));
    }

    private String identify(RoomTemplate template) {
        if (template == null) return "unknown";
        return dungeonManager.identifyTemplate(template).name();
    }

    private String formatLoc(Location loc) {
        return String.format(Locale.US, "%s:(%.1f,%.1f,%.1f)",
                loc.getWorld() != null ? loc.getWorld().getName() : "null",
                loc.getX(), loc.getY(), loc.getZ());
    }

    private void loadMobPools() {
        mobPool.clear();
        bossPool.clear();
        maxMobTier = 1;
        maxBossTier = 1;

        if (mobRewardsConfig != null) {
            ConfigurationSection section = mobRewardsConfig.getConfig().getConfigurationSection("mobs");
            if (section != null) {
                for (String key : section.getKeys(false)) {
                    int tier = section.getInt(key + ".tier", 1);
                    mobPool.add(new MobTemplate(key, tier));
                    maxMobTier = Math.max(maxMobTier, tier);
                }
                plugin.getLogger().info("[TowerDebug] Loaded " + mobPool.size() + " tower mobs from mob_rewards.yml");
            }
        }

        File bossFile = new File(plugin.getDataFolder(), "field_bosses.yml");
        FileConfiguration bossCfg = YamlConfiguration.loadConfiguration(bossFile);
        ConfigurationSection bossSection = bossCfg.getConfigurationSection("mobs");
        if (bossSection != null) {
            for (String key : bossSection.getKeys(false)) {
                int tier = bossSection.getInt(key + ".tier", 1);
                bossPool.add(new MobTemplate(key, tier));
                maxBossTier = Math.max(maxBossTier, tier);
            }
            plugin.getLogger().info("[TowerDebug] Loaded " + bossPool.size() + " tower bosses from field_bosses.yml");
        }
    }

    public void reloadMobPools() {
        loadMobPools();
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, TowerRun>> iterator = activeRuns.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, TowerRun> entry = iterator.next();
            UUID playerId = entry.getKey();
            TowerRun run = entry.getValue();
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                destroyRunWorld(run);
                iterator.remove();
                continue;
            }
            if (!run.awaitingNext && now > run.deadline) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "You ran out of time on this floor.");
                iterator.remove();
                exit(player, run, true);
                continue;
            }
            if (run.awaitingNext && now >= run.nextStageAt) {
                startStage(player, run);
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        UUID entityId = entity.getUniqueId();
        for (TowerRun run : activeRuns.values()) {
            if (run.mobs.remove(entityId)) {
                if (run.mobs.isEmpty()) {
                    Player player = Bukkit.getPlayer(run.playerId);
                    if (player != null) {
                        int clearedStage = run.stage;
                        rewardStageClear(player, clearedStage, isBossStage(clearedStage));
                        ensureRooms(run, clearedStage, isBossStage(clearedStage));
                        Location lobby = run.lobbyCenters.getOrDefault(clearedStage,
                                run.stageCenters.getOrDefault(clearedStage, run.origin));
                        run.stage++;
                        playerStages.put(run.playerId, run.stage);
                        saveProgress(run.playerId);
                        run.awaitingNext = true;
                        run.nextStageAt = System.currentTimeMillis() + NEXT_FLOOR_DELAY_SECONDS * 1000L;
                        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                                "Floor cleared! Catch your breath in the lobby or type /tower exit.");
                        player.teleport(lobby.clone().add(0.5, 1.5, 0.5));
                        plugin.getLogger().info(String.format(Locale.US,
                                "[TowerDebug] Player %s cleared stage=%d (next=%d) lobby=%s",
                                player.getName(), clearedStage, run.stage, formatLoc(lobby)));
                    }
                }
                break;
            }
        }
    }

    private void rewardStageClear(Player player, int stage, boolean boss) {
        int sigils = (int) Math.round((12 + stage * 2.5) * (boss ? 2.5 : 1.0));
        Main.getInstance().getSigilManager().addUnits(player, sigils);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.REWARD,
                ChatColor.AQUA + "+" + sigils + " Soul Sigils" + ChatColor.GRAY + " for clearing floor "
                        + stage + (boss ? ChatColor.DARK_RED + " (Boss)" + ChatColor.GRAY + "." : ChatColor.GRAY + "."));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        TowerRun run = activeRuns.remove(id);
        if (run != null) {
            destroyRunWorld(run);
        }
        saveProgress(id);
    }

    @org.bukkit.event.EventHandler
    public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        loadProgress(event.getPlayer().getUniqueId());
    }

    public void loadProgress(UUID playerId) {
        File file = getProgressFile(playerId);
        if (!file.exists()) {
            playerStages.put(playerId, 1);
            return;
        }
        FileConfiguration data = YamlConfiguration.loadConfiguration(file);
        int stage = data.getInt("stage", 1);
        playerStages.put(playerId, Math.max(1, stage));
    }

    public void saveProgress(UUID playerId) {
        File file = getProgressFile(playerId);
        FileConfiguration data = new YamlConfiguration();
        data.set("stage", playerStages.getOrDefault(playerId, 1));
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save tower progress for " + playerId + ": " + e.getMessage());
        }
    }

    private File getProgressFile(UUID playerId) {
        File folder = new File(plugin.getDataFolder(), "tower");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return new File(folder, "player_" + playerId + ".yml");
    }
}

class TowerRun {
    final UUID playerId;
    final Dungeon dungeon;
    final org.bukkit.World world;
    final Location origin;
    final int spacing;
    int stage;
    long deadline;
    long nextStageAt;
    boolean awaitingNext;
    int timeLimitSeconds;
    final Set<UUID> mobs = new HashSet<>();
    final Map<Integer, Location> stageCenters = new HashMap<>();
    final Map<Integer, Location> lobbyCenters = new HashMap<>();

    TowerRun(UUID playerId, Dungeon dungeon, org.bukkit.World world, int spacing) {
        this.playerId = playerId;
        this.dungeon = dungeon;
        this.world = world;
        this.spacing = spacing;
        this.origin = new Location(world, 0, 64, 0);
    }
}

record MobTemplate(String mobId, int tier) {
}
