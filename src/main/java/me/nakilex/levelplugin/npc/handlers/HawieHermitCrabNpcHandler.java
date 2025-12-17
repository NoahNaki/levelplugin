package me.nakilex.levelplugin.npc.handlers;

import me.nakilex.levelplugin.npc.dialog.NPCDialogManager;
import me.nakilex.levelplugin.quests.data.PlayerQuestProgress;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.def.HawieHermitCrabQuest;
import me.nakilex.levelplugin.quests.gui.QuestState;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.entity.Player;

/**
 * Handles Hawie's hermit crab clearing quest.
 */
public class HawieHermitCrabNpcHandler extends AbstractQuestNpcHandler {

    public HawieHermitCrabNpcHandler(QuestManager questManager, NPCDialogManager dialogManager) {
        super(HawieHermitCrabQuest.ID, questManager, dialogManager);
    }

    @Override
    public boolean handle(Player player, NPC npc, Quest quest, QuestState state,
                          QuestManager questManager, NPCDialogManager dialogManager) {
        if (state == QuestState.AVAILABLE) {
            dialogManager.startDialog(player, quest, npc);
            return true;
        }
        if (state == QuestState.LOCKED) {
            questManager.meetsRequirements(player, quest);
            return true;
        }

        PlayerQuestProgress progress = questManager.getProgress(player.getUniqueId(), HawieHermitCrabQuest.ID);
        if (progress == null) {
            return false;
        }

        boolean crabsCleared = progress.getProgress(0) >= quest.getObjectives().get(0).getAmount();
        boolean returned = progress.getProgress(1) >= 1;

        if (!crabsCleared) {
            player.sendMessage("§cClear the hermit crabs before reporting back.");
            return true;
        }

        if (!returned) {
            dialogManager.startDialog(player,
                    HawieHermitCrabQuest.getReturnDialog(),
                    npc,
                    () -> questManager.handleTalk(player, "npc" + npc.getId()));
            return true;
        }

        return false;
    }
}
