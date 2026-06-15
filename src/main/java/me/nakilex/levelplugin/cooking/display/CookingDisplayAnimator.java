package me.nakilex.levelplugin.cooking.display;

import me.nakilex.levelplugin.Main;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Reusable scale animator for cooking item displays. */
public class CookingDisplayAnimator {
    private static final float INITIAL_SCALE = 0.05f;

    private final Main plugin;
    private final Map<UUID, BukkitTask> activeTasks = new HashMap<>();

    public CookingDisplayAnimator(Main plugin) {
        this.plugin = plugin;
    }

    public AnimatedDisplay animateIn(ItemDisplay display, float targetScale, float step) {
        if (display == null) {
            return null;
        }
        float safeTarget = Math.max(INITIAL_SCALE, targetScale);
        applyScale(display, INITIAL_SCALE);
        replaceTask(display, plugin.getServer().getScheduler().runTaskTimer(plugin,
                new ScaleInTask(display, safeTarget, Math.max(0.01f, step)), 0L, 1L));
        return new AnimatedDisplay(display, safeTarget);
    }

    public void stop(AnimatedDisplay animated) {
        if (animated == null || animated.display() == null) {
            return;
        }
        cancelTask(animated.display());
    }

    public void stopAll() {
        for (BukkitTask task : activeTasks.values()) {
            task.cancel();
        }
        activeTasks.clear();
    }

    public void scaleOutAndRemove(AnimatedDisplay animated, float multiplier, float minimumScale) {
        if (animated == null) return;
        ItemDisplay display = animated.display();
        if (display == null || !display.isValid()) return;
        replaceTask(display, plugin.getServer().getScheduler().runTaskTimer(plugin,
                new ScaleOutTask(display, Math.max(0.01f, multiplier), Math.max(0.0f, minimumScale)), 0L, 1L));
    }

    private void replaceTask(ItemDisplay display, BukkitTask task) {
        cancelTask(display);
        activeTasks.put(display.getUniqueId(), task);
    }

    private void cancelTask(ItemDisplay display) {
        if (display == null) {
            return;
        }
        BukkitTask previous = activeTasks.remove(display.getUniqueId());
        if (previous != null) {
            previous.cancel();
        }
    }

    private float currentScale(Display display) {
        Transformation transformation = display.getTransformation();
        return transformation == null ? INITIAL_SCALE : transformation.getScale().x;
    }

    private static void applyScale(Display display, float scale) {
        display.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(),
                new Vector3f(scale, scale, scale), new AxisAngle4f()));
    }

    private final class ScaleInTask implements Runnable {
        private final ItemDisplay display;
        private final float targetScale;
        private final float step;

        private ScaleInTask(ItemDisplay display, float targetScale, float step) {
            this.display = display;
            this.targetScale = targetScale;
            this.step = step;
        }

        @Override
        public void run() {
            if (!display.isValid()) {
                cancelTask(display);
                return;
            }
            float nextScale = Math.min(targetScale, currentScale(display) + step);
            applyScale(display, nextScale);
            if (nextScale >= targetScale) {
                cancelTask(display);
            }
        }
    }

    private final class ScaleOutTask implements Runnable {
        private final ItemDisplay display;
        private final float multiplier;
        private final float minimumScale;

        private ScaleOutTask(ItemDisplay display, float multiplier, float minimumScale) {
            this.display = display;
            this.multiplier = multiplier;
            this.minimumScale = minimumScale;
        }

        @Override
        public void run() {
            if (!display.isValid()) {
                cancelTask(display);
                return;
            }
            float nextScale = currentScale(display) * multiplier;
            if (nextScale <= minimumScale) {
                display.remove();
                cancelTask(display);
                return;
            }
            applyScale(display, nextScale);
        }
    }

    public record AnimatedDisplay(ItemDisplay display, float targetScale) {}
}
