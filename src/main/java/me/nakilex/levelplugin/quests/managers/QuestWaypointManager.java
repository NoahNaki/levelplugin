package me.nakilex.levelplugin.quests.managers;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.quests.pathfinding.TrailPathfinder;
import me.nakilex.levelplugin.quests.pathfinding.TrailPathfinder.PathSettings;
import me.nakilex.levelplugin.quests.pathfinding.TrailPathfinder.PathTarget;
import me.nakilex.levelplugin.quests.pathfinding.TrailPathfinder.PathTargetKey;
import me.nakilex.levelplugin.quests.pathfinding.TrailPathfinder.StandablePath;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Handles the quest waypoint indicators: hologram pointer, trail particles, and beacon marker.
 */
public class QuestWaypointManager implements Listener {

    private static final Material POINTER_ICON = Material.COMPASS;
    private static final double POINTER_DISTANCE = 2.0;
    private static final double POINTER_VERTICAL_OFFSET = -0.35;
    private static final double POINTER_TEXT_OFFSET = 0.35;
    private static final double BEACON_HIDE_DISTANCE = 10.0;
    private static final int BEACON_HEIGHT = 22;
    private static final int BEACON_STEP = 2;
    private static final int TRAIL_POINT_COUNT = 25;
    private static final double TRAIL_POINT_SPACING = 0.5;
    private static final Particle TRAIL_PARTICLE_NORMAL = Particle.END_ROD;
    private static final Particle TRAIL_PARTICLE_HIGHLIGHT = Particle.FIREWORKS_SPARK;
    private static final double TRAIL_PARTICLE_SPREAD = 0.02;
    private static final int TRAIL_HIGHLIGHT_DISTANCE = 5;
    private static final double TRAIL_HIGHLIGHT_MULTIPLIER = 1.5;
    private static final double TRAIL_INVALIDATION_DISTANCE_SQ = 30 * 30;
    private static final double TRAIL_RETAIN_DISTANCE_SQ = 24 * 24;
    private static final double TRAIL_CALCULATE_AHEAD_DISTANCE_SQ = 18 * 18;
    private static final int PATH_MAX_RADIUS = 80;
    private static final int PATH_MAX_EXPANSIONS = 4500;
    private static final int PATH_MAX_DISTANCE = 120;

    private final Map<UUID, PlayerWaypointState> states = new HashMap<>();
    private final TrailPathfinder pathfinder = new TrailPathfinder();
    private final PathSettings pathSettings = new PathSettings(
            PATH_MAX_RADIUS,
            PATH_MAX_EXPANSIONS,
            PATH_MAX_DISTANCE,
            1,
            1,
            6,
            3
    );
    private final Main plugin;

    public QuestWaypointManager(Main plugin) {
        this.plugin = plugin;
    }

    public void updateIndicators(Player player, PathTarget target, String questName, String objectiveText) {
        if (player == null) {
            return;
        }
        PlayerWaypointState state = states.computeIfAbsent(player.getUniqueId(), key -> new PlayerWaypointState());
        if (target == null || target.location() == null) {
            clearIndicators(player, state);
            return;
        }

        Location targetLoc = target.location();
        boolean sameWorld = targetLoc.getWorld() != null && targetLoc.getWorld().equals(player.getWorld());
        double distance = sameWorld ? player.getLocation().distance(targetLoc) : -1d;
        updatePointer(player, state, targetLoc, questName, objectiveText, distance, sameWorld);

        if (!sameWorld) {
            clearTrail(state);
            return;
        }

        if (distance >= 0 && distance > BEACON_HIDE_DISTANCE) {
            spawnBeaconParticles(player, targetLoc);
        }

        updateTrail(player, state, target);
        spawnTrailParticles(player, state);
    }

    public void clearIndicators(Player player) {
        PlayerWaypointState state = states.remove(player.getUniqueId());
        if (state != null) {
            clearIndicators(player, state);
        }
    }

    public void clearAll() {
        for (UUID playerId : new ArrayList<>(states.keySet())) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                clearIndicators(player, states.get(playerId));
            }
        }
        states.clear();
    }

    private void clearIndicators(Player player, PlayerWaypointState state) {
        if (state == null) {
            return;
        }
        removeDisplay(player, state.pointerItem);
        removeDisplay(player, state.pointerText);
        state.pointerItem = null;
        state.pointerText = null;
        clearTrail(state);
    }

    private void clearTrail(PlayerWaypointState state) {
        if (state != null) {
            state.pathPoints = Collections.emptyList();
            state.pathTargetKey = null;
            if (state.pathFuture != null) {
                state.pathFuture.cancel(true);
            }
            state.pathFuture = CompletableFuture.completedFuture(null);
            state.highlightCounter = 0;
        }
    }

    private void updatePointer(Player player,
                               PlayerWaypointState state,
                               Location targetLoc,
                               String questName,
                               String objectiveText,
                               double distance,
                               boolean sameWorld) {
        Location base = player.getEyeLocation();
        Vector dir = base.getDirection().normalize();
        Location pointerLoc = base.clone().add(dir.multiply(POINTER_DISTANCE)).add(0, POINTER_VERTICAL_OFFSET, 0);
        ItemDisplay item = ensurePointerItem(player, state, pointerLoc);
        TextDisplay text = ensurePointerText(player, state, pointerLoc.clone().add(0, POINTER_TEXT_OFFSET, 0));

        if (item != null) {
            item.teleport(pointerLoc);
        }
        if (text != null) {
            text.teleport(pointerLoc.clone().add(0, POINTER_TEXT_OFFSET, 0));
            text.setText(buildPointerText(questName, objectiveText, distance, sameWorld, targetLoc));
        }
    }

    private ItemDisplay ensurePointerItem(Player player, PlayerWaypointState state, Location location) {
        if (state.pointerItem != null && !state.pointerItem.isDead()) {
            return state.pointerItem;
        }
        ItemDisplay item = (ItemDisplay) location.getWorld().spawnEntity(location, EntityType.ITEM_DISPLAY);
        item.setItemStack(new ItemStack(POINTER_ICON));
        item.setBillboard(Display.Billboard.CENTER);
        item.setTeleportDuration(2);
        configurePrivateDisplay(player, item);
        state.pointerItem = item;
        return item;
    }

    private TextDisplay ensurePointerText(Player player, PlayerWaypointState state, Location location) {
        if (state.pointerText != null && !state.pointerText.isDead()) {
            return state.pointerText;
        }
        TextDisplay display = (TextDisplay) location.getWorld().spawnEntity(location, EntityType.TEXT_DISPLAY);
        display.setBillboard(Display.Billboard.CENTER);
        display.setShadowRadius(0f);
        display.setShadowStrength(0f);
        display.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
        display.setTeleportDuration(2);
        configurePrivateDisplay(player, display);
        state.pointerText = display;
        return display;
    }

    private void configurePrivateDisplay(Player player, Display display) {
        display.setVisibleByDefault(false);
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.hideEntity(plugin, display);
        }
        player.showEntity(plugin, display);
    }

    private void removeDisplay(Player player, Display display) {
        if (display == null) {
            return;
        }
        display.remove();
        if (player != null && player.isOnline()) {
            player.hideEntity(plugin, display);
        }
    }

    private String buildPointerText(String questName,
                                    String objectiveText,
                                    double distance,
                                    boolean sameWorld,
                                    Location targetLoc) {
        String nameLine = ChatColor.LIGHT_PURPLE + (questName == null ? "Quest" : questName);
        String objective = objectiveText == null || objectiveText.isBlank() ? "Reach the objective" : objectiveText;
        if (!sameWorld) {
            String worldName = targetLoc != null && targetLoc.getWorld() != null
                    ? targetLoc.getWorld().getName()
                    : "Unknown";
            return nameLine + "\n" + ChatColor.GRAY + objective + " " + ChatColor.DARK_GRAY + "(" + worldName + ")";
        }
        String distText = distance >= 0 ? String.format("%.0fm", distance) : "??m";
        return nameLine + "\n" + ChatColor.GRAY + objective + " " + ChatColor.YELLOW + distText;
    }

    private void updateTrail(Player player, PlayerWaypointState state, PathTarget target) {
        if (player == null || state == null || target == null || target.location() == null) {
            return;
        }
        PathTargetKey key = target.key();
        if (state.pathTargetKey == null || !state.pathTargetKey.equals(key)) {
            state.pathPoints = new ArrayList<>();
            state.pathTargetKey = key;
        }

        if (!state.pathFuture.isDone()) {
            return;
        }

        if (shouldInvalidateTrail(player, target.location(), state.pathPoints)) {
            state.pathPoints.clear();
        }

        if (state.pathPoints.isEmpty()) {
            startPathCompute(player, state, player.getLocation(), target.location(), false);
            return;
        }

        Location last = state.pathPoints.get(state.pathPoints.size() - 1);
        if (!blockEquals(last, target.location())
                && player.getLocation().distanceSquared(last) < TRAIL_CALCULATE_AHEAD_DISTANCE_SQ) {
            startPathCompute(player, state, last, target.location(), true);
        }
    }

    private boolean shouldInvalidateTrail(Player player, Location target, List<Location> trail) {
        if (trail == null || trail.isEmpty()) {
            return true;
        }
        if (player.getWorld() != trail.get(0).getWorld()) {
            return true;
        }
        boolean allFar = true;
        Location playerLoc = player.getLocation();
        for (Location loc : trail) {
            if (playerLoc.distanceSquared(loc) < TRAIL_INVALIDATION_DISTANCE_SQ) {
                allFar = false;
                break;
            }
        }
        if (allFar) {
            return true;
        }
        return trail.size() == 1 && !blockEquals(trail.get(0), target);
    }

    private void startPathCompute(Player player,
                                  PlayerWaypointState state,
                                  Location start,
                                  Location goal,
                                  boolean append) {
        if (player == null || state == null || start == null || goal == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        PathTargetKey key = state.pathTargetKey;
        state.pathFuture = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            StandablePath path = pathfinder.findPath(start, goal, pathSettings);
            List<Location> points = buildPathPoints(path);
            Bukkit.getScheduler().runTask(plugin, () -> {
                PlayerWaypointState current = states.get(playerId);
                if (current == null || !Objects.equals(current.pathTargetKey, key)) {
                    state.pathFuture.complete(null);
                    return;
                }
                if (append) {
                    pruneTrailBehindPlayer(Bukkit.getPlayer(playerId), current.pathPoints);
                    appendTrailPoints(current, points);
                } else {
                    current.pathPoints = new ArrayList<>(points);
                }
                state.pathFuture.complete(null);
            });
        });
    }

    private void pruneTrailBehindPlayer(Player player, List<Location> trail) {
        if (player == null || trail == null || trail.isEmpty()) {
            return;
        }
        Location playerLoc = player.getLocation();
        int lastIndex = -1;
        for (int i = 0; i < trail.size(); i++) {
            Location loc = trail.get(i);
            if (playerLoc.distanceSquared(loc) >= TRAIL_RETAIN_DISTANCE_SQ) {
                lastIndex = i;
            }
        }
        if (lastIndex > 0) {
            trail.subList(0, lastIndex).clear();
        }
    }

    private void appendTrailPoints(PlayerWaypointState state, List<Location> points) {
        if (state == null || points == null || points.isEmpty()) {
            return;
        }
        if (state.pathPoints.isEmpty()) {
            state.pathPoints = new ArrayList<>(points);
            return;
        }
        for (int i = 0; i < points.size(); i++) {
            if (i == 0) {
                continue;
            }
            state.pathPoints.add(points.get(i));
        }
    }

    private List<Location> buildPathPoints(StandablePath path) {
        if (path == null || path.locations().isEmpty()) {
            if (path != null && path.start() != null && path.goal() != null) {
                return TrailPathfinder.interpolate(List.of(path.start(), path.goal()), TRAIL_POINT_SPACING);
            }
            return Collections.emptyList();
        }
        List<Location> simplified = TrailPathfinder.simplify(path.locations());
        return TrailPathfinder.interpolate(simplified, TRAIL_POINT_SPACING);
    }

    private void spawnTrailParticles(Player player, PlayerWaypointState state) {
        if (state == null || state.pathPoints == null || state.pathPoints.isEmpty()) {
            return;
        }
        int startIndex = findClosestIndex(state.pathPoints, player.getLocation());
        int endIndex = Math.min(state.pathPoints.size(), startIndex + TRAIL_POINT_COUNT);
        for (int i = startIndex; i < endIndex; i++) {
            Location loc = state.pathPoints.get(i);
            boolean highlight = ((i - state.highlightCounter) % TRAIL_HIGHLIGHT_DISTANCE) == 0;
            int amount = highlight ? (int) Math.ceil(1 * TRAIL_HIGHLIGHT_MULTIPLIER) : 1;
            Particle particle = highlight ? TRAIL_PARTICLE_HIGHLIGHT : TRAIL_PARTICLE_NORMAL;
            player.spawnParticle(particle, loc, amount, TRAIL_PARTICLE_SPREAD, TRAIL_PARTICLE_SPREAD, TRAIL_PARTICLE_SPREAD, 0);
        }
        state.highlightCounter = (state.highlightCounter + 1) % TRAIL_HIGHLIGHT_DISTANCE;
    }

    private int findClosestIndex(List<Location> points, Location playerLoc) {
        if (points.isEmpty() || playerLoc == null) {
            return 0;
        }
        double bestDist = Double.MAX_VALUE;
        int bestIndex = 0;
        for (int i = 0; i < points.size(); i++) {
            Location loc = points.get(i);
            double dist = loc.distanceSquared(playerLoc);
            if (dist < bestDist) {
                bestDist = dist;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private void spawnBeaconParticles(Player player, Location target) {
        if (player == null || target == null || target.getWorld() == null) {
            return;
        }
        World world = target.getWorld();
        double x = target.getX() + 0.5;
        double z = target.getZ() + 0.5;
        int baseY = target.getBlockY();
        for (int y = 0; y <= BEACON_HEIGHT; y += BEACON_STEP) {
            Location loc = new Location(world, x, baseY + y, z);
            player.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 1, 0, 0, 0, 0);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        clearIndicators(event.getPlayer());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joined = event.getPlayer();
        for (Map.Entry<UUID, PlayerWaypointState> entry : states.entrySet()) {
            Player owner = Bukkit.getPlayer(entry.getKey());
            PlayerWaypointState state = entry.getValue();
            if (owner == null || state == null) {
                continue;
            }
            if (joined.equals(owner)) {
                continue;
            }
            hideDisplayFrom(state.pointerItem, joined);
            hideDisplayFrom(state.pointerText, joined);
        }
    }

    private void hideDisplayFrom(Display display, Player player) {
        if (display == null || player == null) {
            return;
        }
        player.hideEntity(plugin, display);
    }

    private boolean blockEquals(Location a, Location b) {
        if (a == null || b == null || a.getWorld() == null || b.getWorld() == null) {
            return false;
        }
        return a.getWorld().equals(b.getWorld())
                && a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
    }

    private static class PlayerWaypointState {
        private ItemDisplay pointerItem;
        private TextDisplay pointerText;
        private List<Location> pathPoints = new ArrayList<>();
        private PathTargetKey pathTargetKey;
        private CompletableFuture<Void> pathFuture = CompletableFuture.completedFuture(null);
        private int highlightCounter;
    }
}
