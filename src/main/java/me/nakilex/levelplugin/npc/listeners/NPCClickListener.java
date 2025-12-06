package me.nakilex.levelplugin.npc.listeners;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.fakeblock.QuestGateManager;
import me.nakilex.levelplugin.horse.gui.HorseGUI;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.quests.def.DungeonGuardQuest;
import me.nakilex.levelplugin.quests.def.ZoyaDungeonQuest;
import me.nakilex.levelplugin.quests.gui.QuestState;
import me.nakilex.levelplugin.quests.data.PlayerQuestProgress;
import me.nakilex.levelplugin.quests.def.SerasQuest;
import me.nakilex.levelplugin.quests.def.SharpestSecretQuest;
import me.nakilex.levelplugin.quests.def.StableKeeperQuest;
import me.nakilex.levelplugin.quests.def.SalvagersLessonQuest;
import me.nakilex.levelplugin.quests.def.MarketBeginningsQuest;
import me.nakilex.levelplugin.quests.def.ForgeFundamentalsQuest;
import me.nakilex.levelplugin.quests.def.EssenceWeaversLessonQuest;
import me.nakilex.levelplugin.npc.dialog.NPCDialogManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.CurrencyMessageUtil;
import me.nakilex.levelplugin.utils.NpcNameUtil;
import me.nakilex.levelplugin.enchanting.gui.EnchantGUI;
import me.nakilex.levelplugin.auctionhouse.AuctionHouseGUI;
import me.nakilex.levelplugin.salvage.gui.SalvageGUI;
import me.nakilex.levelplugin.quests.util.QuestServiceAccessTracker;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import java.util.UUID;

public class NPCClickListener implements Listener {

    private final EconomyManager economyManager;
    private final QuestManager questManager;
    private final NPCDialogManager dialogManager;
    private final HorseGUI horseGUI;
    private final EnchantGUI enchantGUI;
    private final AuctionHouseGUI auctionGUI;

    // Constructor to get the EconomyManager instance
    public NPCClickListener(EconomyManager economyManager, QuestManager questManager, NPCDialogManager dialogManager,
                            HorseGUI horseGUI, EnchantGUI enchantGUI, AuctionHouseGUI auctionGUI) {
        this.economyManager = economyManager;
        this.questManager = questManager;
        this.dialogManager = dialogManager;
        this.horseGUI = horseGUI;
        this.enchantGUI = enchantGUI;
        this.auctionGUI = auctionGUI;
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

            if (isNpcName(npc, SalvagersLessonQuest.NPC_NAME)
                    && questManager.hasCompleted(player.getUniqueId(), SalvagersLessonQuest.ID)) {
                if (QuestServiceAccessTracker.isCoolingDown(player.getUniqueId(), QuestServiceAccessTracker.Service.SALVAGE)) {
                    ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                            "Give the salvager a moment before reopening the bench.");
                    return;
                }
                SalvageGUI.openMerchantGUI(player);
                return;
            }
            if (stripped.equalsIgnoreCase("Starter Merchant")) {
                PlayerQuestProgress prog = questManager.getProgress(player.getUniqueId(), "newbeginning");
                if (prog == null || questManager.hasCompleted(player.getUniqueId(), "newbeginning")) {
                    player.performCommand("merchant starter_shop");
                    return;
                }
            }

            if (stripped.equalsIgnoreCase("Potion Merchant")) {
                player.performCommand("merchant potion_merchant");
                return;
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

            Quest quest = questManager.getQuestByNpc(npc);
            if (quest == null && isNpcName(npc, SalvagersLessonQuest.NPC_NAME)) {
                quest = questManager.getQuestById(SalvagersLessonQuest.ID);
            }
            if (quest != null) {
                if (ForgeFundamentalsQuest.ID.equals(quest.getId())) {
                    if (handleForgeFundamentals(player, npc, quest)) {
                        return;
                    }
                }

                if (EssenceWeaversLessonQuest.ID.equals(quest.getId())) {
                    if (handleEssenceWeaverLesson(player, npc, quest)) {
                        return;
                    }
                }

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

                if (StableKeeperQuest.ID.equals(quest.getId())) {
                    if (handleStableKeeper(player, npc, quest)) {
                        return;
                    }
                }

                if (SharpestSecretQuest.ID.equals(quest.getId())) {
                    if (handleSharpestSecret(player, npc)) {
                        return;
                    }
                }

                if (SalvagersLessonQuest.ID.equals(quest.getId())) {
                    if (handleSalvagersLesson(player, npc, quest)) {
                        return;
                    }
                }

                if (MarketBeginningsQuest.ID.equals(quest.getId())) {
                    if (handleMarketBeginnings(player, npc, quest)) {
                        return;
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

                questManager.handleTalk(player, resolveTalkTarget(player.getUniqueId(), quest, npc));
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

    private boolean handleForgeFundamentals(Player player, NPC npc, Quest quest) {
        QuestState state = questManager.getQuestState(player, quest);
        if (state == QuestState.AVAILABLE) {
            dialogManager.startDialog(player, quest, npc);
            return true;
        }
        if (state == QuestState.LOCKED) {
            questManager.meetsRequirements(player, quest);
            return true;
        }

        java.util.UUID uuid = player.getUniqueId();
        PlayerQuestProgress progress = questManager.getProgress(uuid, quest.getId());
        boolean introDone = progress != null && progress.getProgress(0) >= 1;
        boolean serviceDone = progress != null && progress.getProgress(1) >= 1;
        boolean returned = progress != null && progress.getProgress(2) >= 1;

        if (!introDone && progress != null) {
            dialogManager.startDialog(player,
                    quest.getDialogLines(),
                    npc,
                    () -> questManager.handleTalk(player, ForgeFundamentalsQuest.NPC_NAME.equalsIgnoreCase(npc.getName())
                            ? "npc_blacksmith_intro"
                            : "npc" + npc.getId()));
            return true;
        }

        if (returned || questManager.hasCompleted(uuid, ForgeFundamentalsQuest.ID)) {
            player.performCommand("blacksmith");
            return true;
        }

        if (!serviceDone) {
            player.performCommand("blacksmith");
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    "Use repair, reroll, or upgrade once, then check back with the Blacksmith.");
            return true;
        }

        dialogManager.startDialog(player,
                ForgeFundamentalsQuest.getReturnDialog(),
                npc,
                () -> questManager.handleTalk(player, "npc_blacksmith_return"));
        return true;
    }

    private boolean handleEssenceWeaverLesson(Player player, NPC npc, Quest quest) {
        QuestState state = questManager.getQuestState(player, quest);
        if (state == QuestState.AVAILABLE) {
            dialogManager.startDialog(player, quest, npc);
            return true;
        }
        if (state == QuestState.LOCKED) {
            questManager.meetsRequirements(player, quest);
            return true;
        }

        java.util.UUID uuid = player.getUniqueId();
        PlayerQuestProgress progress = questManager.getProgress(uuid, quest.getId());
        boolean introDone = progress != null && progress.getProgress(0) >= 1;
        boolean upgradeTried = progress != null && progress.getProgress(1) >= 1;
        boolean returned = progress != null && progress.getProgress(2) >= 1;

        if (!introDone && progress != null) {
            dialogManager.startDialog(player,
                    quest.getDialogLines(),
                    npc,
                    () -> questManager.handleTalk(player, EssenceWeaversLessonQuest.NPC_NAME.equalsIgnoreCase(npc.getName())
                            ? "npc_essence_weaver_intro"
                            : "npc" + npc.getId()));
            return true;
        }

        if (returned || questManager.hasCompleted(uuid, EssenceWeaversLessonQuest.ID)) {
            player.performCommand("essenceupgrade");
            return true;
        }

        if (!upgradeTried) {
            player.performCommand("essenceupgrade");
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    "Invest a duplicate essence or attempt a star upgrade, then speak with the Essence Weaver again.");
            return true;
        }

        dialogManager.startDialog(player,
                EssenceWeaversLessonQuest.getReturnDialog(),
                npc,
                () -> questManager.handleTalk(player, "npc_essence_weaver_return"));
        return true;
    }

    private boolean handleStableKeeper(Player player, NPC npc, Quest quest) {
        java.util.UUID uuid = player.getUniqueId();
        if (questManager.hasCompleted(uuid, StableKeeperQuest.ID)) {
            horseGUI.openHorseMenu(player);
            return true;
        }

        PlayerQuestProgress progress = questManager.getProgress(uuid, StableKeeperQuest.ID);
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
            player.sendMessage("§ePick a horse from the stable, then talk to the Stable Keeper again.");
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

    private boolean handleSharpestSecret(Player player, NPC npc) {
        java.util.UUID uuid = player.getUniqueId();
        boolean completed = questManager.hasCompleted(uuid, SharpestSecretQuest.ID);
        PlayerQuestProgress progress = questManager.getProgress(uuid, SharpestSecretQuest.ID);

        if (isNpcName(npc, SharpestSecretQuest.NPC_KAZAN_NAME)) {
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

            if (!waitDone) {
                return true;
            }

            if (!orchidFound) {
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

        if (isNpcName(npc, SharpestSecretQuest.NPC_OSIRIS_NAME)) {
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
                        () -> Bukkit.getScheduler().runTaskLater(Main.getInstance(), () ->
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

    private boolean handleSalvagersLesson(Player player, NPC npc, Quest quest) {
        QuestState state = questManager.getQuestState(player, quest);
        if (state == QuestState.AVAILABLE) {
            dialogManager.startDialog(player, quest, npc);
            return true;
        }
        if (state == QuestState.LOCKED) {
            questManager.meetsRequirements(player, quest);
            return true;
        }

        java.util.UUID uuid = player.getUniqueId();
        boolean completed = questManager.hasCompleted(uuid, SalvagersLessonQuest.ID);
        PlayerQuestProgress progress = questManager.getProgress(uuid, SalvagersLessonQuest.ID);
        boolean introDone = progress != null && progress.getProgress(SalvagersLessonQuest.TALK_INTRO_INDEX) >= 1;
        boolean salvaged = progress != null &&
                progress.getProgress(SalvagersLessonQuest.SALVAGE_INDEX) >= SalvagersLessonQuest.SALVAGE_AMOUNT;
        boolean returned = progress != null && progress.getProgress(SalvagersLessonQuest.TALK_RETURN_INDEX) >= 1;
        boolean cooling = QuestServiceAccessTracker.isCoolingDown(uuid, QuestServiceAccessTracker.Service.SALVAGE);

        if (!introDone && progress != null) {
            dialogManager.startDialog(player,
                    quest.getDialogLines(),
                    npc,
                    () -> questManager.handleTalk(player, SalvagersLessonQuest.INTRO_TARGET));
            return true;
        }

        if (completed || returned) {
            if (cooling) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                        "Give the salvager a moment before reopening the bench.");
                return true;
            }
            SalvageGUI.openMerchantGUI(player);
            return true;
        }

        if (!salvaged) {
            if (!cooling) {
                SalvageGUI.openMerchantGUI(player);
            } else {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                        "Let the salvager finish up before trying again.");
            }
            return true;
        }

        dialogManager.startDialog(player,
                SalvagersLessonQuest.getReturnDialog(),
                npc,
                () -> questManager.handleTalk(player, SalvagersLessonQuest.RETURN_TARGET));
        return true;
    }

    private boolean handleMarketBeginnings(Player player, NPC npc, Quest quest) {
        QuestState state = questManager.getQuestState(player, quest);
        if (state == QuestState.AVAILABLE) {
            dialogManager.startDialog(player, quest, npc);
            return true;
        }
        if (state == QuestState.LOCKED) {
            questManager.meetsRequirements(player, quest);
            return true;
        }

        java.util.UUID uuid = player.getUniqueId();
        boolean completed = questManager.hasCompleted(uuid, MarketBeginningsQuest.ID);
        PlayerQuestProgress progress = questManager.getProgress(uuid, MarketBeginningsQuest.ID);
        boolean introDone = progress != null && progress.getProgress(MarketBeginningsQuest.TALK_INTRO_INDEX) >= 1;
        boolean listed = progress != null && progress.getProgress(MarketBeginningsQuest.LIST_INDEX) >= 1;
        boolean bid = progress != null && progress.getProgress(MarketBeginningsQuest.BID_INDEX) >= 1;
        boolean returned = progress != null && progress.getProgress(MarketBeginningsQuest.TALK_RETURN_INDEX) >= 1;
        boolean cooling = QuestServiceAccessTracker.isCoolingDown(uuid, QuestServiceAccessTracker.Service.AUCTION);

        if (!introDone && progress != null) {
            dialogManager.startDialog(player,
                    quest.getDialogLines(),
                    npc,
                    () -> questManager.handleTalk(player, MarketBeginningsQuest.INTRO_TARGET));
            return true;
        }

        if (completed || returned) {
            if (cooling) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                        "Hold on, the auctioneer is sorting paperwork.");
                return true;
            }
            if (auctionGUI != null) {
                auctionGUI.open(player);
            }
            return true;
        }

        if (!listed || !bid) {
            if (!cooling) {
                if (auctionGUI != null) {
                    auctionGUI.open(player);
                }
            } else {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                        "Give the auction house a moment before reopening.");
            }
            return true;
        }

        dialogManager.startDialog(player,
                MarketBeginningsQuest.getReturnDialog(),
                npc,
                () -> questManager.handleTalk(player, MarketBeginningsQuest.RETURN_TARGET));
        return true;
    }

    private boolean isNpcName(NPC npc, String expectedName) {
        if (npc == null || expectedName == null) {
            return false;
        }
        return NpcNameUtil.equalsNormalized(npc.getName(), expectedName);
    }

    private String resolveTalkTarget(UUID playerId, Quest quest, NPC npc) {
        if (quest != null && SharpestSecretQuest.ID.equals(quest.getId())) {
            if (isNpcName(npc, SharpestSecretQuest.NPC_KAZAN_NAME)) {
                return SharpestSecretQuest.NPC_INTRO_TARGET;
            }
            if (isNpcName(npc, SharpestSecretQuest.NPC_OSIRIS_NAME)) {
                return SharpestSecretQuest.NPC_OSIRIS_TARGET;
            }
        }
        if (quest != null && SalvagersLessonQuest.ID.equals(quest.getId())
                && isNpcName(npc, SalvagersLessonQuest.NPC_NAME)) {
            PlayerQuestProgress progress = questManager.getProgress(playerId, SalvagersLessonQuest.ID);
            if (progress != null &&
                    progress.getProgress(SalvagersLessonQuest.SALVAGE_INDEX) >= SalvagersLessonQuest.SALVAGE_AMOUNT) {
                return SalvagersLessonQuest.RETURN_TARGET;
            }
            return SalvagersLessonQuest.INTRO_TARGET;
        }
        if (quest != null && MarketBeginningsQuest.ID.equals(quest.getId())
                && isNpcName(npc, MarketBeginningsQuest.NPC_NAME)) {
            PlayerQuestProgress progress = questManager.getProgress(playerId, MarketBeginningsQuest.ID);
            if (progress != null && progress.getProgress(MarketBeginningsQuest.BID_INDEX) >= 1
                    && progress.getProgress(MarketBeginningsQuest.LIST_INDEX) >= 1) {
                return MarketBeginningsQuest.RETURN_TARGET;
            }
            return MarketBeginningsQuest.INTRO_TARGET;
        }
        return "npc" + npc.getId();
    }
}
