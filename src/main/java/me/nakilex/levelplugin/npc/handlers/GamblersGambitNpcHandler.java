package me.nakilex.levelplugin.npc.handlers;

import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.npc.dialog.NPCDialogManager;
import me.nakilex.levelplugin.quests.data.PlayerQuestProgress;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.def.GamblersGambitQuest;
import me.nakilex.levelplugin.quests.gui.QuestState;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.CurrencyMessageUtil;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Handles the Gambler NPC flow, including repeat runs and fee collection.
 */
public class GamblersGambitNpcHandler extends AbstractQuestNpcHandler {
    private final EconomyManager economyManager;

    public GamblersGambitNpcHandler(QuestManager questManager, NPCDialogManager dialogManager,
                                    EconomyManager economyManager) {
        super(GamblersGambitQuest.ID, questManager, dialogManager);
        this.economyManager = economyManager;
    }

    @Override
    public boolean handle(Player player, NPC npc, Quest quest, QuestState state,
                          QuestManager questManager, NPCDialogManager dialogManager) {
        if (dialogManager.resumePendingChoice(player, npc)) {
            return true;
        }

        if (state == QuestState.LOCKED) {
            questManager.meetsRequirements(player, quest);
            return true;
        }

        if (dialogManager != null && (state == QuestState.AVAILABLE || state == QuestState.COMPLETED)) {
            dialogManager.resetDialog(player);
        }

        UUID uuid = player.getUniqueId();
        PlayerQuestProgress progress = questManager.getProgress(uuid, quest.getId());
        GamblersGambitQuest script = GamblersGambitQuest.getInstance();

        if (state == QuestState.COMPLETED) {
            dialogManager.startDialog(player,
                    GamblersGambitQuest.getRepeatDialog(),
                    npc,
                    () -> dialogManager.startChoiceDialog(player,
                            npc,
                            java.util.List.of("Yes", "No"),
                            GamblersGambitQuest.ID,
                            GamblersGambitQuest.getChoiceFlagBase(),
                            choice -> handleChoice(player, uuid, choice, script, npc)));
            return true;
        }

        if (state == QuestState.AVAILABLE) {
            dialogManager.startDialog(player,
                    GamblersGambitQuest.getOfferDialog(),
                    npc,
                    () -> dialogManager.startChoiceDialog(player,
                            npc,
                            java.util.List.of("Yes", "No"),
                            GamblersGambitQuest.ID,
                            GamblersGambitQuest.getChoiceFlagBase(),
                            choice -> handleChoice(player, uuid, choice, script, npc)));
            return true;
        }

        if (progress != null && progress.getProgress(GamblersGambitQuest.GUESS_OBJECTIVE_INDEX) >= 1) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                    "Enjoy your winnings—you already cracked the gambler's game.");
            return true;
        }

        if (script != null) {
            script.remindGuess(player);
        }
        return true;
    }

    private void handleChoice(Player player, UUID uuid, int choice, GamblersGambitQuest script, NPC npc) {
        questManager.removeFlag(uuid, GamblersGambitQuest.ID,
                GamblersGambitQuest.getChoiceFlagBase() + choice);
        if (choice == 0) {
            int balance = economyManager.getBalance(player);
            if (balance < GamblersGambitQuest.ENTRY_FEE) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                        "You need <glyph:coins_icon> " + GamblersGambitQuest.ENTRY_FEE + " to ante up.");
                return;
            }
            economyManager.deductCoins(player, GamblersGambitQuest.ENTRY_FEE);
            CurrencyMessageUtil.sendLoss(player, CurrencyMessageUtil.Currency.COINS,
                    GamblersGambitQuest.ENTRY_FEE);
            if (questManager.hasCompleted(uuid, GamblersGambitQuest.ID)) {
                questManager.resetQuest(uuid, GamblersGambitQuest.ID, true);
            }
            questManager.startQuest(player, GamblersGambitQuest.ID);
            dialogManager.startDialog(player,
                    GamblersGambitQuest.getAcceptDialog(),
                    npc,
                    () -> {
                        if (script != null) {
                            script.remindGuess(player);
                        }
                    });
        } else {
            dialogManager.startDialog(player,
                    GamblersGambitQuest.getDeclineDialog(),
                    npc,
                    null);
        }
    }
}
