package me.nakilex.levelplugin.utils;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.plugin.Plugin;

/**
 * Helpers for clearing stray Nexo furniture display entities around a location.
 */
public final class FurnitureCleanupUtil {

    private FurnitureCleanupUtil() {
    }

    public static int clearNearbyFurnitureEntities(Plugin plugin, Location target, double radiusSq, String context) {
        if (plugin == null || target == null || target.getWorld() == null) {
            return 0;
        }

        Chunk chunk = target.getChunk();
        if (chunk == null) {
            return 0;
        }

        int removed = 0;
        for (Entity entity : chunk.getEntities()) {
            if (!target.getWorld().equals(entity.getWorld())) {
                continue;
            }
            if (!(entity instanceof ItemDisplay
                    || entity instanceof BlockDisplay
                    || entity instanceof Interaction
                    || entity instanceof ArmorStand)) {
                continue;
            }

            if (entity.getLocation().distanceSquared(target) > radiusSq) {
                continue;
            }

            entity.remove();
            removed++;
        }

        if (removed > 0) {
            plugin.getLogger().info(context + " Cleared " + removed
                    + " stray display entities near location " + target);
        }

        return removed;
    }
}
