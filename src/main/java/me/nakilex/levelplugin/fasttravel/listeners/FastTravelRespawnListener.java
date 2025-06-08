package me.nakilex.levelplugin.fasttravel.listeners;

import me.nakilex.levelplugin.fasttravel.FastTravelManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

public class FastTravelRespawnListener implements Listener {
    private final FastTravelManager manager;
    public FastTravelRespawnListener(FastTravelManager manager){ this.manager = manager; }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event){
        var loc = manager.getNearestUnlockedTown(event.getPlayer());
        if(loc!=null){
            event.setRespawnLocation(loc);
        }
    }
}
