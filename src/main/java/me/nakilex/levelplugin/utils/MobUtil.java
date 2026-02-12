package me.nakilex.levelplugin.utils;

import me.nakilex.levelplugin.mob.custom.CustomMobManager;
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


    /**
     * Returns true when the entity is one of our configured custom mobs.
     */
    public static boolean isCustomMob(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        if (entity.getScoreboardTags().contains(CustomMobManager.CUSTOM_MOB_TAG)) {
            return true;
        }
        return entity.hasMetadata(CustomMobManager.CUSTOM_MOB_ID_META);
    }

    /**
     * Returns true when the entity should be treated as a boss target.
     */
    public static boolean isBossLike(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        if (entity instanceof org.bukkit.entity.Boss) {
            return true;
        }
        if (entity.getScoreboardTags().contains("field_boss")) {
            return true;
        }
        return entity.getScoreboardTags().contains("dungeon_boss");
    }

    /**
     * Rotates the entity to face the given target location.
     *
     * @param entity source entity to rotate
     * @param target location to face
     */
    public static void faceEntity(LivingEntity entity, Location target) {
        Location from = entity.getEyeLocation();
        float yaw = lookYaw(from, target);
        float pitch = lookPitch(from, target);
        entity.setRotation(yaw, pitch);
    }

    /** Computes yaw from one location to another. */
    public static float lookYaw(Location from, Location to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double yaw = Math.toDegrees(Math.atan2(-dx, dz));
        return (float) yaw;
    }

    /** Computes pitch from one location to another. */
    public static float lookPitch(Location from, Location to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double dy = to.getY() - from.getY();
        double dist = Math.sqrt(dx * dx + dz * dz);
        return (float) Math.toDegrees(-Math.atan2(dy, dist));
    }

    /**
     * Resets the entity's pitch to face straight ahead while preserving its current yaw.
     *
     * @param entity entity whose head should be levelled
     */
    public static void levelHead(LivingEntity entity) {
        Location loc = entity.getLocation();
        entity.setRotation(loc.getYaw(), 0f);
    }
}
