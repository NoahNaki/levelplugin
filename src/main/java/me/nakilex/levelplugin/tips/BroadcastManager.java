package me.nakilex.levelplugin.tips;

import me.nakilex.levelplugin.Main;
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

        // Schedule broadcast: first run immediately, then every 'delay' seconds
        broadcastTask = new TipBroadcastTask(plugin, cfg, this)
            .runTaskTimer(plugin, 0L, delay * 30L);

        plugin.getLogger().info("[Tips] BroadcastManager started.");
    }

    /**
     * No-op: countdown removed.
     */
    public void resetCountdown() {
        // Debug countdown removed
    }
}
