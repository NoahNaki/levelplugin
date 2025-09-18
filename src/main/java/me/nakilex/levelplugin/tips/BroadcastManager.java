package me.nakilex.levelplugin.tips;

import me.nakilex.levelplugin.Main;
import org.bukkit.scheduler.BukkitTask;

public class BroadcastManager {
    private final Main plugin;
    private final TipsConfigManager cfg;
    private BukkitTask broadcastTask;
    private long delayTicks;
    private long nextRunAt;

    public BroadcastManager(Main plugin, TipsConfigManager cfg) {
        this.plugin = plugin;
        this.cfg = cfg;
    }

    /**
     * Starts or restarts the broadcast task.
     */
    public void start() {
        cancelTask();
        cfg.load();
        delayTicks = Math.max(20L, cfg.getDelaySeconds() * 20L);
        scheduleNext(0L);
        plugin.getLogger().info("[Tips] BroadcastManager started (" + cfg.getDelaySeconds() + "s interval).");
    }

    public void resetCountdown() {
        scheduleNext(delayTicks);
    }

    private void scheduleNext(long delay) {
        cancelTask();
        if (!cfg.hasTips()) {
            nextRunAt = -1L;
            return;
        }
        broadcastTask = new TipBroadcastTask(plugin, cfg, this).runTaskLater(plugin, delay);
        nextRunAt = System.currentTimeMillis() + (delay * 50L);
    }

    private void cancelTask() {
        if (broadcastTask != null) {
            broadcastTask.cancel();
            broadcastTask = null;
        }
    }

    public long getNextRunAt() {
        return nextRunAt;
    }
}
