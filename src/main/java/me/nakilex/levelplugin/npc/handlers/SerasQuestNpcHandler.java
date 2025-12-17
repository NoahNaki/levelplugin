package me.nakilex.levelplugin.npc.handlers;

import me.nakilex.levelplugin.npc.dialog.NPCDialogManager;
import me.nakilex.levelplugin.quests.data.PlayerQuestProgress;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.def.SerasQuest;
import me.nakilex.levelplugin.quests.gui.QuestState;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.entity.Player;

/**
 * Handles Seras' introductory slime quest.
 */
public class SerasQuestNpcHandler extends AbstractQuestNpcHandler {

    public SerasQuestNpcHandler(QuestManager questManager, NPCDialogManager dialogManager) {
        super(SerasQuest.ID, questManager, dialogManager);
    }

    @Override
    public boolean handle(Player player, NPC npc, Quest quest, QuestState state,
                          QuestManager questManager, NPCDialogManager dialogManager) {
        PlayerQuestProgress progress = questManager.getProgress(player.getUniqueId(), quest.getId());
        if (progress == null) {
            return false;
        }

        boolean introDone = progress.getProgress(0) >= quest.getObjectives().get(0).getAmount();
        boolean killSlimesDone = progress.getProgress(1) >= quest.getObjectives().get(1).getAmount();
        boolean talkedAfterSlimes = progress.getProgress(2) >= quest.getObjectives().get(2).getAmount();

        if (!introDone) {
            dialogManager.startDialog(player,
                    quest.getDialogLines(),
                    npc,
                    () -> questManager.handleTalk(player, "npc" + npc.getId()));
            return true;
        }
        if (killSlimesDone && !talkedAfterSlimes) {
            dialogManager.startDialog(player,
                    SerasQuest.getDialogForObjective(2),
                    npc,
                    () -> questManager.handleTalk(player, "npc" + npc.getId() + "_first"));
            return true;
        }
        if (!killSlimesDone) {
            player.sendMessage("§cClear 10 forest slimes, then report back to Seras.");
            return true;
        }
        return false;
    }
}
