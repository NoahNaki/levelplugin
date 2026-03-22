package me.nakilex.levelplugin.spells;

import me.nakilex.levelplugin.utils.TeleportUtils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.function.Predicate;

public final class SpellTargetingUtil {
    private static final double MIN_BLINK_TRAVEL_DISTANCE = 2.0;

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
     * Ray trace for the first living entity along a segment.
     *
     * @param start      segment start
     * @param segment    direction/length vector from start to end
     * @param hitRadius  ray trace expansion radius
     * @param predicate  optional living-entity filter
     * @return first living entity hit, or null
     */
    public static LivingEntity rayTraceLivingEntity(Location start,
                                                    Vector segment,
                                                    double hitRadius,
                                                    Predicate<LivingEntity> predicate) {
        if (start == null || start.getWorld() == null || segment == null || segment.lengthSquared() <= 0.000001) {
            return null;
        }
        Vector direction = segment.clone().normalize();
        double distance = segment.length();
        RayTraceResult hit = start.getWorld().rayTraceEntities(
                start,
                direction,
                distance,
                Math.max(0.0, hitRadius),
                entity -> entity instanceof LivingEntity living
                        && (predicate == null || predicate.test(living)));
        if (hit == null || !(hit.getHitEntity() instanceof LivingEntity living)) {
            return null;
        }
        return living;
    }

    /**
     * Resolve a practical blink destination that stops before walls and adjusts
     * vertically so the player does not spawn inside blocks.
     */
    public static Location resolveBlinkDestination(Player player, double maxDistance) {
        if (player == null || player.getWorld() == null || maxDistance <= 0) {
            return null;
        }
        Vector direction = player.getEyeLocation().getDirection().clone().normalize();
        Location forwardSafe = resolveForwardSafeBlinkDestination(player, direction, maxDistance, MIN_BLINK_TRAVEL_DISTANCE);
        if (forwardSafe != null) {
            return forwardSafe;
        }

        Location lineTarget = TeleportUtils.resolveLineOfSightTarget(player, direction, maxDistance, 0.65);
        if (lineTarget == null) {
            return null;
        }
        Location candidate = lineTarget.clone();
        candidate.setY(candidate.getY() + 0.05);
        Location safe = findNearestSafeLocation(candidate, 5);
        if (isUsableBlinkDestination(player, safe, direction)) {
            return safe;
        }
        safe = findNearbySafeLocation(candidate, 2, 6);
        if (isUsableBlinkDestination(player, safe, direction)) {
            return safe;
        }

        Location highest = resolveHighestGroundFallback(player, candidate, maxDistance);
        if (isUsableBlinkDestination(player, highest, direction)) {
            return highest;
        }
        return null;
    }

    private static Location resolveForwardSafeBlinkDestination(Player player,
                                                               Vector direction,
                                                               double maxDistance,
                                                               double minTravelDistance) {
        if (player == null || direction == null || direction.lengthSquared() <= 0.000001 || maxDistance <= 0.0) {
            return null;
        }
        Location origin = player.getLocation().clone();
        for (double distance = maxDistance; distance >= minTravelDistance; distance -= 0.5) {
            Location probe = origin.clone().add(direction.clone().multiply(distance));
            probe.setY(probe.getY() + 0.05);
            Location safe = findNearestSafeLocation(probe, 4);
            if (isUsableBlinkDestination(player, safe, direction)) {
                return safe;
            }
        }
        return null;
    }

    private static boolean isUsableBlinkDestination(Player player, Location destination, Vector lookDirection) {
        if (player == null || destination == null || lookDirection == null) {
            return false;
        }
        Location origin = player.getLocation();
        Vector travel = destination.toVector().subtract(origin.toVector());
        if (travel.lengthSquared() < MIN_BLINK_TRAVEL_DISTANCE * MIN_BLINK_TRAVEL_DISTANCE) {
            return false;
        }
        return travel.normalize().dot(lookDirection.clone().normalize()) >= 0.45;
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
