package me.nakilex.levelplugin.items.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/**
 * Applies a short pickup protection window to player-dropped items so
 * accidental drops can't be scooped by nearby players immediately.
 */
public class ItemDropProtectionListener implements Listener {
    private static final long PROTECTION_TICKS = 200L; // 10 seconds

    private final JavaPlugin plugin;

    public ItemDropProtectionListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        Item drop = event.getItemDrop();
        UUID dropperId = event.getPlayer().getUniqueId();

        // Respect any preassigned owner (e.g., from scripted drops).
        if (drop.getOwner() != null && !dropperId.equals(drop.getOwner())) {
            return;
        }

        drop.setOwner(dropperId);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!drop.isDead()) {
                drop.setOwner(null);
            }
        }, PROTECTION_TICKS);
    }
}
