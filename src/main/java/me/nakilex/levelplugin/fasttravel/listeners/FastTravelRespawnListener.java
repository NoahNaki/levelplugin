package me.nakilex.levelplugin.fasttravel.listeners;

import me.nakilex.levelplugin.fasttravel.FastTravelManager;
import me.nakilex.levelplugin.fasttravel.data.FastTravelPoint;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FastTravelRespawnListener implements Listener {
    private final FastTravelManager manager;
    private final Map<UUID, Location> deathLocations = new HashMap<>();

    public FastTravelRespawnListener(FastTravelManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        deathLocations.put(event.getEntity().getUniqueId(), event.getEntity().getLocation());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Location deathLoc = deathLocations.remove(player.getUniqueId());
        if (deathLoc == null) return;

        FastTravelPoint nearest = null;
        double best = Double.MAX_VALUE;
        for (FastTravelPoint pt : manager.getPoints()) {
            if (!pt.isTown()) continue;
            if (!manager.isUnlocked(player, pt.getName())) continue;
            double dist = deathLoc.distanceSquared(pt.getLocation());
            if (dist < best) {
                best = dist;
                nearest = pt;
            }
        }
        if (nearest != null) {
            event.setRespawnLocation(nearest.getLocation());
        }
    }
}
