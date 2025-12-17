package me.nakilex.levelplugin.npc.handlers;

import me.nakilex.levelplugin.npc.dialog.NPCDialogManager;
import me.nakilex.levelplugin.quests.data.PlayerQuestProgress;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.def.SharpestSecretQuest;
import me.nakilex.levelplugin.quests.gui.QuestState;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.NpcNameUtil;
import me.nakilex.levelplugin.enchanting.gui.EnchantGUI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Handles Sharpest Secret's multi-NPC flow.
 */
public class SharpestSecretNpcHandler extends AbstractQuestNpcHandler {

    private final EnchantGUI enchantGUI;

    public SharpestSecretNpcHandler(QuestManager questManager, NPCDialogManager dialogManager,
                                    EnchantGUI enchantGUI) {
        super(SharpestSecretQuest.ID, questManager, dialogManager);
        this.enchantGUI = enchantGUI;
    }

    @Override
    public boolean handle(Player player, NPC npc, Quest quest, QuestState state,
                          QuestManager questManager, NPCDialogManager dialogManager) {
        boolean isKazan = NpcNameUtil.equalsNormalized(npc.getName(), SharpestSecretQuest.NPC_KAZAN_NAME);
        boolean isOsiris = NpcNameUtil.equalsNormalized(npc.getName(), SharpestSecretQuest.NPC_OSIRIS_NAME);
        if (!isKazan && !isOsiris) {
            return false;
        }

        boolean completed = questManager.hasCompleted(player.getUniqueId(), SharpestSecretQuest.ID);
        PlayerQuestProgress progress = questManager.getProgress(player.getUniqueId(), SharpestSecretQuest.ID);

        if (isKazan) {
            if (completed) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                        "Osiris owes you a tasting whenever you need another edge.");
                return true;
            }
            if (progress == null) {
                return false;
            }

            boolean introDone = progress.getProgress(SharpestSecretQuest.TALK_INTRO_INDEX) >= 1;
            boolean waitDone = progress.getProgress(SharpestSecretQuest.WAIT_FOR_NIGHT_INDEX) >= 1;
            boolean orchidFound = progress.getProgress(SharpestSecretQuest.FIND_ORCHID_INDEX) >= 1;
            boolean returned = progress.getProgress(SharpestSecretQuest.TALK_RETURN_INDEX) >= 1;
            boolean osirisSpoken = progress.getProgress(SharpestSecretQuest.TALK_OSIRIS_INDEX) >= 1;

            if (!introDone) {
                dialogManager.startDialog(player,
                        SharpestSecretQuest.getIntroDialog(),
                        npc,
                        () -> questManager.handleTalk(player, SharpestSecretQuest.NPC_INTRO_TARGET));
                return true;
            }

            if (!waitDone || !orchidFound) {
                return true;
            }

            if (orchidFound && !returned) {
                if (!SharpestSecretQuest.hasMidnightOrchid(player)) {
                    ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                            "You don't have the Midnight Orchid on you. Check beneath the oak at midnight again.");
                    return true;
                }
                dialogManager.startDialog(player,
                        SharpestSecretQuest.getReturnDialog(),
                        npc,
                        () -> {
                            SharpestSecretQuest.removeMidnightOrchid(player);
                            questManager.handleTalk(player, SharpestSecretQuest.NPC_RETURN_TARGET);
                        });
                return true;
            }

            if (returned && !osirisSpoken) {
                return true;
            }

            if (osirisSpoken) {
                dialogManager.startDialog(player,
                        SharpestSecretQuest.getOsirisReminderDialog(),
                        npc,
                        () -> {
                            if (enchantGUI != null) {
                                enchantGUI.open(player);
                            }
                        });
                return true;
            }
        }

        if (isOsiris) {
            if (completed) {
                dialogManager.startDialog(player,
                        SharpestSecretQuest.getOsirisReminderDialog(),
                        npc,
                        () -> {
                            if (enchantGUI != null) {
                                enchantGUI.open(player);
                            }
                        });
                return true;
            }

            if (progress == null) {
                return true;
            }

            boolean returned = progress.getProgress(SharpestSecretQuest.TALK_RETURN_INDEX) >= 1;
            boolean osirisSpoken = progress.getProgress(SharpestSecretQuest.TALK_OSIRIS_INDEX) >= 1;

            if (!returned) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                        "You should report back to Kazan before asking for the tasting.");
                return true;
            }

            if (!osirisSpoken) {
                dialogManager.startDialog(player,
                        SharpestSecretQuest.getOsirisIntroDialog(),
                        npc,
                        () -> Bukkit.getScheduler().runTaskLater(me.nakilex.levelplugin.Main.getInstance(), () ->
                                dialogManager.startChoiceDialog(player,
                                        npc,
                                        java.util.List.of("Memory", "Secret", "Spell", "Lie"),
                                        SharpestSecretQuest.ID,
                                        "osiris_choice_",
                                        choice -> {
                                            if (choice == 1) {
                                                dialogManager.startDialog(player,
                                                        SharpestSecretQuest.getOsirisSuccessDialog(player.getName()),
                                                        npc,
                                                        () -> {
                                                            SharpestSecretQuest.giveEnchantToken(player);
                                                            questManager.handleTalk(player, SharpestSecretQuest.NPC_OSIRIS_TARGET);
                                                            if (enchantGUI != null) {
                                                                enchantGUI.open(player);
                                                            }
                                                        });
                                            } else {
                                                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                                                        "Osiris smirks. 'Not quite. Come back when the answer is clear.'");
                                            }
                                        }), 1L));
                return true;
            }

            dialogManager.startDialog(player,
                    SharpestSecretQuest.getOsirisReminderDialog(),
                    npc,
                    () -> {
                        if (enchantGUI != null) {
                            enchantGUI.open(player);
                        }
                    });
            return true;
        }

        return false;
    }
}
