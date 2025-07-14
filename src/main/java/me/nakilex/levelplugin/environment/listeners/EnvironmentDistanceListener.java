package me.nakilex.levelplugin.environment.listeners;

import me.nakilex.levelplugin.environment.EnvironmentManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class EnvironmentDistanceListener implements Listener {
    private final EnvironmentManager manager;
    private static final double LOAD_DIST_SQ = 150 * 150;
    private static final double UNLOAD_DIST_SQ = 200 * 200;

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

        double distSq = to.distanceSquared(origin);
        if (distSq <= LOAD_DIST_SQ) {
            if (!manager.isTownLoaded(player)) {
                if (!manager.hasPlayedInitAnimation(player)) {
                    manager.initializePlayerAnimated(player, 20);
                    manager.markAnimationPlayed(player);
                } else {
                    manager.initializePlayer(player);
                }
                manager.markTownLoaded(player, true);
            }
        } else if (distSq > UNLOAD_DIST_SQ) {
            if (manager.isTownLoaded(player)) {
                manager.unloadPlayerTown(player);
                manager.markTownLoaded(player, false);
            }
        }
    }
}
