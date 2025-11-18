package me.nakilex.levelplugin.npc.listeners;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.fakeblock.QuestGateManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.quests.data.PlayerQuestProgress;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.quests.def.DungeonGuardQuest;
import me.nakilex.levelplugin.quests.def.SerasQuest;
import me.nakilex.levelplugin.quests.def.StableKeeperQuest;
import me.nakilex.levelplugin.quests.def.ZoyaDungeonQuest;
import me.nakilex.levelplugin.quests.gui.QuestState;
import me.nakilex.levelplugin.npc.dialog.NPCDialogManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.CurrencyMessageUtil;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class NPCClickListener implements Listener {

    private final EconomyManager economyManager;
    private final QuestManager questManager;
    private final NPCDialogManager dialogManager;
    // Constructor to get the EconomyManager instance
    public NPCClickListener(EconomyManager economyManager, QuestManager questManager, NPCDialogManager dialogManager) {
        this.economyManager = economyManager;
        this.questManager = questManager;
        this.dialogManager = dialogManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onNPCClick(PlayerInteractEntityEvent event) {
        // Ignore offhand interactions
        if (event.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND) {
            return; // Ignore offhand clicks
        }

            if (CitizensAPI.getNPCRegistry().isNPC(event.getRightClicked())) {
                Player player = event.getPlayer();
                NPC npc = CitizensAPI.getNPCRegistry().getNPC(event.getRightClicked());

            String stripped = org.bukkit.ChatColor.stripColor(npc.getName());
            if (npc.getId() == 546) {
                Main.getInstance().getCodexManager().recordNpc(player, stripped);
            }
            if (stripped.equalsIgnoreCase("Starter Merchant")) {
                PlayerQuestProgress prog = questManager.getProgress(player.getUniqueId(), "newbeginning");
                if (prog == null || questManager.hasCompleted(player.getUniqueId(), "newbeginning")) {
                    player.performCommand("merchant starter_shop");
                    return;
                }
            }

            if (npc.getId() == 546 &&
                    questManager.hasCompleted(player.getUniqueId(), "newbeginning")) {
                if (!dialogManager.hasSession(player)) {
                    NPC seras = CitizensAPI.getNPCRegistry().getById(823);
                    String coords = "unknown";
                    if (seras != null) {
                        Location l = seras.isSpawned() ? seras.getEntity().getLocation() : seras.getStoredLocation();
                        if (l != null) {
                            coords = l.getBlockX() + ", " + l.getBlockY() + ", " + l.getBlockZ();
                        }
                    }
                    String line = "Piwan|You should talk to Seras at §8[§e" + coords + "§8]§f, I'm sure she has plenty of tasks for you, though be wary she's a fiery one.";
                    dialogManager.startDialog(player,
                            java.util.List.of(line),
                            npc,
                            null);
                }
            }

            if (dialogManager.hasSession(player)) {
                NPC sessionNpc = dialogManager.getSessionNpc(player);
                if (sessionNpc != null && sessionNpc.getId() == npc.getId()) {
                    dialogManager.advanceDialog(player, questManager);
                }
                return;
            }

            if (npc.getId() == DungeonGuardQuest.NPC_ID) {
                handleDungeonGuard(player, npc);
                return;
            }

            if (npc.getId() == StableKeeperQuest.NPC_ID) {
                handleStableKeeper(player, npc);
                return;
            }

            Quest quest = questManager.getQuestByNpcId(npc.getId());
            if (quest != null) {
                if ("serashelp".equals(quest.getId())) {
                    PlayerQuestProgress progress = questManager.getProgress(player.getUniqueId(), quest.getId());
                    if (progress != null) {
                        boolean introDone = progress.getProgress(0) >= quest.getObjectives().get(0).getAmount();
                        boolean killSlimesDone = progress.getProgress(1) >= quest.getObjectives().get(1).getAmount();
                        boolean talkAfterSlimes = progress.getProgress(2) >= quest.getObjectives().get(2).getAmount();
                        boolean killKingDone = progress.getProgress(3) >= quest.getObjectives().get(3).getAmount();
                        boolean talkAfterKing = progress.getProgress(4) >= quest.getObjectives().get(4).getAmount();

                        if (!introDone) {
                            dialogManager.startDialog(player,
                                    quest.getDialogLines(),
                                    npc,
                                    () -> questManager.handleTalk(player, "npc" + npc.getId()));
                            return;
                        }
                        if (killSlimesDone && !talkAfterSlimes) {
                            dialogManager.startDialog(player,
                                    me.nakilex.levelplugin.quests.def.SerasQuest.getDialogForObjective(2),
                                    npc,
                                    () -> questManager.handleTalk(player, "npc" + npc.getId() + "_first"));
                            return;
                        }
                        if (killSlimesDone && talkAfterSlimes && !killKingDone) {
                            player.sendMessage("§cComplete the quest first!");
                            return;
                        }
                        if (killKingDone && !talkAfterKing) {
                            dialogManager.startDialog(player,
                                    me.nakilex.levelplugin.quests.def.SerasQuest.getDialogForObjective(4),
                                    npc,
                                    () -> questManager.handleTalk(player, "npc" + npc.getId() + "_second"));
                            return;
                        }
                        if (!killSlimesDone) {
                            player.sendMessage("§cComplete the quest first!");
                            return;
                        }
                    }
                }

                if ("zoyadungeon".equals(quest.getId())) {
                    PlayerQuestProgress progress = questManager.getProgress(player.getUniqueId(), quest.getId());
                    if (progress != null) {
                        boolean introDone = progress.getProgress(0) >= quest.getObjectives().get(0).getAmount();
                        boolean dungeonSaved = progress.getProgress(1) >= quest.getObjectives().get(1).getAmount();
                        boolean finaleDone = progress.getProgress(2) >= quest.getObjectives().get(2).getAmount();

                        if (introDone && !dungeonSaved) {
                            dialogManager.startDialog(player,
                                    ZoyaDungeonQuest.getReminderDialog(),
                                    npc,
                                    null);
                            return;
                        }

                        if (dungeonSaved && !finaleDone) {
                            dialogManager.startDialog(player,
                                    ZoyaDungeonQuest.getCompletionDialog(),
                                    npc,
                                    () -> questManager.handleTalk(player, "npc" + npc.getId() + "_return"));
                            return;
                        }
                    }
                }

                questManager.handleTalk(player, "npc" + npc.getId());
                QuestState state = questManager.getQuestState(player, quest);
                switch (state) {
                    case AVAILABLE -> dialogManager.startDialog(player, quest, npc);
                    case LOCKED -> questManager.meetsRequirements(player, quest);
                    case ACCEPTED, IN_PROGRESS -> player.sendMessage("§cComplete the quest first!");
                    default -> {}
                }
            }
        }
    }

    private void handleStableKeeper(Player player, NPC npc) {
        Quest quest = questManager.getQuestByNpcId(StableKeeperQuest.NPC_ID);
        if (quest == null || !StableKeeperQuest.QUEST_ID.equalsIgnoreCase(quest.getId())) {
            player.performCommand("horse reroll");
            return;
        }

        if (questManager.hasCompleted(player.getUniqueId(), quest.getId())) {
            player.performCommand("horse reroll");
            return;
        }

        PlayerQuestProgress progress = questManager.getProgress(player.getUniqueId(), quest.getId());
        if (progress == null) {
            questManager.handleTalk(player, "npc" + npc.getId());
            QuestState state = questManager.getQuestState(player, quest);
            switch (state) {
                case AVAILABLE -> dialogManager.startDialog(player, quest, npc);
                case LOCKED -> questManager.meetsRequirements(player, quest);
                case ACCEPTED, IN_PROGRESS -> player.sendMessage("§cComplete the quest first!");
                default -> {}
            }
            return;
        }

        boolean introDone = progress.getProgress(0) >= quest.getObjectives().get(0).getAmount();
        boolean roostersDown = progress.getProgress(1) >= quest.getObjectives().get(1).getAmount();
        boolean feedDialog = progress.getProgress(2) >= quest.getObjectives().get(2).getAmount();
        boolean horseBought = progress.getProgress(3) >= quest.getObjectives().get(3).getAmount();
        boolean finaleDone = progress.getProgress(4) >= quest.getObjectives().get(4).getAmount();

        if (!introDone) {
            dialogManager.startDialog(player, quest, npc);
            return;
        }

        if (!roostersDown) {
            player.sendMessage("§cThe Stable Keeper still needs those wild roosters gone.");
            return;
        }

        if (roostersDown && !feedDialog) {
            dialogManager.startDialog(player,
                    StableKeeperQuest.getDialogForObjective(2),
                    npc,
                    () -> questManager.handleTalk(player, "npc" + npc.getId() + "_feed"));
            return;
        }

        if (!horseBought) {
            player.performCommand("horse reroll");
            return;
        }

        if (horseBought && !finaleDone) {
            dialogManager.startDialog(player,
                    StableKeeperQuest.getDialogForObjective(4),
                    npc,
                    () -> questManager.handleTalk(player, "npc" + npc.getId() + "_final"));
            return;
        }

        player.performCommand("horse reroll");
    }

    private void handleDungeonGuard(Player player, NPC npc) {
        if (StatsManager.getInstance().getLevel(player) < DungeonGuardQuest.REQUIRED_LEVEL) {
            dialogManager.startDialog(player, DungeonGuardQuest.getTooWeakDialog(), npc, null);
            return;
        }

        ensureDungeonGuardQuestStarted(player);

        if (hasUnlockedDungeonEntrance(player)) {
            dialogManager.startDialog(player,
                    DungeonGuardQuest.getApprovalDialog(),
                    npc,
                    () -> openDungeonGate(player));
            return;
        }

        if (dialogManager.resumePendingChoice(player, npc)) {
            return;
        }

        dialogManager.startDialog(player, DungeonGuardQuest.getIntroDialog(), npc,
                () -> Bukkit.getScheduler().runTaskLater(Main.getInstance(), () ->
                        dialogManager.startChoiceDialog(player, npc,
                                java.util.List.of("Yes", "No"),
                                DungeonGuardQuest.QUEST_ID,
                                "dungeonguard_choice_",
                                choice -> {
                                    if (choice == 0) {
                                        processDungeonEntryPurchase(player, npc);
                                    } else {
                                        dialogManager.startDialog(player,
                                                DungeonGuardQuest.getDeclineDialog(),
                                                npc,
                                                null);
                                    }
                                }), 1L));
    }

    private void ensureDungeonGuardQuestStarted(Player player) {
        Quest quest = questManager.getQuestByNpcId(DungeonGuardQuest.NPC_ID);
        if (quest == null) {
            return;
        }
        if (questManager.hasCompleted(player.getUniqueId(), quest.getId())) {
            return;
        }
        if (questManager.getProgress(player.getUniqueId(), quest.getId()) == null) {
            questManager.startQuest(player, quest.getId());
        }
    }

    private void processDungeonEntryPurchase(Player player, NPC npc) {
        int balance = economyManager.getBalance(player);
        if (balance < DungeonGuardQuest.ENTRY_FEE) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "You need " + DungeonGuardQuest.ENTRY_FEE + " coins to pay the entrance fee.");
            dialogManager.startDialog(player, DungeonGuardQuest.getDeclineDialog(), npc, null);
            return;
        }

        economyManager.deductCoins(player, DungeonGuardQuest.ENTRY_FEE);
        CurrencyMessageUtil.sendLoss(player, CurrencyMessageUtil.Currency.COINS, DungeonGuardQuest.ENTRY_FEE);
        dialogManager.startDialog(player, DungeonGuardQuest.getApprovalDialog(), npc, () -> {
            questManager.handleTalk(player, "npc" + DungeonGuardQuest.NPC_ID + "_entry");
            openDungeonGate(player);
        });
    }

    private void openDungeonGate(Player player) {
        QuestGateManager gates = Main.getInstance().getQuestGateManager();
        if (gates == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "The dungeon entrance is unavailable right now.");
            return;
        }
        gates.openGate(player, DungeonGuardQuest.GATE_ID);
    }

    private boolean hasUnlockedDungeonEntrance(Player player) {
        return questManager != null
                && questManager.hasCompleted(player.getUniqueId(), DungeonGuardQuest.QUEST_ID);
    }
}
