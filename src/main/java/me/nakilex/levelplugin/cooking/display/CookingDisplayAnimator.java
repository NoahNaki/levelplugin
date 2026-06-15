package me.nakilex.levelplugin.cooking.display;

import me.nakilex.levelplugin.Main;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/** Reusable pulse/hover animator for cooking item displays. */
public class CookingDisplayAnimator {
    private static final long PERIOD_TICKS = 2L;

    private final Main plugin;
    private final Set<AnimatedDisplay> displays = new HashSet<>();
    private BukkitTask task;

    public CookingDisplayAnimator(Main plugin) {
        this.plugin = plugin;
    }

    public AnimatedDisplay animate(ItemDisplay display, Location baseLocation, float baseScale,
                                   double pulseAmplitude, double hoverAmplitude,
                                   double pulseSpeed, double hoverSpeed) {
        if (display == null || baseLocation == null) {
            return null;
        }
        AnimatedDisplay animated = new AnimatedDisplay(display, baseLocation.clone(), baseScale, pulseAmplitude,
                hoverAmplitude, pulseSpeed, hoverSpeed, plugin.getServer().getCurrentTick());
        displays.add(animated);
        ensureRunning();
        return animated;
    }

    public void stop(AnimatedDisplay animated) {
        if (animated == null) return;
        displays.remove(animated);
        stopIfIdle();
    }

    public void stopAll() {
        displays.clear();
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void shrinkAndRemove(AnimatedDisplay animated) {
        if (animated == null) return;
        stop(animated);
        ItemDisplay display = animated.display();
        if (display == null || !display.isValid()) return;
        final int[] tick = {0};
        plugin.getServer().getScheduler().runTaskTimer(plugin, task -> {
            if (!display.isValid()) {
                task.cancel();
                return;
            }
            if (tick[0] >= 6) {
                display.remove();
                task.cancel();
                return;
            }
            applyScale(display, animated.baseScale() * (1.0f - (tick[0] / 6.0f)));
            tick[0]++;
        }, 0L, 1L);
    }

    private void ensureRunning() {
        if (task != null || displays.isEmpty()) return;
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 0L, PERIOD_TICKS);
    }

    private void stopIfIdle() {
        if (displays.isEmpty() && task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        long currentTick = plugin.getServer().getCurrentTick();
        Iterator<AnimatedDisplay> iterator = displays.iterator();
        while (iterator.hasNext()) {
            AnimatedDisplay animated = iterator.next();
            ItemDisplay display = animated.display();
            if (display == null || !display.isValid()) {
                iterator.remove();
                continue;
            }
            long time = currentTick - animated.startTick();
            double pulse = 1.0D + Math.sin(time * animated.pulseSpeed()) * animated.pulseAmplitude();
            double hover = Math.sin(time * animated.hoverSpeed()) * animated.hoverAmplitude();
            applyScale(display, (float) (animated.baseScale() * pulse));
            display.teleport(animated.baseLocation().clone().add(0.0D, hover, 0.0D));
        }
        stopIfIdle();
    }

    private static void applyScale(Display display, float scale) {
        display.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(),
                new Vector3f(scale, scale, scale), new AxisAngle4f()));
    }

    public record AnimatedDisplay(ItemDisplay display, Location baseLocation, float baseScale,
                                  double pulseAmplitude, double hoverAmplitude,
                                  double pulseSpeed, double hoverSpeed, long startTick) {}
}
