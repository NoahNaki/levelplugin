package me.nakilex.levelplugin.utils.registeries;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.horse.managers.HorseConfigManager;
import me.nakilex.levelplugin.player.attributes.managers.ActionBarTask;
import me.nakilex.levelplugin.player.attributes.managers.HealthRegenTask;
import me.nakilex.levelplugin.player.attributes.managers.ManaRegenTask;
import me.nakilex.levelplugin.horse.utils.HorseSaverTask;

public class TaskRegistry {

    public static void startTasks(Main plugin, HorseConfigManager horseConfigManager) {
        // Register all tasks
        new ActionBarTask().runTaskTimer(plugin, 1L, 1L);
        new HealthRegenTask().runTaskTimer(plugin, 20L, 20L);
        new ManaRegenTask().runTaskTimer(plugin, 20L, 20L);
        new HorseSaverTask(horseConfigManager).runTaskTimer(plugin, 6000L, 6000L);
    }
}
