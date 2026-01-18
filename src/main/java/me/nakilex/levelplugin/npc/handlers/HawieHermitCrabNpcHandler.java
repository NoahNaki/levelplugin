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
import me.nakilex.levelplugin.quests.util.QuestMessageUtil;
import me.nakilex.levelplugin.quests.gui.QuestState;
import me.nakilex.levelplugin.quests.managers.QuestManager;
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

        PlayerQuestProgress progress = questManager.getProgress(player.getUniqueId(), HawieHermitCrabQuest.ID);
        if (progress == null) {
            return false;
        }

        boolean introDone = progress.getProgress(0) >= 1;
        boolean crabsCleared = progress.getProgress(1) >= quest.getObjectives().get(1).getAmount();
        boolean returnDone = progress.getProgress(2) >= 1;
        boolean fishCaptured = progress.getProgress(3) >= quest.getObjectives().get(3).getAmount();
        boolean fishReturnDone = progress.getProgress(4) >= 1;

        if (!introDone) {
            startDialog(player,
                    quest.getDialogLines(),
                    npc,
                    citizensNpc,
                    () -> questManager.handleTalk(player, HawieHermitCrabQuest.INTRO_TARGET));
            return true;
        }

        if (!crabsCleared) {
            player.sendMessage("§cClear the hermit crabs before reporting back.");
            return true;
        }

        if (!returnDone) {
            if (player.getInventory().firstEmpty() == -1) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                        "Your inventory is full. Make room before receiving the fishing rod.");
                return true;
            }
            startDialog(player,
                    HawieHermitCrabQuest.getReturnDialog(),
                    npc,
                    citizensNpc,
                    () -> {
                        CustomTool tool = ToolManager.getInstance().getTool(ToolTier.TIER_I, ToolDiscipline.FISHING);
                        if (tool != null) {
                            ItemStack rod = ToolManager.getInstance().createToolItem(tool, player);
                            player.getInventory().addItem(rod);
                            QuestMessageUtil.sendQuestItemReceived(player, rod);
                        }
                        questManager.handleTalk(player, HawieHermitCrabQuest.RETURN_TARGET);
                    });
            return true;
        }

        if (!fishCaptured) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    "Catch a fish from the lake, then bring it back to Hawie.");
            return true;
        }

        if (!fishReturnDone) {
            if (!hasFish(player)) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                        "Bring back a fish from the lake before reporting in.");
                return true;
            }
            removeOneFish(player);
            startDialog(player,
                    HawieHermitCrabQuest.getFishingReturnDialog(),
                    npc,
                    citizensNpc,
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
