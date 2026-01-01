package me.nakilex.levelplugin.player.attributes.managers;

import org.bukkit.scheduler.BukkitRunnable;

/**
 * Repeating task that calls StatsManager to handle health regeneration.
 * Runs every tick (20 times per second) by default.
 */
public class HealthRegenTask extends BukkitRunnable {
    @Override
    public void run() {
        StatsManager.getInstance().regenHealthForAllPlayers();
    }
}
