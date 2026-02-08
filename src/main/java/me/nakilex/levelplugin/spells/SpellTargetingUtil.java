package me.nakilex.levelplugin.spells;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

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
}
