package me.nakilex.xprisonenchants.fx;

import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.function.IntPredicate;

/**
 * Shared helpers for the cinematic part of an area enchant: the plugin handle every animation
 * schedules against, plus the tick loop the four effects are built on.
 */
public final class Effects {

    private static volatile Plugin plugin;

    private Effects() {
    }

    public static void setPlugin(Plugin owner) {
        plugin = owner;
    }

    public static Plugin plugin() {
        return plugin;
    }

    /**
     * Runs {@code frame} once per tick until it returns {@code false} or the tick budget runs out,
     * then runs {@code onFinish} exactly once.
     *
     * <p>Every animation here is cosmetic and runs after the rewards are already settled, so it must
     * never be able to strand entities: {@code onFinish} is where echoes get cleaned up, and it runs
     * whether the effect ended naturally, hit its budget, or the task was cancelled on shutdown.
     *
     * @param maxTicks hard upper bound on the animation length
     * @param frame    given the current tick, returns {@code false} to stop early
     * @param onFinish cleanup, guaranteed to run once
     */
    public static BukkitTask animate(int maxTicks, IntPredicate frame, Runnable onFinish) {
        Plugin owner = plugin;
        if (owner == null || !owner.isEnabled()) {
            onFinish.run();
            return null;
        }
        BukkitRunnable task = new BukkitRunnable() {
            private int tick;
            private boolean finished;

            @Override
            public void run() {
                if (finished) {
                    return;
                }
                boolean keepGoing;
                try {
                    keepGoing = frame.test(tick);
                } catch (RuntimeException ex) {
                    keepGoing = false;
                    owner.getLogger().warning("Enchant animation frame failed: " + ex);
                }
                tick++;
                if (!keepGoing || tick >= maxTicks) {
                    finished = true;
                    try {
                        onFinish.run();
                    } finally {
                        cancel();
                    }
                }
            }
        };
        return task.runTaskTimer(owner, 0L, 1L);
    }

    /** Eases in quadratically: slow to start, fastest at the end. Used for falls and implosions. */
    public static double easeIn(double progress) {
        double clamped = Math.max(0.0, Math.min(1.0, progress));
        return clamped * clamped;
    }

    /** Linear interpolation between two doubles. */
    public static double lerp(double from, double to, double amount) {
        return from + (to - from) * Math.max(0.0, Math.min(1.0, amount));
    }

    /** Linear interpolation between two ints, rounded to nearest. */
    public static int lerpInt(int from, int to, double amount) {
        return (int) Math.round(lerp(from, to, amount));
    }

    /**
     * How far through its level range an enchant is, as 0.0-1.0. Level 1 of a max-10 enchant is
     * 0.0; level 10 is 1.0. Used to scale radius/count between the configured base and max.
     */
    public static double levelScale(int level, int maxLevel) {
        if (maxLevel <= 1) {
            return 1.0;
        }
        return Math.max(0.0, Math.min(1.0, (level - 1) / (double) (maxLevel - 1)));
    }

    /** Point on a horizontal circle around {@code center}. */
    public static Location orbit(Location center, double angle, double radius, double y) {
        return new Location(center.getWorld(),
                center.getX() + Math.cos(angle) * radius,
                y,
                center.getZ() + Math.sin(angle) * radius);
    }

    /** Straight-line position a fraction of the way from {@code from} to {@code to}. */
    public static Location travel(Location from, Location to, double progress) {
        Vector delta = to.toVector().subtract(from.toVector()).multiply(Math.max(0.0, Math.min(1.0, progress)));
        return from.clone().add(delta);
    }

    /** Removes every echo in the list; used as the {@code onFinish} of most animations. */
    public static Runnable cleanup(List<BlockEcho> echoes) {
        return () -> BlockEcho.removeAll(echoes);
    }
}
