package me.nakilex.levelplugin.environment.listeners;

import me.nakilex.levelplugin.environment.EnvironmentManager;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.EventPriority;
import org.bukkit.event.world.ChunkLoadEvent;

public class EnvironmentChunkListener implements Listener {
    private final EnvironmentManager environmentManager;

    public EnvironmentChunkListener(EnvironmentManager environmentManager) {
        this.environmentManager = environmentManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        int cx = chunk.getX();
        int cz = chunk.getZ();
        int view = chunk.getWorld().getViewDistance();
        for (Player player : chunk.getWorld().getPlayers()) {
            int pcx = player.getLocation().getChunk().getX();
            int pcz = player.getLocation().getChunk().getZ();
            if (Math.abs(pcx - cx) <= view && Math.abs(pcz - cz) <= view) {
                environmentManager.handleChunkLoad(player);
            }
        }
    }
}
