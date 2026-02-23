package me.nakilex.levelplugin.spells;

import me.nakilex.levelplugin.utils.TeleportUtils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public final class SpellTargetingUtil {
    private SpellTargetingUtil() {
    }

    public static Location resolveTargetGround(Player player, double maxDistance) {
        if (player == null) {
            return null;
        }
        RayTraceResult result = player.rayTraceBlocks(maxDistance);
        if (result == null || result.getHitBlock() == null) {
            return null;
        }
        Block hitBlock = result.getHitBlock();
        Location base = hitBlock.getLocation();
        return base.add(0.5, 1.0, 0.5);
    }

    public static Location resolveSafeTeleportTarget(Player player, double maxDistance) {
        if (player == null || player.getWorld() == null || maxDistance <= 0) {
            return null;
        }
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().clone().normalize();
        Location fallback = null;

        RayTraceResult trace = player.rayTraceBlocks(maxDistance);
        if (trace != null && trace.getHitPosition() != null) {
            Location nearImpact = trace.getHitPosition().toLocation(player.getWorld())
                    .subtract(direction.clone().multiply(0.7));
            Location adjusted = findNearestSafeLocation(nearImpact, 4);
            if (adjusted != null) {
                return adjusted;
            }
        }

        BlockIterator iterator = new BlockIterator(player.getWorld(), eye.toVector(), direction, 0.0,
                (int) Math.ceil(maxDistance));
        while (iterator.hasNext()) {
            Block block = iterator.next();
            if (!block.getType().isAir() && !block.isPassable()) {
                break;
            }
            Location candidate = findNearestSafeLocation(block.getLocation().add(0.5, 1.0, 0.5), 3);
            if (candidate != null) {
                fallback = candidate;
            }
        }

        if (fallback == null) {
            Location endpoint = eye.clone().add(direction.multiply(maxDistance));
            fallback = findNearestSafeLocation(endpoint, 5);
        }
        if (fallback == null) {
            Location los = player.getLocation().clone().add(direction.multiply(Math.max(2.0, Math.min(maxDistance, 8.0))));
            fallback = findNearbySafeLocation(los, 2, 6);
        }
        return fallback;
    }

    /**
     * Resolve a practical blink destination that stops before walls and adjusts
     * vertically so the player does not spawn inside blocks.
     */
    public static Location resolveBlinkDestination(Player player, double maxDistance) {
        if (player == null || player.getWorld() == null || maxDistance <= 0) {
            return null;
        }
        Location base = TeleportUtils.resolveLineOfSightTarget(
                player,
                player.getEyeLocation().getDirection().clone().normalize(),
                maxDistance,
                0.65);
        if (base == null) {
            return null;
        }
        Location candidate = base.clone();
        candidate.setY(candidate.getY() + 0.05);
        Location safe = findNearestSafeLocation(candidate, 5);
        if (safe != null) {
            return safe;
        }
        safe = findNearbySafeLocation(candidate, 2, 6);
        if (safe != null) {
            return safe;
        }

        Location highest = resolveHighestGroundFallback(player, candidate, maxDistance);
        if (highest != null) {
            return highest;
        }
        return null;
    }

    private static Location resolveHighestGroundFallback(Player player, Location around, double maxDistance) {
        if (player == null || around == null || around.getWorld() == null) {
            return null;
        }
        World world = around.getWorld();
        int x = around.getBlockX();
        int z = around.getBlockZ();
        int highestY = world.getHighestBlockYAt(x, z);
        Location candidate = new Location(world, x + 0.5, highestY + 1.0, z + 0.5,
                player.getLocation().getYaw(), player.getLocation().getPitch());
        if (candidate.distanceSquared(player.getLocation()) > (maxDistance + 3.0) * (maxDistance + 3.0)) {
            return null;
        }
        if (!isSafeTeleportLocation(candidate)) {
            return null;
        }
        return candidate;
    }

    public static boolean isSafeTeleportLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        World world = location.getWorld();
        Block feet = world.getBlockAt(location);
        Block head = world.getBlockAt(location.clone().add(0, 1, 0));
        Block ground = world.getBlockAt(location.clone().add(0, -1, 0));
        return isPassableForPlayer(feet)
                && isPassableForPlayer(head)
                && !ground.isPassable()
                && ground.getType().isSolid();
    }

    public static Location findNearestSafeLocation(Location around, int verticalRange) {
        if (around == null || around.getWorld() == null) {
            return null;
        }
        int range = Math.max(0, verticalRange);
        for (int dy = 0; dy <= range; dy++) {
            Location down = around.clone().add(0, -dy, 0);
            if (isSafeTeleportLocation(down)) {
                return snapToCenter(down);
            }
            if (dy == 0) {
                continue;
            }
            Location up = around.clone().add(0, dy, 0);
            if (isSafeTeleportLocation(up)) {
                return snapToCenter(up);
            }
        }
        return null;
    }

    public static Location findNearbySafeLocation(Location around, int horizontalRadius, int verticalRange) {
        if (around == null || around.getWorld() == null) {
            return null;
        }
        int hz = Math.max(0, horizontalRadius);
        for (int r = 0; r <= hz; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    Location candidate = around.clone().add(dx, 0, dz);
                    Location safe = findNearestSafeLocation(candidate, verticalRange);
                    if (safe != null) {
                        return safe;
                    }
                }
            }
        }
        return null;
    }

    private static boolean isPassableForPlayer(Block block) {
        if (block == null) {
            return false;
        }
        return block.isPassable() || block.getType().isAir();
    }

    private static Location snapToCenter(Location location) {
        return new Location(location.getWorld(), location.getBlockX() + 0.5, location.getBlockY(), location.getBlockZ() + 0.5,
                location.getYaw(), location.getPitch());
    }
}
