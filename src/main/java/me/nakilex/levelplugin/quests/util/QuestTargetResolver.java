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
 * Resolve the active quest target location for a player, respecting tracked quests and objectives.
 */
public final class QuestTargetResolver {

    private QuestTargetResolver() {
    }

    public static QuestTarget resolve(Player player, QuestManager questManager) {
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
        int idx = 0;
        if (progress != null && quest.getId().equals(progress.getQuest().getId())) {
            for (int i = 0; i < quest.getObjectives().size(); i++) {
                if (progress.getProgress(i) < quest.getObjectives().get(i).getAmount()) {
                    idx = i;
                    break;
                }
            }
        }

        Location loc = null;
        BeaconTarget target = quest.getObjectives().get(idx).getBeaconTarget();
        if (target != null) {
            loc = target.resolve(player);
        }
        if (loc == null && state == QuestState.AVAILABLE && quest.getNpcGiverId() != null) {
            BeaconTarget npcTarget = BeaconTargets.npc(quest.getNpcGiverId());
            loc = npcTarget.resolve(player);
        }

        if (loc == null) {
            return null;
        }

        return new QuestTarget(quest, state, loc);
    }

    public record QuestTarget(Quest quest, QuestState state, Location location) {
    }
}
