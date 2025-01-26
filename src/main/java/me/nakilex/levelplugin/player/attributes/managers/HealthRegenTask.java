package me.nakilex.levelplugin.player.attributes.managers;

import org.bukkit.scheduler.BukkitRunnable;

/**
 * Repeating task that calls StatsManager to handle mana regeneration.
 * Runs every second (20 ticks) by default.
 */
public class HealthRegenTask extends BukkitRunnable {
    @Override
    public void run() {
        StatsManager.getInstance().regenHealthForAllPlayers();
    }
}
