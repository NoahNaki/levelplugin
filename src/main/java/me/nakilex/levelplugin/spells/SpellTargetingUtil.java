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
        BlockIterator iterator = new BlockIterator(player.getWorld(), eye.toVector(), direction, 0.0, (int) Math.ceil(maxDistance));
        while (iterator.hasNext()) {
            Block block = iterator.next();
            if (!block.getType().isAir() && !block.isPassable()) {
                break;
            }
            Location candidate = block.getLocation().add(0.5, 1.0, 0.5);
            if (isSafeTeleportLocation(candidate)) {
                fallback = candidate;
            }
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
        return feet.isPassable()
                && head.isPassable()
                && !ground.isPassable()
                && ground.getType().isSolid();
    }
}
