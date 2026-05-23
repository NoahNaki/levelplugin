package me.nakilex.levelplugin.player.mining.listeners;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.environment.EnvironmentAreaInstanceManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles kingdom mine regeneration gameplay for per-player environment worlds.
 */
public final class KingdomMineRegenListener implements Listener {

    private static final long REGEN_TICKS = 20L * 10L;

    private final Main plugin;
    private final EnvironmentAreaInstanceManager areaManager;
    private final Map<Material, Material> oreFallback = new HashMap<>();

    public KingdomMineRegenListener(Main plugin, EnvironmentAreaInstanceManager areaManager) {
        this.plugin = plugin;
        this.areaManager = areaManager;
        registerOreFallbacks();
    }

    private void registerOreFallbacks() {
        for (Material material : Material.values()) {
            if (material.name().endsWith("_ORE") || material == Material.ANCIENT_DEBRIS) {
                if (material.name().startsWith("DEEPSLATE_")) {
                    oreFallback.put(material, Material.DEEPSLATE);
                } else if (material.name().startsWith("NETHER_") || material == Material.ANCIENT_DEBRIS) {
                    oreFallback.put(material, Material.NETHERRACK);
                } else {
                    oreFallback.put(material, Material.STONE);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        if (!areaManager.isMineBlock(player, block)) {
            return;
        }

        Material current = block.getType();
        Material next = nextState(current);
        if (next == null) {
            return;
        }

        event.setDropItems(false);
        event.setExpToDrop(0);
        BlockData original = block.getBlockData().clone();
        block.setType(next, false);

        if (next == Material.BEDROCK) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!block.getChunk().isLoaded()) {
                    block.getChunk().load();
                }
                if (block.getType() == Material.BEDROCK && areaManager.isMineBlock(player, block)) {
                    block.setBlockData(original, false);
                }
            }, REGEN_TICKS);
        }
    }

    private Material nextState(Material material) {
        if (material == Material.STONE || material == Material.DEEPSLATE || material == Material.NETHERRACK) {
            return Material.COBBLESTONE;
        }
        if (material == Material.COBBLESTONE) {
            return Material.BEDROCK;
        }
        Material fallback = oreFallback.get(material);
        if (fallback != null) {
            return fallback;
        }
        return null;
    }
}
