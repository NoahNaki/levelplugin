package me.nakilex.levelplugin.utils;

import me.nakilex.levelplugin.Main;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Generic per-player block glow helper.
 *
 * <p>Uses fr.skytasul.glowingentities via reflection when available. This keeps
 * the utility reusable and avoids hard-coupling callers to any single implementation.</p>
 */
public class BlockGlowUtil implements Listener {
    private final Main plugin;

    private final Map<UUID, GlowState> active = new HashMap<>();
    private BukkitTask tickTask;

    private boolean supported;
    private Object glowingBlocksInstance;
    private Method setGlowingMethod;
    private Method unsetGlowingMethod;
    private long tickCounter;

    public BlockGlowUtil(Main plugin) {
        this.plugin = plugin;
        initializeBridge();
        restartTickTask();
    }

    public boolean isSupported() {
        return supported;
    }

    public void setGlowing(Player player, Collection<Block> blocks, ChatColor color, int durationTicks) {
        if (!supported || player == null || blocks == null || blocks.isEmpty()) {
            return;
        }
        clearGlowing(player);

        Set<Block> tracked = new HashSet<>();
        for (Block block : blocks) {
            if (block == null) {
                continue;
            }
            if (invokeSetGlowing(block, player, color)) {
                tracked.add(block);
            }
        }

        if (!tracked.isEmpty()) {
            long expiresAt = tickCounter + Math.max(1, durationTicks);
            active.put(player.getUniqueId(), new GlowState(tracked, expiresAt));
        }
    }

    public void setGlowing(Block block, Player player, ChatColor color, int durationTicks) {
        if (block == null) {
            return;
        }
        setGlowing(player, Collections.singletonList(block), color, durationTicks);
    }

    public void clearGlowing(Player player) {
        if (player == null || !supported) {
            return;
        }
        GlowState removed = active.remove(player.getUniqueId());
        if (removed == null) {
            return;
        }
        for (Block block : removed.blocks) {
            invokeUnsetGlowing(block, player);
        }
    }

    public void shutdown() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }

        if (!supported) {
            active.clear();
            return;
        }

        for (UUID id : new ArrayList<>(active.keySet())) {
            Player player = plugin.getServer().getPlayer(id);
            if (player != null) {
                clearGlowing(player);
            }
        }
        active.clear();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clearGlowing(event.getPlayer());
    }

    private void initializeBridge() {
        try {
            Class<?> glowingBlocksClass = Class.forName("fr.skytasul.glowingentities.GlowingBlocks");
            Constructor<?> constructor = glowingBlocksClass.getConstructor(org.bukkit.plugin.Plugin.class);
            glowingBlocksInstance = constructor.newInstance(plugin);
            setGlowingMethod = glowingBlocksClass.getMethod("setGlowing", Block.class, Player.class, ChatColor.class);
            unsetGlowingMethod = glowingBlocksClass.getMethod("unsetGlowing", Block.class, Player.class);
            supported = true;
        } catch (ReflectiveOperationException ex) {
            supported = false;
            plugin.getLogger().warning("[BlockGlowUtil] GlowingBlocks bridge unavailable; block glow is disabled.");
        }
    }

    private void restartTickTask() {
        if (tickTask != null) {
            tickTask.cancel();
        }
        tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    private void tick() {
        tickCounter++;
        if (!supported || active.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<UUID, GlowState>> iterator = active.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, GlowState> entry = iterator.next();
            Player player = plugin.getServer().getPlayer(entry.getKey());
            GlowState state = entry.getValue();
            if (player == null || !player.isOnline() || tickCounter >= state.expiresAtTick) {
                if (player != null) {
                    for (Block block : state.blocks) {
                        invokeUnsetGlowing(block, player);
                    }
                }
                iterator.remove();
            }
        }
    }

    private boolean invokeSetGlowing(Block block, Player player, ChatColor color) {
        try {
            setGlowingMethod.invoke(glowingBlocksInstance, block, player, color);
            return true;
        } catch (ReflectiveOperationException ex) {
            return false;
        }
    }

    private void invokeUnsetGlowing(Block block, Player player) {
        try {
            unsetGlowingMethod.invoke(glowingBlocksInstance, block, player);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private record GlowState(Set<Block> blocks, long expiresAtTick) {}
}
