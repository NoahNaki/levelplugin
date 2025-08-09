package me.nakilex.levelplugin.cursormenu.scheduler;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Simple abstraction layer over the server scheduler so that the
 * menu system can support both Bukkit and Folia style schedulers.
 */
public interface SchedulerAdapter {
    /**
     * Run a repeating task on the main thread.
     *
     * @param plugin owning plugin
     * @param runnable task to execute
     * @param period ticks between executions
     * @return BukkitTask representing the scheduled task
     */
    BukkitTask runRepeating(Plugin plugin, Runnable runnable, long period);
}
