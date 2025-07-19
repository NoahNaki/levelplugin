package me.nakilex.levelplugin.environment.listeners;

import me.nakilex.levelplugin.environment.EnvironmentManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;

/** Prevents lost items when dropping them onto fake blocks in towns. */
public class TownItemDropListener implements Listener {
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

    public TownItemDropListener(EnvironmentManager manager) {
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

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        var player = event.getPlayer();
        if (manager.isTownLoaded(player) && inTownRegion(player.getLocation())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot drop inside this town.");
        }
    }
}
