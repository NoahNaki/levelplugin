package me.nakilex.levelplugin.waypoints;

import me.nakilex.levelplugin.Main;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Renders quest-related waypoint visuals: beacon, trail particles, and hologram text.
 */
public class WaypointDisplayManager implements Listener {
    private static final double HOVER_OFFSET = 1.6;
    private static final double HOLOGRAM_MOVE_THRESHOLD_SQUARED = 0.5;
    private static final double MIN_DISTANCE_TO_RENDER = 10.0;
    private static final double TRAIL_STEP = 1.8;
    private static final double TRAIL_MAX_DISTANCE = 40.0;
    private static final Particle TRAIL_PARTICLE = Particle.END_ROD;

    private final Main plugin;
    private final WaypointBeaconRenderer beaconRenderer;
    private final WaypointHologramRenderer hologramRenderer;
    private final WaypointTrailRenderer trailRenderer;

    public WaypointDisplayManager(Main plugin, me.nakilex.levelplugin.quests.managers.BeaconManager beaconManager) {
        this.plugin = plugin;
        this.beaconRenderer = new WaypointBeaconRenderer(beaconManager);
        this.hologramRenderer = new WaypointHologramRenderer(HOVER_OFFSET, HOLOGRAM_MOVE_THRESHOLD_SQUARED);
        this.trailRenderer = new WaypointTrailRenderer(TRAIL_PARTICLE, TRAIL_STEP, TRAIL_MAX_DISTANCE);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void update(Player player, Location target, List<String> hologramLines) {
        if (player == null || target == null || target.getWorld() == null) {
            clear(player);
            return;
        }
        if (!player.getWorld().equals(target.getWorld())) {
            clear(player);
            return;
        }

        double distance = player.getLocation().distance(target);
        if (distance < MIN_DISTANCE_TO_RENDER) {
            clear(player);
            return;
        }

        beaconRenderer.show(player, target);
        hologramRenderer.update(player, target, hologramLines);
        trailRenderer.render(player, target, distance);
    }

    public void clear(Player player) {
        if (player == null) {
            return;
        }
        beaconRenderer.clear(player);
        hologramRenderer.clear(player);
    }

    public void clearAll() {
        for (Player player : new ArrayList<>(plugin.getServer().getOnlinePlayers())) {
            clear(player);
        }
        hologramRenderer.clearAll();
        beaconRenderer.clearAll();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clear(event.getPlayer());
    }

    @EventHandler
    public void onDisable(PluginDisableEvent event) {
        if (event.getPlugin() == plugin) {
            clearAll();
        }
    }

    public WaypointBeaconRenderer getBeaconRenderer() {
        return beaconRenderer;
    }

    public WaypointHologramRenderer getHologramRenderer() {
        return hologramRenderer;
    }

    public WaypointTrailRenderer getTrailRenderer() {
        return trailRenderer;
    }
}
