package me.nakilex.levelplugin.npc.handlers;

import me.nakilex.levelplugin.Main;
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
import org.bukkit.scheduler.BukkitRunnable;

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
            if (questManager.isQuestCooling(uuid, quest.getId())) {
                long remaining = questManager.getCooldownRemaining(uuid, quest);
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                        "This daily wager opens again in " + formatDuration(remaining) + ".");
                return true;
            }
            openOfferWithChoice(player, npc, GamblersGambitQuest.getRepeatDialog(), script);
            return true;
        }

        if (state == QuestState.AVAILABLE) {
            openOfferWithChoice(player, npc, GamblersGambitQuest.getOfferDialog(), script);
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

    private void openOfferWithChoice(Player player, NPC npc, java.util.List<String> dialog,
                                     GamblersGambitQuest script) {
        dialogManager.startDialog(player,
                dialog,
                npc,
                () -> new BukkitRunnable() {
                    @Override
                    public void run() {
                        dialogManager.startChoiceDialog(player,
                                npc,
                                java.util.List.of("Yes", "No"),
                                GamblersGambitQuest.ID,
                                GamblersGambitQuest.getChoiceFlagBase(),
                                choice -> handleChoice(player, player.getUniqueId(), choice, script, npc));
                    }
                }.runTaskLater(Main.getInstance(), 1L));
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

    private String formatDuration(long millis) {
        java.time.Duration duration = java.time.Duration.ofMillis(Math.max(0, millis));
        long hours = duration.toHours();
        long minutes = duration.minusHours(hours).toMinutes();
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        return minutes + "m";
    }
}
