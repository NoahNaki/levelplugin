package me.nakilex.levelplugin.spells.utils.animation;

import me.nakilex.levelplugin.Main;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Simplified animation helper inspired by MagicSpells' SpellAnimation.
 * Runs a task for a set number of ticks at a given interval.
 */
public abstract class SpellAnimation extends BukkitRunnable {
    private final int maxTicks;
    private int tick;

    /**
     * @param interval  run interval in ticks
     * @param duration  total run duration in ticks
     */
    public SpellAnimation(int interval, int duration) {
        this.maxTicks = duration;
        this.tick = 0;
        runTaskTimer(Main.getInstance(), 0L, interval);
    }

    @Override
    public void run() {
        if (tick >= maxTicks) {
            cancel();
            onEnd();
            return;
        }
        onTick(tick++);
    }

    protected void onEnd() {}
    protected abstract void onTick(int tick);
}
