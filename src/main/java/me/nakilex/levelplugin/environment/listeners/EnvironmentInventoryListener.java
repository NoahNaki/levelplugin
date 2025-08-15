package me.nakilex.levelplugin.environment.listeners;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.environment.EnvironmentManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

/**
 * Handles inventory interactions inside towns, preventing item loss and
 * refreshing building holograms when a player's materials change.
 */
public class EnvironmentInventoryListener implements Listener {
    private final EnvironmentManager manager;

    // Region coordinates match EnvironmentDistanceListener
    private static final int REGION_X1 = 2133;
    private static final int REGION_Y1 = -64;
    private static final int REGION_Z1 = -1138;
    private static final int REGION_X2 = 1905;
    private static final int REGION_Y2 = 69;
    private static final int REGION_Z2 = -1357;

    private static final int REGION_MIN_X = Math.min(REGION_X1, REGION_X2);
    private static final int REGION_MAX_X = Math.max(REGION_X1, REGION_X2);
    private static final int REGION_MIN_Y = Math.min(REGION_Y1, REGION_Y2);
    private static final int REGION_MAX_Y = Math.max(REGION_Y1, REGION_Y2);
    private static final int REGION_MIN_Z = Math.min(REGION_Z1, REGION_Z2);
    private static final int REGION_MAX_Z = Math.max(REGION_Z1, REGION_Z2);

    public EnvironmentInventoryListener(EnvironmentManager manager) {
        this.manager = manager;
    }

    private static boolean inTownRegion(Location loc) {
        if (loc == null) return false;
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        return x >= REGION_MIN_X && x <= REGION_MAX_X
                && y >= REGION_MIN_Y && y <= REGION_MAX_Y
                && z >= REGION_MIN_Z && z <= REGION_MAX_Z;
    }

    private void scheduleRefresh(Player player) {
        Bukkit.getScheduler().runTask(Main.getInstance(),
                () -> manager.refreshAllBuildingHolograms(player));
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        var player = event.getPlayer();
        if (manager.isTownLoaded(player) && inTownRegion(player.getLocation())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot drop inside this town.");
        }
        scheduleRefresh(player);
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            scheduleRefresh(player);
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            scheduleRefresh(player);
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            scheduleRefresh(player);
        }
    }
}
