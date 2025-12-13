package me.nakilex.levelplugin.lootchests.listeners;

import me.nakilex.levelplugin.lootchests.managers.LootChestManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

public class LootChestChunkListener implements Listener {

    private final LootChestManager lootChestManager;

    public LootChestChunkListener(LootChestManager lootChestManager) {
        this.lootChestManager = lootChestManager;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        lootChestManager.removeInactiveChestsInChunk(event.getChunk());
    }
}
