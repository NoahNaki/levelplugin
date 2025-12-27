package me.nakilex.levelplugin.quests.tasks;

import me.nakilex.levelplugin.quests.data.*;
import me.nakilex.levelplugin.quests.gui.QuestState;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.utils.ChatUtil;
import me.nakilex.levelplugin.waypoints.WaypointDisplayManager;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs once a tick to keep the rectangular lime beacon “solid”.
 */
public class QuestBeaconTask extends BukkitRunnable {

    private final QuestManager  questManager;
    private final WaypointDisplayManager waypointDisplayManager;

    public QuestBeaconTask(QuestManager questManager, WaypointDisplayManager waypointDisplayManager) {
        this.questManager = questManager;
        this.waypointDisplayManager = waypointDisplayManager;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {

            // --- pick quest -------------------------------------------------
            PlayerQuestProgress progress = questManager.getProgress(player.getUniqueId());
            Quest quest = progress != null ? progress.getQuest() : null;

            String tracked = questManager.getTrackedQuest(player.getUniqueId());
            if (tracked != null && (quest == null || !quest.getId().equals(tracked))) {
                quest = questManager.getQuest(tracked);
            }

            // --- pick objective ---------------------------------------------
            Location loc = null;
            QuestState state = null;
            String questName = null;
            String objectiveLabel = null;
            if (quest != null && quest.isLocationVisible()) {
                state = questManager.getQuestState(player, quest);
                questName = quest.getName();
                int idx = 0;
                if (progress != null && quest.getId().equals(progress.getQuest().getId())) {
                    // first unfinished objective
                    for (int i = 0; i < quest.getObjectives().size(); i++) {
                        if (progress.getProgress(i) < quest.getObjectives().get(i).getAmount()) {
                            idx = i;
                            break;
                        }
                    }
                }
                QuestObjective objective = quest.getObjectives().get(idx);
                BeaconTarget target = objective.getBeaconTarget();
                if (target != null) {
                    loc = target.resolve(player);
                }
                objectiveLabel = objective.getDescription();
                if (loc == null && state == QuestState.AVAILABLE && quest.getNpcGiverId() != null) {
                    BeaconTarget npcTarget = BeaconTargets.npc(quest.getNpcGiverId());
                    loc = npcTarget.resolve(player);
                    objectiveLabel = "Speak with the quest giver";
                }
            }

            if (loc != null && loc.getWorld() != null && loc.getWorld().equals(player.getWorld())) {
                waypointDisplayManager.update(player, loc, buildHologramLines(questName, objectiveLabel));
            } else {
                waypointDisplayManager.clear(player);
            }
        }
    }

    private List<String> buildHologramLines(String questName, String objectiveLabel) {
        List<String> lines = new ArrayList<>();
        if (questName != null && !questName.isBlank()) {
            lines.add(ChatColor.GOLD + ChatUtil.applyEmojis(questName));
        }
        if (objectiveLabel != null && !objectiveLabel.isBlank()) {
            lines.add(ChatColor.YELLOW + ChatUtil.applyEmojis(objectiveLabel));
        }
        return lines;
    }
}
