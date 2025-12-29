package me.nakilex.levelplugin.pathfinding;

import me.nakilex.levelplugin.dungeon.DungeonManager;
import me.nakilex.levelplugin.dungeon.verified.CrimsonReliquaryDungeon;
import me.nakilex.levelplugin.pathfinding.npc.ArcherMercenary;
import me.nakilex.levelplugin.pathfinding.npc.MageMercenary;
import me.nakilex.levelplugin.pathfinding.npc.PathNpc;
import me.nakilex.levelplugin.pathfinding.npc.RogueMercenary;
import me.nakilex.levelplugin.pathfinding.npc.WarriorMercenary;
import me.nakilex.levelplugin.mercenary.MercenaryAffinityManager;
import me.nakilex.levelplugin.mercenary.MercenaryRole;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import me.nakilex.levelplugin.waypoints.bukkit.BukkitPathfindingService;
import me.nakilex.levelplugin.waypoints.bukkit.PathLocationUtils;
import me.nakilex.levelplugin.waypoints.engine.result.PathUtils;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class DungeonExpeditionManager implements Listener {
    private static final String RELIQUARY_KEY = "crimson reliquary";
    private static final double PATH_INTERPOLATION_STEP = 0.35;
    private static final double PATH_POINT_DISTANCE = 20.0;
    private final Plugin plugin;
    private final DungeonManager dungeonManager;
    private final BukkitPathfindingService pathfindingService = new BukkitPathfindingService();
    private final MercenaryAffinityManager affinityManager;
    private final Map<UUID, ExpeditionRun> activeRuns = new HashMap<>();

    public DungeonExpeditionManager(Plugin plugin, DungeonManager dungeonManager, MercenaryAffinityManager affinityManager) {
        this.plugin = plugin;
        this.dungeonManager = dungeonManager;
        this.affinityManager = affinityManager;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void startCrimsonReliquaryExpedition(Player player) {
        if (player == null) {
            return;
        }
        if (activeRuns.containsKey(player.getUniqueId())) {
            ChatMessageUtil.send(player, MessageType.WARNING, "A dungeon expedition is already running.");
            return;
        }

        CrimsonReliquaryDungeon reliquary = resolveReliquary();
        if (reliquary == null) {
            ChatMessageUtil.send(player, MessageType.ERROR, "Crimson Reliquary is not registered.");
            return;
        }

        dungeonManager.startInstance(player, reliquary.getKey());
        ChatMessageUtil.send(player, MessageType.INFO, "Starting Crimson Reliquary expedition...");
        plugin.getLogger().info("[DungeonExpedition] Requested expedition for "
                + player.getName() + " layout=" + reliquary.getKey());

        new BukkitRunnable() {
            int attempts = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                attempts++;
                World world = player.getWorld();
                if (!dungeonManager.isInstanceWorld(world)) {
                    return;
                }
                String layout = dungeonManager.getInstanceLayout(world);
                if (layout == null || !layout.equalsIgnoreCase(reliquary.getKey())) {
                    return;
                }
                var routeOpt = reliquary.getExpeditionRoute(world);
                if (routeOpt.isEmpty()) {
                    if (attempts % 20 == 0) {
                        plugin.getLogger().info("[DungeonExpedition] Waiting for instance to finish loading...");
                    }
                    return;
                }
                plugin.getLogger().info("[DungeonExpedition] Route ready: points=" + routeOpt.get().path().size()
                        + " spawn=" + formatLocation(routeOpt.get().spawn()));
                spawnMercenaries(player, routeOpt.get());
                cancel();
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void spawnMercenaries(Player player, CrimsonReliquaryDungeon.ExpeditionRoute route) {
        if (route.path().size() < 2) {
            ChatMessageUtil.send(player, MessageType.ERROR, "Expedition route is missing path markers.");
            return;
        }

        int npcId = resolveRandomMercenaryId();
        PathNpc profile = resolveRandomProfile(npcId);
        int gearScore = affinityManager == null ? 0 : affinityManager.getGearScore(npcId);
        if (gearScore <= 0) {
            gearScore = 1000;
        }

        List<PathFollower> followers = new ArrayList<>();
        List<PathDebug> debugPaths = new ArrayList<>();
        List<Particle.DustOptions> colors = List.of(
                new Particle.DustOptions(Color.fromRGB(77, 197, 255), 1.2f),
                new Particle.DustOptions(Color.fromRGB(231, 120, 255), 1.2f),
                new Particle.DustOptions(Color.fromRGB(117, 255, 153), 1.2f),
                new Particle.DustOptions(Color.fromRGB(255, 198, 92), 1.2f)
        );
        Location start = route.spawn().clone();
        Location target = route.path().size() > 1 ? route.path().get(1) : route.spawn();
        List<Location> path = buildPathfindingRoute(start, target, route.path());
        NPC npc = spawnMercenaryNpc(npcId, profile, start);
        forceLoadTargetChunk(path);
        PathFollower follower = new PathFollower(plugin, npc, path, profile, false, null, gearScore);
        follower.start();
        followers.add(follower);
        debugPaths.add(new PathDebug(path, colors.get(0)));
        plugin.getLogger().info("[DungeonExpedition] Spawned mercenary " + profile.name()
                + " npcId=" + npcId
                + " gearScore=" + gearScore
                + " pathPoints=" + path.size()
                + " start=" + formatLocation(path.get(0)));
        if (path.size() > 1) {
            plugin.getLogger().info("[DungeonExpedition] " + profile.name() + " point1="
                    + formatLocation(path.get(1)));
        }

        BukkitTask particleTask = startPathDebugParticles(player.getUniqueId(), debugPaths);
        activeRuns.put(player.getUniqueId(), new ExpeditionRun(route.spawn().getWorld(), followers, particleTask));
        ChatMessageUtil.send(player, MessageType.SUCCESS, "Mercenaries deployed. Showing their path trails.");
    }

    private List<Location> buildPathfindingRoute(Location start, Location target, List<Location> fallbackPath) {
        if (start == null || target == null) {
            return fallbackPath == null ? List.of() : fallbackPath;
        }
        return pathfindingService.findPath(start, target)
                .map(path -> PathUtils.interpolate(path, PATH_INTERPOLATION_STEP))
                .map(path -> PathLocationUtils.toLocations(start.getWorld(), path, 0.0, true, 0))
                .map(points -> PathLocationUtils.downsampleByDistance(points, PATH_POINT_DISTANCE))
                .filter(list -> !list.isEmpty())
                .orElseGet(() -> fallbackPath == null ? List.of() : fallbackPath);
    }

    private CrimsonReliquaryDungeon resolveReliquary() {
        return dungeonManager.getVerifiedDefinition(RELIQUARY_KEY) instanceof CrimsonReliquaryDungeon reliquary
                ? reliquary
                : null;
    }

    @EventHandler
    public void onBossDeath(EntityDeathEvent event) {
        if (!event.getEntity().getScoreboardTags().contains("dungeon_boss")) {
            return;
        }
        World world = event.getEntity().getWorld();
        activeRuns.entrySet().removeIf(entry -> {
            ExpeditionRun run = entry.getValue();
            if (!run.world.equals(world)) {
                return false;
            }
            run.cleanup();
            return true;
        });
    }

    private BukkitTask startPathDebugParticles(UUID playerId, List<PathDebug> paths) {
        return new BukkitRunnable() {
            @Override
            public void run() {
                Player player = Bukkit.getPlayer(playerId);
                if (player == null || !player.isOnline()) {
                    cancel();
                    return;
                }
                for (PathDebug debug : paths) {
                    spawnPathParticles(player, debug);
                }
            }
        }.runTaskTimer(plugin, 10L, 20L);
    }

    private void spawnPathParticles(Player player, PathDebug debug) {
        int stride = 2;
        int count = 1;
        for (int i = 0; i < debug.path().size(); i += stride) {
            Location point = debug.path().get(i);
            player.spawnParticle(Particle.DUST, point, count, 0, 0, 0, 0, debug.dust());
        }
    }

    private record PathDebug(List<Location> path, Particle.DustOptions dust) {
    }

    private int resolveRandomMercenaryId() {
        if (affinityManager == null || affinityManager.getMercenaryIds().isEmpty()) {
            return -1;
        }
        List<Integer> ids = new ArrayList<>(affinityManager.getMercenaryIds());
        return ids.get(ThreadLocalRandom.current().nextInt(ids.size()));
    }

    private PathNpc resolveRandomProfile(int npcId) {
        if (affinityManager == null || npcId <= 0) {
            return new WarriorMercenary();
        }
        MercenaryRole role = affinityManager.getRole(npcId);
        return switch (role) {
            case SUPPORT -> new MageMercenary();
            case TANK -> new WarriorMercenary();
            case DPS -> ThreadLocalRandom.current().nextBoolean() ? new RogueMercenary() : new ArcherMercenary();
        };
    }

    private NPC spawnMercenaryNpc(int npcId, PathNpc profile, Location start) {
        NPC template = npcId > 0 ? CitizensAPI.getNPCRegistry().getById(npcId) : null;
        NPC npc = template != null ? template.copy() : CitizensAPI.getNPCRegistry().createNPC(profile.type(), profile.name());
        npc.setBukkitEntityType(profile.type());
        npc.spawn(start);
        npc.setProtected(false);
        return npc;
    }

    private void forceLoadTargetChunk(List<Location> path) {
        if (path == null || path.size() < 2) {
            return;
        }
        Location target = path.get(1);
        if (target == null || target.getWorld() == null) {
            return;
        }
        var chunk = target.getChunk();
        if (!chunk.isLoaded()) {
            chunk.load(true);
        }
        chunk.addPluginChunkTicket(plugin);
        chunk.setForceLoaded(true);
        plugin.getLogger().info("[DungeonExpedition] Preloaded point1 chunk "
                + chunk.getX() + "," + chunk.getZ() + " for " + formatLocation(target));
    }

    private String formatLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return "null";
        }
        return location.getWorld().getName() + ":"
                + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    private static final class ExpeditionRun {
        private final World world;
        private final List<PathFollower> followers;
        private final BukkitTask particleTask;

        private ExpeditionRun(World world, List<PathFollower> followers, BukkitTask particleTask) {
            this.world = world;
            this.followers = followers;
            this.particleTask = particleTask;
        }

        private void cleanup() {
            if (particleTask != null) {
                particleTask.cancel();
            }
            for (PathFollower follower : followers) {
                follower.stop();
            }
        }
    }
}
