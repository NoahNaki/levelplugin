package me.nakilex.levelplugin.cursormenu.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Default scheduler adapter using the standard Bukkit scheduler.
 */
public class BukkitSchedulerAdapter implements SchedulerAdapter {
    @Override
    public BukkitTask runRepeating(Plugin plugin, Runnable runnable, long period) {
        return Bukkit.getScheduler().runTaskTimer(plugin, runnable, 1L, period);
    }
}
