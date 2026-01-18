package me.nakilex.levelplugin.npc.handlers;

import me.nakilex.levelplugin.npc.dialog.NPCDialogManager;
import me.nakilex.levelplugin.quests.data.PlayerQuestProgress;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.def.SerasSlimeKingQuest;
import me.nakilex.levelplugin.quests.gui.QuestState;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.entity.Player;

/**
 * Handles Seras' Slime King follow-up quest.
 */
public class SerasSlimeKingNpcHandler extends AbstractQuestNpcHandler {

    public SerasSlimeKingNpcHandler(QuestManager questManager, NPCDialogManager dialogManager) {
        super(SerasSlimeKingQuest.ID, questManager, dialogManager);
    }

    @Override
    public boolean handle(Player player, me.nakilex.levelplugin.npc.system.NPC npc,
                          net.citizensnpcs.api.npc.NPC citizensNpc,
                          Quest quest, QuestState state,
                          QuestManager questManager, NPCDialogManager dialogManager) {
        if (state == QuestState.AVAILABLE) {
            startQuestDialog(player, quest, npc, citizensNpc);
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
            startDialog(player,
                    quest.getDialogLines(),
                    npc,
                    citizensNpc,
                    () -> questManager.handleTalk(player, "npc" + getNpcId(npc, citizensNpc) + "_second"));
            return true;
        }

        if (!slimeKingDefeated) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    "Seras|The Slime King is still oozing around—bring it down and return to me.");
            return true;
        }

        if (!finaleDone) {
            startDialog(player,
                    SerasSlimeKingQuest.getDialogForObjective(2),
                    npc,
                    citizensNpc,
                    () -> questManager.handleTalk(player, "npc" + getNpcId(npc, citizensNpc) + "_third"));
            return true;
        }

        return false;
    }
}
