package me.nakilex.levelplugin.tips;

import me.nakilex.levelplugin.Main;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class BroadcastManager {
    private final Main plugin;
    private final TipsConfigManager cfg;
    private BukkitTask broadcastTask;
    private BukkitTask countdownTask;
    private int countdown;

    public BroadcastManager(Main plugin, TipsConfigManager cfg) {
        this.plugin = plugin;
        this.cfg = cfg;
    }

    /**
     * Starts or restarts both the broadcast and debug countdown tasks.
     */
    public void start() {
        // Cancel existing tasks
        if (broadcastTask != null && !broadcastTask.isCancelled()) broadcastTask.cancel();
        if (countdownTask != null && !countdownTask.isCancelled()) countdownTask.cancel();

        // Load config and initial countdown
        cfg.load();
        int delay = cfg.getDelaySeconds();
        countdown = delay;

        // Schedule broadcast: first run immediately, then every 'delay' seconds
        broadcastTask = new TipBroadcastTask(plugin, cfg, this)
            .runTaskTimer(plugin, 0L, delay * 20L);

        // Schedule debug countdown logger every second
        countdownTask = new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getLogger().info("[Tips] Next broadcast in " + countdown + "s");
                countdown--;
                if (countdown <= 0) countdown = cfg.getDelaySeconds();
            }
        }.runTaskTimer(plugin, 20L, 20L);

        plugin.getLogger().info("[Tips] BroadcastManager started.");
    }

    /**
     * Reset countdown to full delay (called after each broadcast).
     */
    public void resetCountdown() {
        this.countdown = cfg.getDelaySeconds();
    }
}
