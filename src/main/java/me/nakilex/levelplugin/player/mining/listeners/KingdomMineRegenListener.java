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
import me.nakilex.levelplugin.utils.ChatMessageUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles kingdom mine regeneration gameplay for per-player environment worlds.
 */
public final class KingdomMineRegenListener implements Listener {

    private static final long REGEN_TICKS = 20L * 10L;
    private static final long DEBUG_COOLDOWN_MS = 1200L;

    private final Main plugin;
    private final EnvironmentAreaInstanceManager areaManager;
    private final Map<Material, Material> oreFallback = new HashMap<>();
    private final Map<UUID, Long> lastDebugMessageAt = new HashMap<>();

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

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        EnvironmentAreaInstanceManager.MineDebugInfo debug = areaManager.mineDebug(player, block);

        // Always expose event flow for environment worlds so we can diagnose cancellations/filters.
        maybeSendDebug(player, debug, event.isCancelled(), "event_seen");

        if (event.isCancelled()) {
            maybeSendDebug(player, debug, true, "cancelled_before_mine_logic");
            return;
        }

        if (!debug.insideMine()) {
            maybeSendDebug(player, debug, false, "outside_mine");
            return;
        }

        Material current = block.getType();
        Material next = nextState(current);
        if (next == null) {
            maybeSendDebug(player, debug, false, "inside_mine_unsupported_material:" + current.name());
            return;
        }

        event.setDropItems(false);
        event.setExpToDrop(0);
        BlockData original = block.getBlockData().clone();
        block.setType(next, false);
        maybeSendDebug(player, debug, false, "transition:" + current.name() + "->" + next.name());

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

    private void maybeSendDebug(Player player, EnvironmentAreaInstanceManager.MineDebugInfo debug, boolean cancelled, String stage) {
        if (player == null || debug == null) {
            return;
        }
        String worldName = player.getWorld() == null ? "" : player.getWorld().getName();
        if (!worldName.startsWith("environment_")) {
            return;
        }
        long now = System.currentTimeMillis();
        long last = lastDebugMessageAt.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < DEBUG_COOLDOWN_MS) {
            return;
        }
        lastDebugMessageAt.put(player.getUniqueId(), now);
        String message = "[MineDebug] stage=" + stage + ", cancelled=" + cancelled + ", " + debug.summary();
        if (player.isOp()) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO, message);
        }
        plugin.getLogger().info("[KingdomMineDebug] player=" + player.getName() + " " + message);
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
