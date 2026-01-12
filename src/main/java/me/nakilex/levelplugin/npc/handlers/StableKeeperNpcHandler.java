package me.nakilex.levelplugin.npc.handlers;

import me.nakilex.levelplugin.horse.gui.HorseGUI;
import me.nakilex.levelplugin.npc.dialog.NPCDialogManager;
import me.nakilex.levelplugin.quests.data.PlayerQuestProgress;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.def.StableKeeperQuest;
import me.nakilex.levelplugin.quests.gui.QuestState;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.npc.system.NPC;
import org.bukkit.entity.Player;

/**
 * Handles the Stable Keeper quest flow.
 */
public class StableKeeperNpcHandler extends AbstractQuestNpcHandler {

    private final HorseGUI horseGUI;

    public StableKeeperNpcHandler(QuestManager questManager, NPCDialogManager dialogManager, HorseGUI horseGUI) {
        super(StableKeeperQuest.ID, questManager, dialogManager);
        this.horseGUI = horseGUI;
    }

    @Override
    public boolean handle(Player player, NPC npc, Quest quest, QuestState state,
                          QuestManager questManager, NPCDialogManager dialogManager) {
        if (questManager.hasCompleted(player.getUniqueId(), StableKeeperQuest.ID)) {
            horseGUI.openHorseMenu(player);
            return true;
        }

        PlayerQuestProgress progress = questManager.getProgress(player.getUniqueId(), StableKeeperQuest.ID);
        if (progress == null) {
            return false;
        }

        boolean introDone = progress.getProgress(StableKeeperQuest.TALK_INTRO_INDEX) >= 1;
        boolean roostersCleared = progress.getProgress(StableKeeperQuest.KILL_ROOSTERS_INDEX) >= 5;
        boolean reportDone = progress.getProgress(StableKeeperQuest.TALK_REPORT_INDEX) >= 1;
        boolean horseBought = progress.getProgress(StableKeeperQuest.BUY_HORSE_INDEX) >= 1;
        boolean finaleDone = progress.getProgress(StableKeeperQuest.TALK_FINAL_INDEX) >= 1;

        if (!introDone) {
            dialogManager.startDialog(player,
                    quest.getDialogLines(),
                    npc,
                    () -> questManager.handleTalk(player, StableKeeperQuest.NPC_TALK_TARGET));
            return true;
        }

        if (!roostersCleared) {
            player.sendMessage("§cThin out five wild roosters so the feed can grow back.");
            return true;
        }

        if (roostersCleared && !reportDone) {
            dialogManager.startDialog(player,
                    StableKeeperQuest.getDialogForObjective(StableKeeperQuest.TALK_REPORT_INDEX),
                    npc,
                    () -> questManager.handleTalk(player, StableKeeperQuest.NPC_RETURN_TARGET));
            return true;
        }

        if (reportDone && !horseBought) {
            horseGUI.openHorseMenu(player);
            player.sendMessage("§eClaim a horse from the stable, then talk to the Stable Keeper again.");
            return true;
        }

        if (horseBought && !finaleDone) {
            dialogManager.startDialog(player,
                    StableKeeperQuest.getDialogForObjective(StableKeeperQuest.TALK_FINAL_INDEX),
                    npc,
                    () -> questManager.handleTalk(player, StableKeeperQuest.NPC_FINAL_TARGET));
            return true;
        }

        return false;
    }
}
