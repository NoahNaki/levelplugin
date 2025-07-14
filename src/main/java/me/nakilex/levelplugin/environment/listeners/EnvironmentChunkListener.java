package me.nakilex.levelplugin.environment.listeners;

import me.nakilex.levelplugin.environment.EnvironmentManager;
import io.papermc.paper.event.player.PlayerChunkLoadEvent;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class EnvironmentChunkListener implements Listener {
    private final EnvironmentManager environmentManager;

    public EnvironmentChunkListener(EnvironmentManager environmentManager) {
        this.environmentManager = environmentManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(PlayerChunkLoadEvent event) {
        Player player = event.getPlayer();
        Chunk chunk = event.getChunk();
        int cx = chunk.getX();
        int cz = chunk.getZ();

        // Re-send fake blocks for this chunk to the player now that it is
        // actually sent to them
        environmentManager.handleChunkLoad(player, chunk);

        // If this chunk contains the player's settlement origin, respawn all
        // structures instantly so they appear correctly
        var origin = environmentManager.getOrigin(player.getUniqueId());
        if (origin != null && origin.getChunk().getX() == cx && origin.getChunk().getZ() == cz) {
            environmentManager.initializePlayer(player);
        }
    }
}
