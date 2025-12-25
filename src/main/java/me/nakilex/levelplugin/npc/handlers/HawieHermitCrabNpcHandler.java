package me.nakilex.levelplugin.npc.handlers;

import me.nakilex.levelplugin.npc.dialog.NPCDialogManager;
import me.nakilex.levelplugin.quests.data.PlayerQuestProgress;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.items.tools.CustomTool;
import me.nakilex.levelplugin.items.tools.ToolDiscipline;
import me.nakilex.levelplugin.items.tools.ToolManager;
import me.nakilex.levelplugin.items.tools.ToolTier;
import me.nakilex.levelplugin.player.fishing.utils.FishingItemUtil;
import me.nakilex.levelplugin.quests.def.HawieHermitCrabQuest;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.quests.gui.QuestState;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.inventory.ItemStack;
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
        boolean crabReturnDone = progress.getProgress(1) >= 1;
        boolean fishReturnDone = progress.getProgress(2) >= 1;

        if (!crabsCleared) {
            player.sendMessage("§cClear the hermit crabs before reporting back.");
            return true;
        }

        if (!crabReturnDone) {
            if (player.getInventory().firstEmpty() == -1) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                        "Your inventory is full. Make room before receiving the fishing rod.");
                return true;
            }
            CustomTool tool = ToolManager.getInstance().getTool(ToolTier.TIER_I, ToolDiscipline.FISHING);
            if (tool != null) {
                ItemStack rod = ToolManager.getInstance().createToolItem(tool, player);
                player.getInventory().addItem(rod);
            }
            dialogManager.startDialog(player,
                    HawieHermitCrabQuest.getReturnDialog(),
                    npc,
                    () -> questManager.handleTalk(player, HawieHermitCrabQuest.CRAB_RETURN_TARGET));
            return true;
        }

        if (!fishReturnDone) {
            if (!hasFish(player)) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                        "Bring back a fish from the lake before reporting in.");
                return true;
            }
            removeOneFish(player);
            dialogManager.startDialog(player,
                    HawieHermitCrabQuest.getFishingReturnDialog(),
                    npc,
                    () -> questManager.handleTalk(player, HawieHermitCrabQuest.FISH_RETURN_TARGET));
            return true;
        }

        player.performCommand("fishrewards");
        return true;

    }

    private boolean hasFish(Player player) {
        for (ItemStack stack : player.getInventory().getContents()) {
            if (FishingItemUtil.isFish(stack)) {
                return true;
            }
        }
        return false;
    }

    private void removeOneFish(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (!FishingItemUtil.isFish(stack)) continue;
            if (stack.getAmount() <= 1) {
                contents[i] = null;
            } else {
                stack.setAmount(stack.getAmount() - 1);
            }
            player.getInventory().setContents(contents);
            return;
        }
    }
}
