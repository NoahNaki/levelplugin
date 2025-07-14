package me.nakilex.levelplugin.environment.listeners;

import me.nakilex.levelplugin.environment.EnvironmentManager;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class EnvironmentChunkListener implements Listener {
    private final EnvironmentManager environmentManager;

    public EnvironmentChunkListener(EnvironmentManager environmentManager) {
        this.environmentManager = environmentManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();

        for (Player player : chunk.getWorld().getPlayers()) {
            int cx = chunk.getX();
            int cz = chunk.getZ();

            // resend fake blocks contained in this chunk
            environmentManager.handleChunkLoad(player, chunk);

            var origin = environmentManager.getOrigin(player.getUniqueId());
            if (origin != null && origin.getChunk().getX() == cx && origin.getChunk().getZ() == cz) {
                environmentManager.initializePlayer(player);
            }
        }
    }
}
