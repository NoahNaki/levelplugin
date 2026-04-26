package me.nakilex.levelplugin.stronghold.run;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.stronghold.StrongholdShrineManager;
import me.nakilex.levelplugin.stronghold.utils.StrongholdMobSpawnUtil;
import me.nakilex.levelplugin.utils.StrongholdWorldUtil;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import static me.nakilex.levelplugin.utils.ChatMessageUtil.send;

/**
 * Minimal first-pass Stronghold run driver:
 * starts auto wave spawning and random shrine placement for solo runs.
 */
public class StrongholdRunManager {
    private static final int SHRINES_PER_RUN = 1;
    private static final int FIRST_WAVE_DELAY_SECONDS = 3;
    private static final int WAVE_INTERVAL_SECONDS = 5;

    private final Main plugin;
    private final StrongholdShrineManager shrineManager;
    private final Map<UUID, ActiveRun> activeRuns = new HashMap<>();
    private final List<String> waveMobPool = List.of("goblin_warrior", "goblin_archer", "goblin_assassin");

    public StrongholdRunManager(Main plugin, StrongholdShrineManager shrineManager) {
        this.plugin = plugin;
        this.shrineManager = shrineManager;
    }

    public void startSoloRun(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        World world = player.getWorld();
        if (!StrongholdWorldUtil.isStrongholdWorld(world)) {
            return;
        }
        UUID worldId = world.getUID();
        stopRun(worldId);

        Location origin = player.getLocation().clone();
        int shrines = shrineManager.spawnRandomShrines(origin, SHRINES_PER_RUN, 72, 250.0);
        if (shrines > 0) {
            send(player, MessageType.INFO, "Placed " + ChatColor.WHITE + shrines + ChatColor.GRAY + " shrine(s) around the stronghold.");
        }

        ActiveRun run = new ActiveRun(worldId, origin);
        activeRuns.put(worldId, run);
        run.start();
        send(player, MessageType.SUCCESS, "Stronghold waves started.");
    }

    public void stopAll() {
        for (UUID worldId : new ArrayList<>(activeRuns.keySet())) {
            stopRun(worldId);
        }
    }

    private void stopRun(UUID worldId) {
        ActiveRun existing = activeRuns.remove(worldId);
        if (existing != null) {
            existing.stop();
        }
    }

    private final class ActiveRun {
        private final UUID worldId;
        private final Location origin;
        private final List<UUID> spawned = new ArrayList<>();

        private BukkitTask task;
        private int wave = 0;
        private int secondsUntilNextWave = FIRST_WAVE_DELAY_SECONDS;

        private ActiveRun(UUID worldId, Location origin) {
            this.worldId = worldId;
            this.origin = origin;
        }

        private void start() {
            this.task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
                World world = plugin.getServer().getWorld(worldId);
                if (world == null || !StrongholdWorldUtil.isStrongholdWorld(world)) {
                    stopRun(worldId);
                    return;
                }
                if (secondsUntilNextWave > 0) {
                    secondsUntilNextWave--;
                    return;
                }
                secondsUntilNextWave = WAVE_INTERVAL_SECONDS;
                wave++;
                spawnWave(world, wave);
            }, 20L, 20L);
        }

        private void stop() {
            if (task != null) {
                task.cancel();
                task = null;
            }
            for (UUID id : spawned) {
                var e = plugin.getServer().getEntity(id);
                if (e instanceof LivingEntity living && !living.isDead()) {
                    living.remove();
                }
            }
            spawned.clear();
        }

        private void spawnWave(World world, int waveNumber) {
            List<Player> players = world.getPlayers().stream().filter(Player::isOnline).toList();
            if (players.isEmpty()) {
                return;
            }
            int spawnCount = Math.min(10, 2 + waveNumber);
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
                spawned.add(mob.getUniqueId());
                if (mob instanceof Mob hostile) {
                    hostile.setTarget(target);
                }
                world.spawnParticle(Particle.SMOKE, spawn, 10, 0.2, 0.2, 0.2, 0.01);
            }
            for (Player player : players) {
                send(player, MessageType.INFO, "Wave " + ChatColor.WHITE + waveNumber + ChatColor.GRAY + " started.");
            }
        }

        private Location findSpawnNear(Location playerLoc, Location fallbackOrigin, double minRadius, double maxRadius) {
            World world = playerLoc.getWorld();
            if (world == null) {
                return null;
            }
            for (int attempt = 0; attempt < 16; attempt++) {
                double angle = ThreadLocalRandom.current().nextDouble(0, Math.PI * 2);
                double dist = ThreadLocalRandom.current().nextDouble(minRadius, maxRadius);
                Vector offset = new Vector(Math.cos(angle) * dist, 0.0, Math.sin(angle) * dist);
                Location base = playerLoc.clone().add(offset);
                int y = world.getHighestBlockYAt(base);
                Location spawn = new Location(world, base.getX(), Math.max(y + 1, fallbackOrigin.getY()), base.getZ());
                if (spawn.getBlock().getType().isAir() && spawn.clone().add(0, 1, 0).getBlock().getType().isAir()) {
                    return spawn;
                }
            }
            return null;
        }
    }
}
