package me.nakilex.levelplugin.environment.listeners;

import me.nakilex.levelplugin.environment.EnvironmentManager;
import io.papermc.paper.event.player.PlayerChunkLoadEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.EventPriority;

public class EnvironmentChunkListener implements Listener {
    private final EnvironmentManager environmentManager;

    public EnvironmentChunkListener(EnvironmentManager environmentManager) {
        this.environmentManager = environmentManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(PlayerChunkLoadEvent event) {
        environmentManager.handleChunkLoad(event.getPlayer());
    }
}
