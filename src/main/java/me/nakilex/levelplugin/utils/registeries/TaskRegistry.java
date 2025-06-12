package me.nakilex.levelplugin.utils.registeries;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.horse.managers.HorseConfigManager;
import me.nakilex.levelplugin.horse.managers.HorseManager;
import me.nakilex.levelplugin.player.attributes.managers.ActionBarTask;
import me.nakilex.levelplugin.player.attributes.managers.HealthRegenTask;
import me.nakilex.levelplugin.player.attributes.managers.ManaRegenTask;
import me.nakilex.levelplugin.player.attributes.managers.StaminaTask;
import me.nakilex.levelplugin.horse.utils.HorseSaverTask;
import me.nakilex.levelplugin.spells.managers.ManaCostTracker;
import me.nakilex.levelplugin.scoreboard.ScoreboardTask;
import me.nakilex.levelplugin.scoreboard.PlayerScoreboardManager;
import me.nakilex.levelplugin.quests.tasks.QuestNPCEffectTask;
import me.nakilex.levelplugin.quests.tasks.QuestBeaconTask;
import me.nakilex.levelplugin.quests.managers.BeaconManager;
import me.nakilex.levelplugin.world.LeafParticleTask;
import me.nakilex.levelplugin.leaderboards.LeaderboardUpdateTask;
import me.nakilex.levelplugin.leaderboards.LeaderboardManager;

public class TaskRegistry {

    public static void startTasks(Main plugin, HorseConfigManager horseConfigManager, HorseManager horseManager) {
        // Register all tasks
        new ActionBarTask().runTaskTimer(plugin, 1L, 1L);
        new HealthRegenTask().runTaskTimer(plugin, 20L, 20L);
        new ManaRegenTask().runTaskTimer(plugin, 20L, 20L);
        new StaminaTask().runTaskTimer(plugin, 2L, 2L);
        new HorseSaverTask(horseManager, horseConfigManager).runTaskTimer(plugin, 20L, 20L);

        PlayerScoreboardManager sbManager = plugin.getScoreboardManager();
        if (sbManager != null) {
            new ScoreboardTask(sbManager).runTaskTimer(plugin, 20L, 20L);
        }

        LeaderboardManager lbManager = plugin.getLeaderboardManager();
        if (lbManager != null) {
            new LeaderboardUpdateTask(lbManager).runTaskTimer(plugin, 200L, 200L);
        }

        new LeafParticleTask(plugin).runTaskTimer(plugin, 20L, 20L);

        new QuestNPCEffectTask(plugin.getQuestManager()).runTaskTimer(plugin, 20L, 20L);
        BeaconManager beaconMgr = plugin.getBeaconManager();
        new QuestBeaconTask(plugin.getQuestManager(), beaconMgr).runTaskTimer(plugin, 10L, 20L);
    }
}
