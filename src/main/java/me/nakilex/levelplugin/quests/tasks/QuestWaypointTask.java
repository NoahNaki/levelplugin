package me.nakilex.levelplugin.quests.tasks;

import me.nakilex.levelplugin.quests.data.BeaconTarget;
import me.nakilex.levelplugin.quests.data.BeaconTargets;
import me.nakilex.levelplugin.quests.data.PlayerQuestProgress;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.data.QuestObjective;
import me.nakilex.levelplugin.quests.gui.QuestState;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.quests.managers.QuestWaypointManager;
import me.nakilex.levelplugin.quests.pathfinding.TrailPathfinder.PathTarget;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Updates quest waypoint indicators on a short interval.
 */
public class QuestWaypointTask extends BukkitRunnable {

    private final QuestManager questManager;
    private final QuestWaypointManager waypointManager;

    public QuestWaypointTask(QuestManager questManager, QuestWaypointManager waypointManager) {
        this.questManager = questManager;
        this.waypointManager = waypointManager;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            QuestTarget resolved = resolveQuestTarget(player);
            if (resolved == null || resolved.location == null) {
                waypointManager.clearIndicators(player);
                continue;
            }
            PathTarget target = PathTarget.of(resolved.location);
            waypointManager.updateIndicators(player, target, resolved.questName, resolved.objectiveText);
        }
    }

    private QuestTarget resolveQuestTarget(Player player) {
        PlayerQuestProgress progress = questManager.getProgress(player.getUniqueId());
        Quest quest = progress != null ? progress.getQuest() : null;

        String tracked = questManager.getTrackedQuest(player.getUniqueId());
        if (tracked != null && (quest == null || !quest.getId().equals(tracked))) {
            quest = questManager.getQuest(tracked);
        }

        if (quest == null) {
            return null;
        }

        QuestState state = questManager.getQuestState(player, quest);
        QuestObjective objective = pickObjective(progress, quest);

        Location loc = null;
        if (objective != null) {
            BeaconTarget target = objective.getBeaconTarget();
            if (target != null) {
                loc = target.resolve(player);
            }
        }
        if (loc == null && state == QuestState.AVAILABLE && quest.getNpcGiverId() != null) {
            BeaconTarget npcTarget = BeaconTargets.npc(quest.getNpcGiverId());
            loc = npcTarget.resolve(player);
        }

        if (loc == null) {
            return null;
        }

        String objectiveText = objective != null ? questManager.describeObjective(objective) : null;
        return new QuestTarget(loc, quest.getName(), objectiveText);
    }

    private QuestObjective pickObjective(PlayerQuestProgress progress, Quest quest) {
        if (quest == null || quest.getObjectives().isEmpty()) {
            return null;
        }
        if (progress != null && quest.getId().equals(progress.getQuest().getId())) {
            for (int i = 0; i < quest.getObjectives().size(); i++) {
                if (progress.getProgress(i) < quest.getObjectives().get(i).getAmount()) {
                    return quest.getObjectives().get(i);
                }
            }
        }
        return quest.getObjectives().get(0);
    }

    private static class QuestTarget {
        private final Location location;
        private final String questName;
        private final String objectiveText;

        private QuestTarget(Location location, String questName, String objectiveText) {
            this.location = location;
            this.questName = questName;
            this.objectiveText = objectiveText;
        }
    }
}
