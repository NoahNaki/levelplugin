package me.nakilex.levelplugin.quests.util;

import me.nakilex.levelplugin.quests.data.BeaconTarget;
import me.nakilex.levelplugin.quests.data.BeaconTargets;
import me.nakilex.levelplugin.quests.data.PlayerQuestProgress;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.gui.QuestState;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Utilities for resolving quest tracking locations.
 */
public final class QuestNavigationUtil {
    private QuestNavigationUtil() {
    }

    public static QuestTrackingInfo resolveTracking(Player player, QuestManager questManager) {
        if (player == null || questManager == null) {
            return null;
        }

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
        int objectiveIndex = findObjectiveIndex(quest, progress);

        BeaconTarget target = quest.getObjectives().get(objectiveIndex).getBeaconTarget();
        Location location = target != null ? target.resolve(player) : null;
        if (location == null && state == QuestState.AVAILABLE && quest.getNpcGiverId() != null) {
            location = BeaconTargets.npc(quest.getNpcGiverId()).resolve(player);
        }

        return new QuestTrackingInfo(quest, state, objectiveIndex, location);
    }

    private static int findObjectiveIndex(Quest quest, PlayerQuestProgress progress) {
        if (quest == null) {
            return 0;
        }
        if (progress == null || !quest.getId().equals(progress.getQuest().getId())) {
            return 0;
        }
        for (int i = 0; i < quest.getObjectives().size(); i++) {
            if (progress.getProgress(i) < quest.getObjectives().get(i).getAmount()) {
                return i;
            }
        }
        return 0;
    }

    public record QuestTrackingInfo(Quest quest, QuestState state, int objectiveIndex, Location location) {
    }
}
