package me.nakilex.levelplugin.quests.managers;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.FluidCollisionMode;
import org.bukkit.block.Block;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.Listener;
import org.bukkit.Material;
import org.bukkit.util.RayTraceResult;
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
    private static final double MIN_PLAYER_DISTANCE = 40.0;

    private final Map<UUID, ItemDisplay> activeBeacons = new HashMap<>();
    private final Map<UUID, Long> debugThrottle = new HashMap<>();

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

        Location target = resolveBeaconLocation(player, location);
        if (target == null || target.getWorld() == null) {
            debug(player, "Resolved beacon location is null for target "
                    + LocationUtils.blockLocationString(location));
            removeBeam(player);
            return;
        }

        debugLineOfSight(player, target);

        Location clampedTarget = enforceMinimumDistance(player, target, location);
        if (clampedTarget != null) {
            target = clampedTarget;
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

    public void removeAll() {
        for (ItemDisplay display : activeBeacons.values()) {
            if (display != null && !display.isDead()) {
                NexoFurniture.remove(display);
            }
        }
        activeBeacons.clear();
    }

    private Location resolveBeaconLocation(Player player, Location location) {
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
        Location resolved = LocationUtils.firstAirAbove(adjusted, 6);
        if (resolved != null && resolved.getBlock().getType().isAir()) {
            Location losAdjusted = adjustForObstruction(player, resolved);
            if (losAdjusted != null) {
                return losAdjusted;
            }
            return resolved;
        }
        Location surfaceFallback = LocationUtils.aboveSurface(anchor);
        if (surfaceFallback != null) {
            Location fallbackAdjusted = surfaceFallback.clone().add(0, 1 + BASE_HIDE_OFFSET, 0);
            Location fallbackResolved = LocationUtils.firstAirAbove(fallbackAdjusted, 12);
            if (fallbackResolved != null) {
                Location losAdjusted = adjustForObstruction(player, fallbackResolved);
                if (losAdjusted != null) {
                    return losAdjusted;
                }
                return fallbackResolved;
            }
        }
        debug(player, "Beacon placement still inside block for target "
                + LocationUtils.blockLocationString(location)
                + " resolved=" + LocationUtils.blockLocationString(resolved)
                + " adjusted=" + LocationUtils.blockLocationString(adjusted));
        return resolved;
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
            display.setTeleportDuration(2);
            display.teleport(location);
        }
        return display;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        removeBeam(event.getPlayer());
    }

    private void debug(Player player, String message) {
        if (!isDebugEnabled() || player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        long last = debugThrottle.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < 5000L) {
            return;
        }
        debugThrottle.put(player.getUniqueId(), now);
        Main.getInstance().getLogger().info("[BeaconManager] " + player.getName() + ": " + message);
    }

    private void debugLineOfSight(Player player, Location target) {
        if (!isDebugEnabled() || player == null || target == null || target.getWorld() == null) {
            return;
        }
        Location eye = player.getEyeLocation();
        if (!eye.getWorld().equals(target.getWorld())) {
            debug(player, "Beacon target is in different world: " + target.getWorld().getName());
            return;
        }
        double distance = eye.distance(target);
        if (distance < 0.1) {
            return;
        }
        RayTraceResult trace = eye.getWorld().rayTraceBlocks(
                eye,
                target.toVector().subtract(eye.toVector()).normalize(),
                distance,
                FluidCollisionMode.NEVER,
                true);
        if (trace != null && trace.getHitBlock() != null) {
            Block hit = trace.getHitBlock();
            debug(player, "Line of sight blocked by " + hit.getType()
                    + " at " + LocationUtils.blockLocationString(hit.getLocation())
                    + " target=" + LocationUtils.blockLocationString(target));
        }
    }

    private Location adjustForObstruction(Player player, Location target) {
        if (player == null || target == null || target.getWorld() == null) {
            return null;
        }
        Location eye = player.getEyeLocation();
        if (!eye.getWorld().equals(target.getWorld())) {
            return null;
        }
        double distance = eye.distance(target);
        if (distance < 0.1) {
            return null;
        }
        RayTraceResult trace = eye.getWorld().rayTraceBlocks(
                eye,
                target.toVector().subtract(eye.toVector()).normalize(),
                distance,
                FluidCollisionMode.NEVER,
                true);
        if (trace == null || trace.getHitBlock() == null) {
            return null;
        }
        Block hit = trace.getHitBlock();
        Location hitLoc = hit.getLocation().add(0, 1 + BASE_HIDE_OFFSET, 0);
        Location aboveHit = LocationUtils.firstAirAbove(hitLoc, 12);
        if (aboveHit != null && aboveHit.getBlock().getType().isAir()) {
            return aboveHit;
        }
        Location surface = LocationUtils.aboveSurface(hit.getLocation());
        if (surface == null) {
            return null;
        }
        Location surfaceAdjusted = surface.clone().add(0, 1 + BASE_HIDE_OFFSET, 0);
        Location surfaceResolved = LocationUtils.firstAirAbove(surfaceAdjusted, 12);
        if (surfaceResolved != null && surfaceResolved.getBlock().getType().isAir()) {
            return surfaceResolved;
        }
        return null;
    }

    private Location enforceMinimumDistance(Player player, Location target, Location destination) {
        if (player == null || target == null || target.getWorld() == null || destination == null) {
            return null;
        }
        Location playerLoc = player.getLocation();
        if (!playerLoc.getWorld().equals(target.getWorld()) || !playerLoc.getWorld().equals(destination.getWorld())) {
            return null;
        }
        double destDistance = playerLoc.distance(destination);
        if (destDistance <= MIN_PLAYER_DISTANCE) {
            return null;
        }
        double distance = playerLoc.distance(target);
        if (distance >= MIN_PLAYER_DISTANCE || distance < 0.1) {
            return null;
        }
        return playerLoc.clone()
                .add(target.toVector().subtract(playerLoc.toVector()).normalize().multiply(MIN_PLAYER_DISTANCE));
    }

    private boolean isDebugEnabled() {
        Main plugin = Main.getInstance();
        return plugin != null
                && plugin.getCustomConfig() != null
                && plugin.getCustomConfig().getBoolean("debug.beacon-entity", false);
    }
}
