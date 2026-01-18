package me.nakilex.levelplugin.npc.handlers;

import me.nakilex.levelplugin.npc.dialog.NPCDialogManager;
import me.nakilex.levelplugin.quests.def.AbandonedCastleQuest;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.gui.QuestState;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import org.bukkit.entity.Player;

/**
 * Ensures Cedric interactions properly trigger the turn-in dialog when the Crimson Reliquary is cleared.
 */
public class AbandonedCastleNpcHandler extends AbstractQuestNpcHandler {

    public AbandonedCastleNpcHandler(QuestManager questManager, NPCDialogManager dialogManager) {
        super(AbandonedCastleQuest.ID, questManager, dialogManager);
    }

    @Override
    public boolean handle(Player player, me.nakilex.levelplugin.npc.system.NPC npc,
                          net.citizensnpcs.api.npc.NPC citizensNpc,
                          Quest quest, QuestState state,
                          QuestManager questManager, NPCDialogManager dialogManager) {
        int npcId = getNpcId(npc, citizensNpc);
        if (npcId != AbandonedCastleQuest.NPC_ID) {
            return false;
        }
        if (AbandonedCastleQuest.isReadyForTurnIn(player, questManager)) {
            me.nakilex.levelplugin.Main.getInstance().getLogger().info("[CedricTurnIn] Handler invoked for " + player.getName() + " state=" + state);
            boolean handled = npc != null
                    ? AbandonedCastleQuest.handleCedricTurnIn(player, questManager, npc, dialogManager)
                    : AbandonedCastleQuest.handleCedricTurnIn(player, questManager, citizensNpc, dialogManager);
            if (handled) {
                return true;
            }
        }
        if (state == QuestState.TURN_IN_READY) {
            return true;
        }

        // Prevent the generic "Complete the quest first" spam once the quest is active
        if (state == QuestState.ACCEPTED || state == QuestState.IN_PROGRESS) {
            me.nakilex.levelplugin.quests.data.PlayerQuestProgress progress =
                    questManager.getProgress(player.getUniqueId(), AbandonedCastleQuest.ID);
            if (progress != null && quest != null) {
                int needed = quest.getObjectives().get(0).getAmount();
                if (progress.getProgress(0) < needed) {
                    startDialog(player,
                            quest.getDialogLines(),
                            npc,
                            citizensNpc,
                            () -> questManager.handleTalk(player, AbandonedCastleQuest.INTRO_TARGET));
                    return true;
                }
            }
            me.nakilex.levelplugin.utils.ChatMessageUtil.send(player,
                    me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType.INFO,
                    "Cedric will debrief you once you've cleared the Crimson Reliquary.");
            return true;
        }
        return false;
    }
}
