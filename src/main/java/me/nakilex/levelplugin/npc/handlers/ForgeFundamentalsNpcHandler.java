package me.nakilex.levelplugin.npc.handlers;

import me.nakilex.levelplugin.npc.dialog.NPCDialogManager;
import me.nakilex.levelplugin.quests.data.PlayerQuestProgress;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.def.ForgeFundamentalsQuest;
import me.nakilex.levelplugin.quests.gui.QuestState;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.npc.system.NPC;
import org.bukkit.entity.Player;

/**
 * Handles the blacksmith training quest interactions.
 */
public class ForgeFundamentalsNpcHandler extends AbstractQuestNpcHandler {

    public ForgeFundamentalsNpcHandler(QuestManager questManager, NPCDialogManager dialogManager) {
        super(ForgeFundamentalsQuest.ID, questManager, dialogManager);
    }

    @Override
    public boolean handle(Player player, NPC npc, Quest quest, QuestState state,
                          QuestManager questManager, NPCDialogManager dialogManager) {
        if (state == QuestState.AVAILABLE) {
            player.performCommand("blacksmith");
            return true;
        }
        if (state == QuestState.LOCKED) {
            player.performCommand("blacksmith");
            return true;
        }

        PlayerQuestProgress progress = questManager.getProgress(player.getUniqueId(), quest.getId());
        boolean introDone = progress != null && progress.getProgress(0) >= 1;
        boolean serviceDone = progress != null && progress.getProgress(1) >= 1;
        boolean returned = progress != null && progress.getProgress(2) >= 1;

        if (!introDone && progress != null) {
            dialogManager.startDialog(player,
                    quest.getDialogLines(),
                    npc,
                    () -> questManager.handleTalk(player, ForgeFundamentalsQuest.NPC_NAME.equalsIgnoreCase(npc.getName())
                            ? "npc_blacksmith_intro"
                            : "npc" + npc.getId()));
            return true;
        }

        if (returned || questManager.hasCompleted(player.getUniqueId(), ForgeFundamentalsQuest.ID)) {
            player.performCommand("blacksmith");
            return true;
        }

        if (!serviceDone) {
            player.performCommand("blacksmith");
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    "Use repair, reroll, or upgrade once, then check back with the Blacksmith.");
            return true;
        }

        dialogManager.startDialog(player,
                ForgeFundamentalsQuest.getReturnDialog(),
                npc,
                () -> questManager.handleTalk(player, "npc_blacksmith_return"));
        return true;
    }
}
