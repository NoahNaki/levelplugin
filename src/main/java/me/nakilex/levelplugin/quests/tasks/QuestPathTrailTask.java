package me.nakilex.levelplugin.quests.tasks;

import de.bsommerfeld.pathetic.api.pathing.result.Path;
import de.bsommerfeld.pathetic.api.pathing.result.PathfinderResult;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.pathfinding.PatheticPathfinderService;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.quests.util.QuestTargetResolver;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Renders a particle trail along the path to the currently tracked quest target.
 */
public class QuestPathTrailTask extends BukkitRunnable {

    private static final int MAX_TRAIL_POINTS = 48;
    private static final int TRAIL_STEP = 2;
    private static final long RECALCULATE_INTERVAL_MS = 4000L;
    private static final double MIN_TARGET_DISTANCE_SQUARED = 36.0;
    private static final double RECALCULATE_DISTANCE_SQUARED = 9.0;

    private final Main plugin;
    private final QuestManager questManager;
    private final PatheticPathfinderService pathfinderService;
    private final Map<UUID, TrailState> trails = new HashMap<>();

    public QuestPathTrailTask(Main plugin, QuestManager questManager) {
        this.plugin = plugin;
        this.questManager = questManager;
        this.pathfinderService = new PatheticPathfinderService();
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            QuestTargetResolver.QuestTarget target = QuestTargetResolver.resolve(player, questManager);
            if (target == null || target.location() == null) {
                trails.remove(player.getUniqueId());
                continue;
            }

            Location targetLocation = target.location();
            if (targetLocation.getWorld() == null || !targetLocation.getWorld().equals(player.getWorld())) {
                trails.remove(player.getUniqueId());
                continue;
            }

            Location start = player.getLocation();
            if (start.distanceSquared(targetLocation) < MIN_TARGET_DISTANCE_SQUARED) {
                trails.remove(player.getUniqueId());
                continue;
            }

            TrailState state = trails.computeIfAbsent(player.getUniqueId(), id -> new TrailState());
            if (shouldRecalculate(state, start, targetLocation)) {
                state.markPending(start, targetLocation);
                requestPath(player, start, targetLocation, state);
            }

            renderTrail(player, state.points);
        }
    }

    private boolean shouldRecalculate(TrailState state, Location start, Location target) {
        if (state.pending) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (state.lastUpdated == 0L || now - state.lastUpdated > RECALCULATE_INTERVAL_MS) {
            return true;
        }
        if (state.lastStart == null || state.lastTarget == null) {
            return true;
        }
        return state.lastStart.distanceSquared(start) > RECALCULATE_DISTANCE_SQUARED
                || state.lastTarget.distanceSquared(target) > RECALCULATE_DISTANCE_SQUARED;
    }

    private void requestPath(Player player, Location start, Location target, TrailState state) {
        World world = player.getWorld();
        pathfinderService.findPath(world, start, target).thenAccept(result -> {
            if (!Bukkit.isPrimaryThread()) {
                Bukkit.getScheduler().runTask(plugin, () -> applyResult(world, result, state));
            } else {
                applyResult(world, result, state);
            }
        });
    }

    private void applyResult(World world, PathfinderResult result, TrailState state) {
        state.pending = false;
        if (result == null) {
            state.update(Collections.emptyList());
            return;
        }
        Path path = result.getPath();
        if (path == null || path.length() == 0) {
            state.update(Collections.emptyList());
            return;
        }
        List<Location> points = new ArrayList<>();
        for (PathPosition position : path.collect()) {
            points.add(new Location(
                    world,
                    position.getCenteredX(),
                    position.getFlooredY() + 0.15,
                    position.getCenteredZ()));
            if (points.size() >= MAX_TRAIL_POINTS) {
                break;
            }
        }
        state.update(points);
    }

    private void renderTrail(Player player, List<Location> points) {
        if (points == null || points.isEmpty()) {
            return;
        }
        for (int i = 0; i < points.size(); i += TRAIL_STEP) {
            Location loc = points.get(i);
            player.spawnParticle(Particle.END_ROD, loc, 1, 0, 0, 0, 0);
        }
    }

    private static final class TrailState {
        private List<Location> points = Collections.emptyList();
        private Location lastStart;
        private Location lastTarget;
        private long lastUpdated;
        private boolean pending;

        private void markPending(Location start, Location target) {
            this.pending = true;
            this.lastStart = start.clone();
            this.lastTarget = target.clone();
        }

        private void update(List<Location> points) {
            this.points = points != null ? points : Collections.emptyList();
            this.lastUpdated = System.currentTimeMillis();
        }
    }
}
