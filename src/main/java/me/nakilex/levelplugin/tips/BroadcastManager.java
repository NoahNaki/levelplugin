package me.nakilex.levelplugin.tips;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.AnnouncementTimingUtil;
import org.bukkit.scheduler.BukkitTask;

public class BroadcastManager {
    private final Main plugin;
    private final TipsConfigManager cfg;
    private BukkitTask broadcastTask;

    public BroadcastManager(Main plugin, TipsConfigManager cfg) {
        this.plugin = plugin;
        this.cfg = cfg;
    }

    /**
     * Starts or restarts the broadcast task.
     */
    public void start() {
        // Cancel existing task if running
        if (broadcastTask != null && !broadcastTask.isCancelled()) {
            broadcastTask.cancel();
        }

        // Load config
        cfg.load();
        int delay = cfg.getDelaySeconds();

        long intervalTicks = delay * 30L;
        long initialDelayTicks = AnnouncementTimingUtil.computeInitialDelayTicks(intervalTicks, 1, 3, 20L);

        // Schedule broadcast with a staggered startup offset.
        broadcastTask = new TipBroadcastTask(plugin, cfg, this)
            .runTaskTimer(plugin, initialDelayTicks, intervalTicks);

        plugin.getLogger().info("[Tips] BroadcastManager started.");
    }

    /**
     * No-op: countdown removed.
     */
    public void resetCountdown() {
        // Debug countdown removed
    }
}
