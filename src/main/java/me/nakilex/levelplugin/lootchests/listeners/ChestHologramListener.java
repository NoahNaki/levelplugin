package me.nakilex.levelplugin.lootchests.listeners;

import me.nakilex.levelplugin.lootchests.data.ChestData;
import me.nakilex.levelplugin.lootchests.managers.LootChestManager;
import org.bukkit.Chunk;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

public class ChestHologramListener implements Listener {
    private final LootChestManager lootChestManager;

    public ChestHologramListener(LootChestManager lootChestManager) {
        this.lootChestManager = lootChestManager;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();

        // 1) remove any stale stands in this chunk
        for (var e : chunk.getEntities()) {
            if (e instanceof ArmorStand stand &&
                stand.getScoreboardTags().contains("loot_hologram")) {
                stand.remove();
            }
        }

        // 2) spawn fresh holograms for any chests whose Location is in here
        for (ChestData data : lootChestManager.getAllChestData()) {
            var loc = data.toLocation();
            if (loc != null && loc.getChunk().equals(chunk)) {
                lootChestManager.spawnHologramForChest(data);
            }
        }
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        // remove any holograms so they don't linger when the chunk unloads
        for (var e : chunk.getEntities()) {
            if (e instanceof ArmorStand stand &&
                stand.getScoreboardTags().contains("loot_hologram")) {
                stand.remove();
            }
        }
    }
}
