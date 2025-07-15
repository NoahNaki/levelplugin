package me.nakilex.levelplugin.environment.listeners;

import me.nakilex.levelplugin.environment.EnvironmentManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;

public class EnvironmentDistanceListener implements Listener {
    private final EnvironmentManager manager;
    // Begin loading the town when players are within 350 blocks
    private static final double LOAD_DIST_SQ = 350 * 350;
    // Unload once they move beyond 400 blocks to keep a buffer
    private static final double UNLOAD_DIST_SQ = 400 * 400;
    /** Distance player must move closer before loading again. */
    private static final double RELOAD_STEP = 30.0;
    /** Stop triggering repeated loads once within this distance. */
    private static final double STOP_LOAD_DIST = 10.0;

    private final Map<java.util.UUID, Double> lastLoadDistance = new HashMap<>();
    private final Map<java.util.UUID, Double> previousDistance = new HashMap<>();

    public EnvironmentDistanceListener(EnvironmentManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        Location from = event.getFrom();
        if (to == null || (to.getBlockX() == from.getBlockX() && to.getBlockZ() == from.getBlockZ())) return;

        Location origin = manager.getOrigin(player.getUniqueId());
        if (origin == null || !origin.getWorld().equals(to.getWorld())) return;

        double dist = to.distance(origin);

        java.util.UUID id = player.getUniqueId();
        Double prev = previousDistance.put(id, dist);

        if (dist * dist <= LOAD_DIST_SQ) {
            manager.preloadTownChunks(player);
            if (!manager.isTownLoaded(player)) {
                if (!manager.hasPlayedInitAnimation(player)) {
                    manager.initializePlayerAnimated(player, 20);
                    manager.markAnimationPlayed(player);
                } else {
                    manager.initializePlayer(player);
                }
                manager.markTownLoaded(player, true);
                lastLoadDistance.put(id, dist);
            } else {
                Double last = lastLoadDistance.get(id);
                if (last != null && prev != null && dist < prev && last - dist >= RELOAD_STEP && dist > STOP_LOAD_DIST) {
                    manager.preloadTownChunks(player);
                    manager.initializePlayer(player);
                    lastLoadDistance.put(id, dist);
                }
            }
        } else if (dist * dist > UNLOAD_DIST_SQ) {
            if (manager.isTownLoaded(player)) {
                manager.unloadPlayerTown(player);
                manager.markTownLoaded(player, false);
            }
            lastLoadDistance.remove(id);
            previousDistance.remove(id);
        }
    }
}
