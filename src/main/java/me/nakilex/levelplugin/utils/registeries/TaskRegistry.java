package me.nakilex.levelplugin.utils.registeries;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.horse.managers.HorseConfigManager;
import me.nakilex.levelplugin.horse.managers.HorseManager;
import me.nakilex.levelplugin.player.attributes.managers.ActionBarTask;
import me.nakilex.levelplugin.player.attributes.managers.HealthRegenTask;
import me.nakilex.levelplugin.player.attributes.managers.ManaRegenTask;
import me.nakilex.levelplugin.horse.utils.HorseSaverTask;
import me.nakilex.levelplugin.scoreboard.ScoreboardTask;
import me.nakilex.levelplugin.scoreboard.PlayerScoreboardManager;
import me.nakilex.levelplugin.quests.tasks.QuestNPCEffectTask;
import me.nakilex.levelplugin.quests.tasks.QuestBeaconTask;
import me.nakilex.levelplugin.quests.tasks.QuestPlayTimeTask;
import me.nakilex.levelplugin.quests.managers.BeaconManager;
import me.nakilex.levelplugin.world.LeafParticleTask;
import me.nakilex.levelplugin.leaderboards.LeaderboardUpdateTask;
import me.nakilex.levelplugin.leaderboards.LeaderboardManager;
import org.bukkit.entity.Player;

public class TaskRegistry {

    private static QuestNPCEffectTask questNpcTask;

    public static void startTasks(Main plugin,
                                  HorseConfigManager horseConfigManager,
                                  HorseManager horseManager,
                                  me.nakilex.levelplugin.npc.wandering.WanderingMerchantManager merchantManager) {
        // Register all tasks
        new ActionBarTask(plugin).runTaskTimer(plugin, 1L, 1L);
        new HealthRegenTask().runTaskTimer(plugin, 20L, 20L);
        new ManaRegenTask().runTaskTimer(plugin, 20L, 20L);
        new HorseSaverTask(horseManager, horseConfigManager).runTaskTimer(plugin, 20L, 20L);

        PlayerScoreboardManager sbManager = plugin.getScoreboardManager();
        if (sbManager != null) {
            // update scoreboard every 2 seconds instead of each second
            new ScoreboardTask(sbManager).runTaskTimer(plugin, 40L, 40L);
        }

        LeaderboardManager lbManager = plugin.getLeaderboardManager();
        if (lbManager != null) {
            new LeaderboardUpdateTask(lbManager).runTaskTimer(plugin, 200L, 200L);
        }

        new LeafParticleTask(plugin).runTaskTimer(plugin, 20L, 20L);

        questNpcTask = new QuestNPCEffectTask(plugin.getQuestManager());
        questNpcTask.runTaskTimer(plugin, 20L, 20L);
        BeaconManager beaconMgr = plugin.getBeaconManager();
        new QuestBeaconTask(plugin.getQuestManager(), beaconMgr).runTaskTimer(plugin, 10L, 20L);
        new QuestPlayTimeTask(plugin.getQuestManager()).runTaskTimer(plugin, 1200L, 1200L);

        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (merchantManager.isActive()) return;
                if (System.currentTimeMillis() - merchantManager.getLastSpawn() < 20 * 60 * 1000L) return;
                Player target = plugin.getServer().getOnlinePlayers().stream().findAny().orElse(null);
                if (target != null) merchantManager.spawnNear(target);
            }
        }.runTaskTimer(plugin, 1200L, 1200L);
    }

    public static void stopTasks() {
        if (questNpcTask != null) {
            questNpcTask.cancel();
            questNpcTask.clearGlyphs();
            questNpcTask = null;
        }
    }
}
