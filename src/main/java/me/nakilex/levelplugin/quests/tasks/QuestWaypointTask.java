package me.nakilex.levelplugin.quests.tasks;

import me.nakilex.levelplugin.quests.data.BeaconTarget;
import me.nakilex.levelplugin.quests.data.BeaconTargets;
import me.nakilex.levelplugin.quests.data.PlayerQuestProgress;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.data.QuestObjective;
import me.nakilex.levelplugin.quests.gui.QuestState;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.quests.managers.QuestWaypointManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

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
            QuestTrackingResult result = resolveTrackedTarget(player);
            if (result == null || result.location == null) {
                waypointManager.clear(player);
                continue;
            }
            if (result.location.getWorld() == null || !result.location.getWorld().equals(player.getWorld())) {
                waypointManager.clear(player);
                continue;
            }
            waypointManager.update(player, result.location, result.label);
        }
    }

    private QuestTrackingResult resolveTrackedTarget(Player player) {
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
        QuestObjective objective = null;
        int idx = 0;
        if (progress != null && quest.getId().equals(progress.getQuest().getId())) {
            for (int i = 0; i < quest.getObjectives().size(); i++) {
                if (progress.getProgress(i) < quest.getObjectives().get(i).getAmount()) {
                    idx = i;
                    break;
                }
            }
        }
        if (!quest.getObjectives().isEmpty()) {
            objective = quest.getObjectives().get(Math.min(idx, quest.getObjectives().size() - 1));
        }

        Location targetLoc = null;
        if (objective != null) {
            BeaconTarget target = objective.getBeaconTarget();
            if (target != null) {
                targetLoc = target.resolve(player);
            }
        }
        if (targetLoc == null && state == QuestState.AVAILABLE && quest.getNpcGiverId() != null) {
            BeaconTarget npcTarget = BeaconTargets.npc(quest.getNpcGiverId());
            targetLoc = npcTarget.resolve(player);
        }
        if (targetLoc == null) {
            return null;
        }
        String label = quest.getName();
        if (objective != null) {
            String objectiveText = questManager.describeObjective(objective);
            if (objectiveText != null && !objectiveText.isBlank()) {
                label = quest.getName() + " - " + objectiveText;
            }
        }
        return new QuestTrackingResult(targetLoc, label);
    }

    private static class QuestTrackingResult {
        private final Location location;
        private final String label;

        private QuestTrackingResult(Location location, String label) {
            this.location = location;
            this.label = label;
        }
    }
}
