package me.nakilex.levelplugin.npc.handlers;

import me.nakilex.levelplugin.npc.dialog.NPCDialogManager;
import me.nakilex.levelplugin.quests.data.PlayerQuestProgress;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.def.SerasSlimeKingQuest;
import me.nakilex.levelplugin.quests.gui.QuestState;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.npc.system.NPC;
import org.bukkit.entity.Player;

/**
 * Handles Seras' Slime King follow-up quest.
 */
public class SerasSlimeKingNpcHandler extends AbstractQuestNpcHandler {

    public SerasSlimeKingNpcHandler(QuestManager questManager, NPCDialogManager dialogManager) {
        super(SerasSlimeKingQuest.ID, questManager, dialogManager);
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

        PlayerQuestProgress progress = questManager.getProgress(player.getUniqueId(), quest.getId());
        if (progress == null) {
            return false;
        }

        boolean introDone = progress.getProgress(0) >= quest.getObjectives().get(0).getAmount();
        boolean slimeKingDefeated = progress.getProgress(1) >= quest.getObjectives().get(1).getAmount();
        boolean finaleDone = progress.getProgress(2) >= quest.getObjectives().get(2).getAmount();

        if (!introDone) {
            dialogManager.startDialog(player,
                    quest.getDialogLines(),
                    npc,
                    () -> questManager.handleTalk(player, "npc" + npc.getId() + "_second"));
            return true;
        }

        if (!slimeKingDefeated) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    "Seras|The Slime King is still oozing around—bring it down and return to me.");
            return true;
        }

        if (!finaleDone) {
            dialogManager.startDialog(player,
                    SerasSlimeKingQuest.getDialogForObjective(2),
                    npc,
                    () -> questManager.handleTalk(player, "npc" + npc.getId() + "_third"));
            return true;
        }

        return false;
    }
}
