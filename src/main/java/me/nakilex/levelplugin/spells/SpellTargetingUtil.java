package me.nakilex.levelplugin.spells;

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
        return fallback;
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
