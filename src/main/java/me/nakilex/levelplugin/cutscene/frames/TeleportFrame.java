package me.nakilex.levelplugin.cutscene.frames;

import me.nakilex.levelplugin.cutscene.effects.CutsceneEffects;
import me.nakilex.levelplugin.cutscene.effects.EffectSettings;
import me.nakilex.levelplugin.cutscene.playback.CutsceneContext;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class TeleportFrame implements Frame {
    private final Location location;
    private final String worldName;
    private final long durationMs;
    private final EffectSettings effects;
    /** blocks per second, <=0 means instant */
    private final double speed;

    public TeleportFrame(Location location, long durationMs, EffectSettings effects,
                         String worldName, double speed) {
        this.location = location;
        this.durationMs = durationMs;
        this.effects = effects == null ? EffectSettings.empty() : effects;
        this.worldName = worldName;
        this.speed = speed;
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

    public double getSpeed() {
        return speed;
    }

    public EffectSettings getEffects() {
        return effects;
    }

    private static float wrapAngle(float angle) {
        angle = angle % 360f;
        if (angle >= 180f) angle -= 360f;
        if (angle < -180f) angle += 360f;
        return angle;
    }

    @Override
    public org.bukkit.scheduler.BukkitTask play(CutsceneContext context) {
        Player player = context.getViewer();
        var plugin = context.getPlugin();
        if (location != null) {
            Location target = location.clone();
            if (worldName != null) {
                var world = plugin.getServer().getWorld(worldName);
                if (world != null) {
                    target.setWorld(world);
                }
            }

            if (speed > 0) {
                Location start = player.getLocation().clone();
                double distance = start.distance(target);
                double scaled = Math.pow(speed, 1.5); // amplify high speeds
                long ticks = Math.max(1L, Math.round(distance / scaled * 20.0));

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
                            CutsceneEffects.play(player, effects, target, plugin);
                            cancel();
                            return;
                        }

                        double pct = smooth((t + 1) / (double) ticks);
                        curr.setX(start.getX() + dx * pct);
                        curr.setY(start.getY() + dy * pct);
                        curr.setZ(start.getZ() + dz * pct);
                        curr.setYaw(start.getYaw() + dyaw * (float) pct);
                        curr.setPitch(start.getPitch() + dpitch * (float) pct);
                        player.teleport(curr);
                        t++;
                    }
                }.runTaskTimer(plugin, 0L, 1L);
            } else {
                player.teleport(target);
                CutsceneEffects.play(player, effects, target, plugin);
                return null;
            }
        } else {
            CutsceneEffects.play(player, effects, player.getLocation(), plugin);
            return null;
        }
    }

    private double smooth(double t) {
        return 3 * t * t - 2 * t * t * t; // smoothstep
    }

    @Override
    public Location getTargetLocation() {
        return location;
    }
}
