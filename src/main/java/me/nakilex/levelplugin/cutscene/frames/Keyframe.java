package me.nakilex.levelplugin.cutscene.frames;

import me.nakilex.levelplugin.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * A frame that smoothly moves the player to a new location. If a lookAt
 * location is provided the player's view will be updated each tick to
 * keep looking at that position.
 */
public class Keyframe implements Frame {
    private final Location location;
    private final Location lookAt;
    private final long durationMs;
    private final String worldName;

    public Keyframe(Location location, Location lookAt, long durationMs, String worldName) {
        this.location = location;
        this.lookAt = lookAt;
        this.durationMs = durationMs;
        this.worldName = worldName;
    }

    @Override
    public long getDuration() {
        return durationMs;
    }

    public Location getLocation() {
        return location;
    }

    public String getWorldName() {
        return worldName;
    }

    private static float wrapAngle(float angle) {
        angle = angle % 360f;
        if (angle >= 180f) angle -= 360f;
        if (angle < -180f) angle += 360f;
        return angle;
    }

    private static float lookYaw(Location from, Location to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double yaw = Math.toDegrees(Math.atan2(-dx, dz));
        return (float) yaw;
    }

    private static float lookPitch(Location from, Location to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double dy = to.getY() - from.getY();
        double dist = Math.sqrt(dx * dx + dz * dz);
        return (float) Math.toDegrees(-Math.atan2(dy, dist));
    }

    @Override
    public org.bukkit.scheduler.BukkitTask play(Player player, Main plugin) {
        if (location == null) return null;

        Location target = location.clone();
        if (worldName != null) {
            var world = Bukkit.getWorld(worldName);
            if (world != null) {
                target.setWorld(world);
            }
        }

        Location start = player.getLocation().clone();
        long ticks = Math.max(1L, durationMs / 50L);

        double dx = target.getX() - start.getX();
        double dy = target.getY() - start.getY();
        double dz = target.getZ() - start.getZ();
        float dyaw = wrapAngle(target.getYaw() - start.getYaw());
        float dpitch = target.getPitch() - start.getPitch();

        return new BukkitRunnable() {
            long t = 0;
            Location curr = start.clone();

            @Override
            public void run() {
                if (t >= ticks) {
                    player.teleport(target);
                    cancel();
                    return;
                }

                double pct = smooth((t + 1) / (double) ticks);
                curr.setX(start.getX() + dx * pct);
                curr.setY(start.getY() + dy * pct);
                curr.setZ(start.getZ() + dz * pct);
                float yaw;
                float pitch;
                if (lookAt != null) {
                    yaw = lookYaw(curr, lookAt);
                    pitch = lookPitch(curr, lookAt);
                } else {
                    yaw = start.getYaw() + dyaw * (float) pct;
                    pitch = start.getPitch() + dpitch * (float) pct;
                }
                curr.setYaw(yaw);
                curr.setPitch(pitch);

                player.teleport(curr);
                t++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private double smooth(double t) {
        return 3 * t * t - 2 * t * t * t;
    }
}
