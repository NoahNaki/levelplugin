package me.nakilex.levelplugin.environment.listeners;

import me.nakilex.levelplugin.environment.EnvironmentManager;
import me.nakilex.levelplugin.Main;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

public class EnvironmentDistanceListener implements Listener {
    private final EnvironmentManager manager;
    private static final double LOAD_DIST_SQ = 150 * 150;
    private static final double UNLOAD_DIST_SQ = 200 * 200;
    // begin preloading when within this distance
    private static final double AUTOLOAD_START_SQ = 100 * 100;
    // stop repeating loads once player reaches this distance
    private static final double AUTOLOAD_STOP_SQ = 50 * 50;

    private final Map<java.util.UUID, BukkitTask> loadTasks = new HashMap<>();

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

        java.util.UUID id = player.getUniqueId();

        if (distSq <= AUTOLOAD_START_SQ && distSq > AUTOLOAD_STOP_SQ) {
            if (!loadTasks.containsKey(id)) {
                BukkitTask task = new BukkitRunnable() {
                    @Override
                    public void run() {
                        manager.preloadTownChunks(player);
                        if (!manager.isTownLoaded(player)) {
                            manager.initializePlayer(player);
                        }
                    }
                }.runTaskTimer(Main.getInstance(), 0L, 70L); // approx 3.5s
                loadTasks.put(id, task);
            }
        } else {
            BukkitTask t = loadTasks.remove(id);
            if (t != null) t.cancel();
        }

        if (distSq <= LOAD_DIST_SQ) {
            manager.preloadTownChunks(player);
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
