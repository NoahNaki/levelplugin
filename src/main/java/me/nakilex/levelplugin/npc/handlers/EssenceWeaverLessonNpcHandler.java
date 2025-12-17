package me.nakilex.levelplugin.npc.handlers;

import me.nakilex.levelplugin.npc.dialog.NPCDialogManager;
import me.nakilex.levelplugin.quests.data.PlayerQuestProgress;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.def.EssenceWeaversLessonQuest;
import me.nakilex.levelplugin.quests.gui.QuestState;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.entity.Player;

/**
 * Handles the Essence Weaver tutorial interactions.
 */
public class EssenceWeaverLessonNpcHandler extends AbstractQuestNpcHandler {

    public EssenceWeaverLessonNpcHandler(QuestManager questManager, NPCDialogManager dialogManager) {
        super(EssenceWeaversLessonQuest.ID, questManager, dialogManager);
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
        boolean introDone = progress != null && progress.getProgress(0) >= 1;
        boolean upgradeTried = progress != null && progress.getProgress(1) >= 1;
        boolean returned = progress != null && progress.getProgress(2) >= 1;

        if (!introDone && progress != null) {
            dialogManager.startDialog(player,
                    quest.getDialogLines(),
                    npc,
                    () -> questManager.handleTalk(player, EssenceWeaversLessonQuest.NPC_NAME.equalsIgnoreCase(npc.getName())
                            ? "npc_essence_weaver_intro"
                            : "npc" + npc.getId()));
            return true;
        }

        if (returned || questManager.hasCompleted(player.getUniqueId(), EssenceWeaversLessonQuest.ID)) {
            player.performCommand("essenceupgrade");
            return true;
        }

        if (!upgradeTried) {
            player.performCommand("essenceupgrade");
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    "Invest a duplicate essence or attempt a star upgrade, then speak with the Essence Weaver again.");
            return true;
        }

        dialogManager.startDialog(player,
                EssenceWeaversLessonQuest.getReturnDialog(),
                npc,
                () -> questManager.handleTalk(player, "npc_essence_weaver_return"));
        return true;
    }
}
