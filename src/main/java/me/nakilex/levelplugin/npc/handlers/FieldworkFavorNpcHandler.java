package me.nakilex.levelplugin.npc.handlers;

import me.nakilex.levelplugin.items.tools.CustomTool;
import me.nakilex.levelplugin.items.tools.ToolDiscipline;
import me.nakilex.levelplugin.items.tools.ToolManager;
import me.nakilex.levelplugin.items.tools.ToolTier;
import me.nakilex.levelplugin.npc.dialog.NPCDialogManager;
import me.nakilex.levelplugin.quests.data.PlayerQuestProgress;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.def.FieldworkFavorQuest;
import me.nakilex.levelplugin.quests.gui.QuestState;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.quests.util.QuestMessageUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Handles the fieldwork lifeskilling quest for the farm NPC.
 */
public class FieldworkFavorNpcHandler extends AbstractQuestNpcHandler {

    public FieldworkFavorNpcHandler(QuestManager questManager, NPCDialogManager dialogManager) {
        super(FieldworkFavorQuest.ID, questManager, dialogManager);
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
        if (state == QuestState.COMPLETED) {
            player.performCommand("farmrewards");
            return true;
        }

        PlayerQuestProgress progress = questManager.getProgress(player.getUniqueId(), FieldworkFavorQuest.ID);
        if (progress == null) {
            return false;
        }

        boolean introDone = progress.getProgress(0) >= 1;
        int harvested = progress.getProgress(1);
        int required = quest.getObjectives().get(1).getAmount();
        boolean wheatHarvested = harvested >= required;
        boolean returnDone = progress.getProgress(2) >= 1;

        if (!introDone) {
            if (player.getInventory().firstEmpty() == -1) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                        "Your inventory is full. Make room before receiving the scythe.");
                return true;
            }
            startDialog(player,
                    quest.getDialogLines(),
                    npc,
                    citizensNpc,
                    () -> {
                        CustomTool tool = ToolManager.getInstance().getTool(ToolTier.TIER_I, ToolDiscipline.FARMING);
                        if (tool != null) {
                            ItemStack scythe = ToolManager.getInstance().createToolItem(tool, player);
                            player.getInventory().addItem(scythe);
                            QuestMessageUtil.sendQuestItemReceived(player, scythe);
                        }
                        questManager.handleTalk(player, FieldworkFavorQuest.INTRO_TARGET);
                    });
            return true;
        }

        if (!wheatHarvested) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    "Harvest wheat from the field behind me: " + harvested + "/" + required + ".");
            return true;
        }

        if (!returnDone) {
            startDialog(player,
                    FieldworkFavorQuest.getReturnDialog(),
                    npc,
                    citizensNpc,
                    () -> questManager.handleTalk(player, FieldworkFavorQuest.RETURN_TARGET));
            return true;
        }

        player.performCommand("farmrewards");
        return true;
    }
}
