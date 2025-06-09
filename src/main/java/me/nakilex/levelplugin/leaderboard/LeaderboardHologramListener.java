package me.nakilex.levelplugin.leaderboard;

import org.bukkit.Chunk;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

public class LeaderboardHologramListener implements Listener {
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        removeStands(event.getChunk());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        removeStands(event.getChunk());
    }

    private void removeStands(Chunk chunk) {
        for (var e : chunk.getEntities()) {
            if (e instanceof ArmorStand stand && stand.getScoreboardTags().contains("leaderboard_hologram")) {
                stand.remove();
            }
        }
    }
}
