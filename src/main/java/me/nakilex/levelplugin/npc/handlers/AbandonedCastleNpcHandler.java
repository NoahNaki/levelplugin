package me.nakilex.levelplugin.npc.handlers;

import me.nakilex.levelplugin.npc.dialog.NPCDialogManager;
import me.nakilex.levelplugin.quests.def.AbandonedCastleQuest;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.gui.QuestState;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.entity.Player;

/**
 * Ensures Cedric interactions properly trigger the turn-in dialog when the Crimson Reliquary is cleared.
 */
public class AbandonedCastleNpcHandler extends AbstractQuestNpcHandler {

    public AbandonedCastleNpcHandler(QuestManager questManager, NPCDialogManager dialogManager) {
        super(AbandonedCastleQuest.ID, questManager, dialogManager);
    }

    @Override
    public boolean handle(Player player, NPC npc, Quest quest, QuestState state,
                          QuestManager questManager, NPCDialogManager dialogManager) {
        if (npc == null || npc.getId() != AbandonedCastleQuest.NPC_ID) {
            return false;
        }
        if (state == QuestState.TURN_IN_READY || AbandonedCastleQuest.handleCedricTurnIn(player, questManager, npc, dialogManager)) {
            return true;
        }

        // Prevent the generic "Complete the quest first" spam once the quest is active
        if (state == QuestState.ACCEPTED || state == QuestState.IN_PROGRESS) {
            me.nakilex.levelplugin.utils.ChatMessageUtil.send(player,
                    me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType.INFO,
                    "Cedric will debrief you once you've cleared the Crimson Reliquary.");
            return true;
        }
        return false;
    }
}
