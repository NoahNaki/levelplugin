package me.nakilex.levelplugin.fasttravel.listeners;

import me.nakilex.levelplugin.fasttravel.FastTravelManager;
import me.nakilex.levelplugin.fasttravel.data.FastTravelPoint;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

public class RespawnListener implements Listener {
    private final FastTravelManager manager;

    public RespawnListener(FastTravelManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        FastTravelPoint pt = manager.getNearestTown(event.getPlayer());
        if (pt != null) {
            event.setRespawnLocation(pt.getLocation());
        }
    }
}
