package me.nakilex.levelplugin.tower;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.arena.instance.ArenaInstance;
import me.nakilex.levelplugin.arena.instance.ArenaInstanceManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Foundation for an infinite tower using the arena instance as the combat room.
 * Players progress floor-by-floor with a short time limit per stage and a brief
 * intermission between floors.
 */
public class TowerManager implements Listener, Runnable {

    private static final int NEXT_FLOOR_DELAY_SECONDS = 10;
    private static final int BASE_TIME_LIMIT = 75;

    private final Plugin plugin;
    private final ArenaInstanceManager arenaInstanceManager;
    private final Map<UUID, TowerRun> activeRuns = new HashMap<>();
    private final Map<UUID, Integer> playerStages = new HashMap<>();
    private BukkitTask ticker;
    private final Random random = new Random();

    public TowerManager(Plugin plugin, ArenaInstanceManager arenaInstanceManager) {
        this.plugin = plugin;
        this.arenaInstanceManager = arenaInstanceManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
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
        ArenaInstance instance = arenaInstanceManager.createInstance();
        if (instance == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "The arena room is unavailable right now.");
            return;
        }
        int stage = playerStages.getOrDefault(player.getUniqueId(), 1);
        TowerRun run = new TowerRun(player.getUniqueId(), instance, stage);
        activeRuns.put(player.getUniqueId(), run);
        teleportToArena(player, instance);
        startStage(player, run);
    }

    public void exit(Player player) {
        TowerRun run = activeRuns.remove(player.getUniqueId());
        if (run == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "You are not inside the tower.");
            return;
        }
        arenaInstanceManager.destroyInstance(run.instance());
        saveProgress(player.getUniqueId());
        Location fallback = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0).getSpawnLocation();
        if (fallback != null) {
            player.teleport(fallback);
        }
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO, "You leave the tower.");
    }

    private void teleportToArena(Player player, ArenaInstance instance) {
        player.teleport(instance.getFirstSpawn());
    }

    private void startStage(Player player, TowerRun run) {
        run.awaitingNext = false;
        run.mobs.clear();
        run.timeLimitSeconds = computeTimeLimit(run.stage);
        run.deadline = System.currentTimeMillis() + run.timeLimitSeconds * 1000L;
        spawnWave(run);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                ChatColor.YELLOW + "Floor " + run.stage + ChatColor.GRAY + " begins. Clear it before the timer expires!");
    }

    private void spawnWave(TowerRun run) {
        boolean boss = run.stage % 10 == 0;
        int mobCount = boss ? 1 : Math.min(6, 3 + run.stage / 3);
        Location center = run.instance().getFirstSpawn().clone().add(run.instance().getSecondSpawn()).multiply(0.5);
        for (int i = 0; i < mobCount; i++) {
            Location spawn = center.clone().add(randomOffset(3.5));
            LivingEntity entity = spawnRandomMob(spawn, boss);
            if (entity == null) {
                continue;
            }
            scaleAttributes(entity, run.stage, boss);
            run.mobs.add(entity.getUniqueId());
        }
    }

    private void scaleAttributes(LivingEntity entity, int stage, boolean boss) {
        double health = (30 + stage * 6) * (boss ? 2.5 : 1.0);
        double damage = (3 + stage * 0.8) * (boss ? 1.8 : 1.0);
        if (entity.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
            entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(health);
            entity.setHealth(health);
        }
        if (entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
            entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(damage);
        }
        entity.setCustomName(ChatColor.RED + "Floor " + stage + (boss ? " Boss" : ""));
        entity.setCustomNameVisible(true);
    }

    private LivingEntity spawnRandomMob(Location loc, boolean boss) {
        LivingEntity spawned;
        if (boss) {
            spawned = loc.getWorld().spawn(loc, Zombie.class, z -> z.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD)));
        } else {
            spawned = switch (random.nextInt(3)) {
                case 0 -> loc.getWorld().spawn(loc, Zombie.class);
                case 1 -> loc.getWorld().spawn(loc, Skeleton.class);
                default -> loc.getWorld().spawn(loc, Zombie.class);
            };
        }
        return spawned;
    }

    private Vector randomOffset(double radius) {
        double x = (random.nextDouble() - 0.5) * 2 * radius;
        double z = (random.nextDouble() - 0.5) * 2 * radius;
        return new Vector(x, 0, z);
    }

    private int computeTimeLimit(int stage) {
        return BASE_TIME_LIMIT + Math.min(90, stage * 3);
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
                arenaInstanceManager.destroyInstance(run.instance());
                iterator.remove();
                continue;
            }
            if (!run.awaitingNext && now > run.deadline) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "You ran out of time on this floor.");
                iterator.remove();
                arenaInstanceManager.destroyInstance(run.instance());
                exit(player);
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
                        rewardStageClear(player, clearedStage);
                        run.stage++;
                        playerStages.put(run.playerId, run.stage);
                        saveProgress(run.playerId);
                        run.awaitingNext = true;
                        run.nextStageAt = System.currentTimeMillis() + NEXT_FLOOR_DELAY_SECONDS * 1000L;
                        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                                "Floor cleared! Next floor begins shortly.");
                    }
                }
                break;
            }
        }
    }

    private void rewardStageClear(Player player, int stage) {
        int coins = 50 + stage * 10;
        Main.getInstance().getEconomyManager().addCoins(player.getUniqueId(), coins);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.REWARD,
                ChatColor.GOLD + "+" + coins + " coins" + ChatColor.GRAY + " for clearing floor " + stage + ".");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        TowerRun run = activeRuns.remove(id);
        if (run != null) {
            arenaInstanceManager.destroyInstance(run.instance());
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
    final ArenaInstance instance;
    int stage;
    long deadline;
    long nextStageAt;
    boolean awaitingNext;
    int timeLimitSeconds;
    final Set<UUID> mobs = new HashSet<>();

    TowerRun(UUID playerId, ArenaInstance instance, int stage) {
        this.playerId = playerId;
        this.instance = instance;
        this.stage = stage;
    }
}
