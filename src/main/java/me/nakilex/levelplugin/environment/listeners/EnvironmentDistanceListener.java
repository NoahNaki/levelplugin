package me.nakilex.levelplugin.environment.listeners;

import me.nakilex.levelplugin.environment.EnvironmentManager;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.guild.Guild;
import me.nakilex.levelplugin.guild.GuildManager;
import me.nakilex.levelplugin.guild.siege.GuildSiegeManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;

public class EnvironmentDistanceListener implements Listener {
    private final EnvironmentManager manager;
    // Begin loading the town when players are within 350 blocks
    private static final double LOAD_DIST_SQ = 350 * 350;
    // Unload once they move beyond 400 blocks to keep a buffer
    private static final double UNLOAD_DIST_SQ = 400 * 400;
    /** Distance player must move closer before loading again. */
    private static final double RELOAD_STEP = 30.0;
    /** Stop triggering repeated loads once within this distance. */
    private static final double STOP_LOAD_DIST = 10.0;

    // Two opposite corners of the cuboid selection containing the town
    private static final int REGION_X1 = 2133;
    private static final int REGION_Y1 = -64;
    private static final int REGION_Z1 = -1138;
    private static final int REGION_X2 = 1905;
    private static final int REGION_Y2 = 69;
    private static final int REGION_Z2 = -1357;

    // Derived min/max values for easier checks
    private static final int REGION_MIN_X = Math.min(REGION_X1, REGION_X2);
    private static final int REGION_MAX_X = Math.max(REGION_X1, REGION_X2);
    private static final int REGION_MIN_Y = Math.min(REGION_Y1, REGION_Y2);
    private static final int REGION_MAX_Y = Math.max(REGION_Y1, REGION_Y2);
    private static final int REGION_MIN_Z = Math.min(REGION_Z1, REGION_Z2);
    private static final int REGION_MAX_Z = Math.max(REGION_Z1, REGION_Z2);

    private final Map<java.util.UUID, Double> lastLoadDistance = new HashMap<>();
    private final Map<java.util.UUID, Double> previousDistance = new HashMap<>();

    public EnvironmentDistanceListener(EnvironmentManager manager) {
        this.manager = manager;
    }

    private static void debug(String msg) {
        if (EnvironmentManager.isDebug()) {
            Main.getInstance().getLogger().info("[DistanceDebug] " + msg);
        }
    }

    /** Check whether a location falls inside the defined town cuboid. */
    private boolean inTownRegion(Location loc) {
        if (loc == null) return false;
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        return x >= REGION_MIN_X && x <= REGION_MAX_X
                && y >= REGION_MIN_Y && y <= REGION_MAX_Y
                && z >= REGION_MIN_Z && z <= REGION_MAX_Z;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        Location from = event.getFrom();
        if (to == null || (to.getBlockX() == from.getBlockX() && to.getBlockZ() == from.getBlockZ())) return;

        Location origin = manager.getOrigin(player.getUniqueId());
        if (origin == null) {
            debug("No origin set for " + player.getName());
            return;
        }
        if (!origin.getWorld().equals(to.getWorld())) {
            return;
        }

        // Only show town holograms to members of owning guild
        String owner = GuildSiegeManager.getInstance().getOwnerGuild();
        if (owner != null) {
            Guild g = GuildManager.getInstance().getGuild(player.getUniqueId());
            if (g == null || !owner.equalsIgnoreCase(g.getName())) {
                manager.removeAllBuildingHolograms(player.getUniqueId());
                return;
            }
        }

        double dist = to.distance(origin);

        java.util.UUID id = player.getUniqueId();
        Double prev = previousDistance.put(id, dist);

        boolean inside = inTownRegion(to);
        boolean wasInside = inTownRegion(from);

        if (inside && !wasInside) {
            debug("Player " + player.getName() + " entered town region at "
                    + to.getBlockX() + "," + to.getBlockY() + "," + to.getBlockZ());
        } else if (!inside && wasInside) {
            debug("Player " + player.getName() + " left town region");
        }


        if (dist * dist <= LOAD_DIST_SQ) {
            debug("Player " + player.getName() + " within " + dist + " blocks of town");
            if (!manager.isTownLoaded(player)) {
                if (!manager.hasPlayedInitAnimation(player)) {
                    manager.initializePlayerAnimated(player, 20);
                    manager.markAnimationPlayed(player);
                } else {
                    manager.initializePlayer(player);
                }
                manager.markTownLoaded(player, true);
                lastLoadDistance.put(id, dist);
            } else {
                Double last = lastLoadDistance.get(id);
                if (last != null && prev != null && dist < prev && last - dist >= RELOAD_STEP && dist > STOP_LOAD_DIST) {
                    manager.initializePlayer(player);
                    lastLoadDistance.put(id, dist);
                }
            }
        } else if (dist * dist > UNLOAD_DIST_SQ) {
            if (manager.isTownLoaded(player)) {
                debug("Unloading town view for " + player.getName());
                manager.unloadPlayerTown(player);
                manager.markTownLoaded(player, false);
            }
            lastLoadDistance.remove(id);
            previousDistance.remove(id);
        }
    }
}
