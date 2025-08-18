package me.nakilex.levelplugin.utils;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;

import java.util.Comparator;

/** Utility methods for working with mobs and entities. */
public final class MobUtil {
    private MobUtil() {}

    /**
     * Finds the nearest hostile monster around the source entity.
     *
     * @param source reference entity
     * @param radius search radius
     * @return closest {@link LivingEntity} that is a {@link Monster}, or null if none
     */
    public static LivingEntity findNearestHostile(LivingEntity source, double radius) {
        Location loc = source.getLocation();
        return loc.getWorld().getNearbyEntities(loc, radius, radius, radius).stream()
                .filter(e -> e instanceof Monster)
                .map(e -> (LivingEntity) e)
                .min(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(loc)))
                .orElse(null);
    }
}
