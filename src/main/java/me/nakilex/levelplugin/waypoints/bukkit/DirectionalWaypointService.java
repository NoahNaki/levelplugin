package me.nakilex.levelplugin.waypoints.bukkit;

import me.nakilex.levelplugin.waypoints.api.provider.NavigationTarget;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Maintains private, player-relative text markers that point toward world-space destinations.
 */
public final class DirectionalWaypointService implements Listener {
    private static final double DISPLAY_DISTANCE = 5.0D;
    private static final double ARRIVAL_DISTANCE = 2.0D;
    private static final long UPDATE_PERIOD_TICKS = 2L;
    private static final String ENTITY_TAG = "levelplugin_directional_waypoint";

    private final JavaPlugin plugin;
    private final Map<UUID, ActiveWaypoint> activeWaypoints = new HashMap<>();
    private BukkitTask updateTask;

    public DirectionalWaypointService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (updateTask != null) {
            return;
        }
        updateTask = plugin.getServer().getScheduler().runTaskTimer(
                plugin, this::updateAll, 1L, UPDATE_PERIOD_TICKS);
    }

    /** Shows or replaces the active waypoint for a player. */
    public void show(Player player, String label, NavigationTarget target) {
        if (player == null || target == null) {
            return;
        }
        ActiveWaypoint previous = activeWaypoints.get(player.getUniqueId());
        TextDisplay display = previous == null ? null : previous.display;
        activeWaypoints.put(player.getUniqueId(), new ActiveWaypoint(sanitizeLabel(label), target, display));
        update(player, activeWaypoints.get(player.getUniqueId()));
    }

    public boolean hasWaypoint(Player player) {
        return player != null && activeWaypoints.containsKey(player.getUniqueId());
    }

    public void remove(Player player) {
        if (player != null) {
            remove(player.getUniqueId());
        }
    }

    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        for (UUID playerId : activeWaypoints.keySet().toArray(UUID[]::new)) {
            remove(playerId);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        remove(event.getPlayer());
    }

    private void updateAll() {
        for (Map.Entry<UUID, ActiveWaypoint> entry : activeWaypoints.entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                update(player, entry.getValue());
            }
        }
    }

    private void update(Player player, ActiveWaypoint waypoint) {
        Location target = waypoint.target.resolve(player);
        Location eye = player.getEyeLocation();
        if (!isSameWorld(eye, target)) {
            waypoint.removeDisplay();
            return;
        }

        double distance = player.getLocation().distance(target);
        if (distance <= ARRIVAL_DISTANCE) {
            waypoint.removeDisplay();
            return;
        }

        Vector direction = target.toVector().subtract(eye.toVector());
        if (direction.lengthSquared() < 1.0E-6D) {
            waypoint.removeDisplay();
            return;
        }

        double markerDistance = Math.min(DISPLAY_DISTANCE, direction.length());
        Location displayLocation = eye.clone().add(direction.normalize().multiply(markerDistance));
        if (waypoint.display == null || !waypoint.display.isValid()
                || !waypoint.display.getWorld().equals(displayLocation.getWorld())) {
            waypoint.removeDisplay();
            waypoint.display = spawnDisplay(player, displayLocation);
        } else {
            waypoint.display.teleport(displayLocation);
        }
        waypoint.display.setText(ChatColor.GOLD + "" + ChatColor.BOLD + waypoint.label
                + ChatColor.GRAY + " (" + Math.round(distance) + "m)");
    }

    private TextDisplay spawnDisplay(Player owner, Location location) {
        World world = location.getWorld();
        if (world == null) {
            return null;
        }
        TextDisplay display = world.spawn(location, TextDisplay.class, spawned -> {
            spawned.setPersistent(false);
            spawned.setVisibleByDefault(false);
            spawned.addScoreboardTag(ENTITY_TAG);
            spawned.setBillboard(Display.Billboard.CENTER);
            spawned.setAlignment(TextDisplay.TextAlignment.CENTER);
            spawned.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            spawned.setShadowed(true);
            spawned.setSeeThrough(false);
            spawned.setTeleportDuration((int) UPDATE_PERIOD_TICKS);
            spawned.setViewRange(0.5F);
        });
        owner.showEntity(plugin, display);
        return display;
    }

    private void remove(UUID playerId) {
        ActiveWaypoint waypoint = activeWaypoints.remove(playerId);
        if (waypoint != null) {
            waypoint.removeDisplay();
        }
    }

    private boolean isSameWorld(Location first, Location second) {
        return first != null && second != null && first.getWorld() != null
                && first.getWorld().equals(second.getWorld());
    }

    private String sanitizeLabel(String label) {
        String clean = label == null ? "Waypoint" : ChatColor.stripColor(label).trim();
        if (clean.isEmpty()) {
            return "Waypoint";
        }
        return clean.length() > 32 ? clean.substring(0, 32) : clean;
    }

    private static final class ActiveWaypoint {
        private final String label;
        private final NavigationTarget target;
        private TextDisplay display;

        private ActiveWaypoint(String label, NavigationTarget target, TextDisplay display) {
            this.label = label;
            this.target = target;
            this.display = display;
        }

        private void removeDisplay() {
            if (display != null && display.isValid()) {
                display.remove();
            }
            display = null;
        }
    }
}
