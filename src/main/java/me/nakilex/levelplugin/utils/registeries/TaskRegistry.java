package me.nakilex.levelplugin.utils.registeries;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.horse.managers.HorseConfigManager;
import me.nakilex.levelplugin.horse.managers.HorseManager;
import me.nakilex.levelplugin.player.attributes.managers.ActionBarTask;
import me.nakilex.levelplugin.player.attributes.managers.HealthRegenTask;
import me.nakilex.levelplugin.player.attributes.managers.ManaRegenTask;
import me.nakilex.levelplugin.horse.utils.HorseSaverTask;
import me.nakilex.levelplugin.spells.managers.ManaCostTracker;
import me.nakilex.levelplugin.scoreboard.ScoreboardTask;
import me.nakilex.levelplugin.scoreboard.PlayerScoreboardManager;
import me.nakilex.levelplugin.quests.tasks.QuestNPCEffectTask;
import me.nakilex.levelplugin.quests.tasks.QuestBeaconTask;

public class TaskRegistry {

    public static void startTasks(Main plugin, HorseConfigManager horseConfigManager, HorseManager horseManager) {
        // Register all tasks
        new ActionBarTask().runTaskTimer(plugin, 1L, 1L);
        new HealthRegenTask().runTaskTimer(plugin, 20L, 20L);
        new ManaRegenTask().runTaskTimer(plugin, 20L, 20L);
        new HorseSaverTask(horseManager, horseConfigManager).runTaskTimer(plugin, 20L, 20L);

        PlayerScoreboardManager sbManager = plugin.getScoreboardManager();
        if (sbManager != null) {
            new ScoreboardTask(sbManager).runTaskTimer(plugin, 20L, 20L);
        }

        new QuestNPCEffectTask(plugin.getQuestManager()).runTaskTimer(plugin, 20L, 20L);
        new QuestBeaconTask(plugin.getQuestManager()).runTaskTimer(plugin, 20L, 40L);
    }
}
