package me.nakilex.levelplugin.npc.handlers;

import me.nakilex.levelplugin.npc.dialog.NPCDialogManager;
import me.nakilex.levelplugin.quests.data.PlayerQuestProgress;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.def.ZoyaDungeonQuest;
import me.nakilex.levelplugin.quests.gui.QuestState;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import org.bukkit.entity.Player;

/**
 * Handles the Zoya dungeon creation quest reminders.
 */
public class ZoyaDungeonNpcHandler extends AbstractQuestNpcHandler {

    public ZoyaDungeonNpcHandler(QuestManager questManager, NPCDialogManager dialogManager) {
        super("zoyadungeon", questManager, dialogManager);
    }

    @Override
    public boolean handle(Player player, me.nakilex.levelplugin.npc.system.NPC npc,
                          net.citizensnpcs.api.npc.NPC citizensNpc,
                          Quest quest, QuestState state,
                          QuestManager questManager, NPCDialogManager dialogManager) {
        PlayerQuestProgress progress = questManager.getProgress(player.getUniqueId(), quest.getId());
        if (progress == null) {
            return false;
        }
        boolean introDone = progress.getProgress(0) >= quest.getObjectives().get(0).getAmount();
        boolean dungeonSaved = progress.getProgress(1) >= quest.getObjectives().get(1).getAmount();
        boolean finaleDone = progress.getProgress(2) >= quest.getObjectives().get(2).getAmount();

        if (introDone && !dungeonSaved) {
            startDialog(player,
                    ZoyaDungeonQuest.getReminderDialog(),
                    npc,
                    citizensNpc,
                    null);
            return true;
        }

        if (dungeonSaved && !finaleDone) {
            startDialog(player,
                    ZoyaDungeonQuest.getCompletionDialog(),
                    npc,
                    citizensNpc,
                    () -> questManager.handleTalk(player, "npc" + getNpcId(npc, citizensNpc) + "_return"));
            return true;
        }

        return false;
    }
}
