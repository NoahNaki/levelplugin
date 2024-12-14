package me.nakilex.levelplugin.tasks;

import me.nakilex.levelplugin.managers.StatsManager;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Repeating task that calls StatsManager to handle mana regeneration.
 * Runs every second (20 ticks) by default.
 */
public class ManaRegenTask extends BukkitRunnable {
    @Override
    public void run() {
        StatsManager.getInstance().regenManaForAllPlayers();
    }
}
