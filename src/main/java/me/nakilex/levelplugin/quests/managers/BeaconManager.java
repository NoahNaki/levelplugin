package me.nakilex.levelplugin.quests.managers;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.Listener;
import org.bukkit.Material;
import com.nexomc.nexo.api.NexoFurniture;
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.lootchests.utils.LocationUtils;
import me.nakilex.levelplugin.utils.NexoUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Quest waypoint beacon using a Nexo furniture entity.
 */
public class BeaconManager implements Listener {

    private static final String FURNITURE_ID = "base_beacon_magenta_inventory";
    private static final double BASE_HIDE_OFFSET = -0.9;

    private final Map<UUID, ItemDisplay> activeBeacons = new HashMap<>();

    /**
     * Draw a constant beacon for {@code player}.
     *
     * @param player   viewer
     * @param location centre of the block that defines the X/Z of the column
     */
    public void showBeam(Player player, Location location) {
        if (player == null || location == null) {
            return;
        }

        World world = location.getWorld();
        if (world == null) return;

        Location target = resolveBeaconLocation(location);
        if (target == null || target.getWorld() == null) {
            removeBeam(player);
            return;
        }

        ItemDisplay display = activeBeacons.get(player.getUniqueId());
        if (display == null || display.isDead() || !display.getWorld().equals(target.getWorld())) {
            if (display != null && !display.isDead()) {
                NexoFurniture.remove(display);
            }
            display = spawnBeacon(target);
            if (display != null) {
                activeBeacons.put(player.getUniqueId(), display);
            }
        } else {
            display.teleport(target);
        }
    }

    public void removeBeam(Player player) {
        if (player == null) return;
        ItemDisplay display = activeBeacons.remove(player.getUniqueId());
        if (display != null && !display.isDead()) {
            NexoFurniture.remove(display);
        }
    }

    private Location resolveBeaconLocation(Location location) {
        Location anchor = shouldCenterOnBlock(location)
                ? LocationUtils.centerOnBlock(location)
                : location.clone();
        if (anchor == null) return null;
        Location surface = LocationUtils.surfaceBelow(anchor, false);
        if (surface == null) return null;
        Location adjusted = surface.clone().add(0, 1 + BASE_HIDE_OFFSET, 0);
        World world = adjusted.getWorld();
        if (world == null) return null;
        double minY = world.getMinHeight();
        if (adjusted.getY() < minY) {
            adjusted.setY(minY);
        }
        return adjusted;
    }

    private boolean shouldCenterOnBlock(Location location) {
        double xDelta = Math.abs(location.getX() - Math.rint(location.getX()));
        double zDelta = Math.abs(location.getZ() - Math.rint(location.getZ()));
        return xDelta < 1.0e-6 && zDelta < 1.0e-6;
    }

    private ItemDisplay spawnBeacon(Location location) {
        FurnitureMechanic mechanic = NexoFurniture.furnitureMechanic(FURNITURE_ID);
        if (mechanic == null) {
            Main.getInstance().getLogger().warning("[BeaconManager] Unknown furniture '" + FURNITURE_ID + "'.");
            NexoUtil.logAvailableFurnitureIds(Main.getInstance().getLogger());
            return null;
        }
        Location spawn = location.clone().subtract(0, BASE_HIDE_OFFSET, 0);
        if (spawn.getBlock().getType() != Material.AIR) {
            spawn.getBlock().setType(Material.AIR, false);
        }
        ItemDisplay display = NexoFurniture.place(FURNITURE_ID, spawn, 0f, BlockFace.NORTH);
        if (display != null) {
            display.teleport(location);
        }
        return display;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        removeBeam(event.getPlayer());
    }
}
