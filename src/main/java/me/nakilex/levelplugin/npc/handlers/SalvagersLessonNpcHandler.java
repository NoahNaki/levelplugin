package me.nakilex.levelplugin.npc.handlers;

import me.nakilex.levelplugin.npc.dialog.NPCDialogManager;
import me.nakilex.levelplugin.quests.data.PlayerQuestProgress;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.def.SalvagersLessonQuest;
import me.nakilex.levelplugin.quests.gui.QuestState;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.quests.util.QuestServiceAccessTracker;
import me.nakilex.levelplugin.salvage.gui.SalvageGUI;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.entity.Player;

/**
 * Handles salvager tutorial interactions.
 */
public class SalvagersLessonNpcHandler extends AbstractQuestNpcHandler {

    public SalvagersLessonNpcHandler(QuestManager questManager, NPCDialogManager dialogManager) {
        super(SalvagersLessonQuest.ID, questManager, dialogManager);
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

        boolean completed = questManager.hasCompleted(player.getUniqueId(), SalvagersLessonQuest.ID);
        PlayerQuestProgress progress = questManager.getProgress(player.getUniqueId(), SalvagersLessonQuest.ID);
        boolean introDone = progress != null && progress.getProgress(SalvagersLessonQuest.TALK_INTRO_INDEX) >= 1;
        boolean salvaged = progress != null &&
                progress.getProgress(SalvagersLessonQuest.SALVAGE_INDEX) >= SalvagersLessonQuest.SALVAGE_AMOUNT;
        boolean returned = progress != null && progress.getProgress(SalvagersLessonQuest.TALK_RETURN_INDEX) >= 1;
        boolean cooling = QuestServiceAccessTracker.isCoolingDown(player.getUniqueId(), QuestServiceAccessTracker.Service.SALVAGE);

        if (!introDone && progress != null) {
            startDialog(player,
                    quest.getDialogLines(),
                    npc,
                    citizensNpc,
                    () -> questManager.handleTalk(player, SalvagersLessonQuest.INTRO_TARGET));
            return true;
        }

        if (completed || returned) {
            if (cooling) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                        "Give the salvager a moment before reopening the bench.");
                return true;
            }
            SalvageGUI.openMerchantGUI(player);
            return true;
        }

        if (!salvaged) {
            if (!cooling) {
                SalvageGUI.openMerchantGUI(player);
            } else {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                        "Let the salvager finish up before trying again.");
            }
            return true;
        }

        startDialog(player,
                SalvagersLessonQuest.getReturnDialog(),
                npc,
                citizensNpc,
                () -> questManager.handleTalk(player, SalvagersLessonQuest.RETURN_TARGET));
        return true;
    }
}
