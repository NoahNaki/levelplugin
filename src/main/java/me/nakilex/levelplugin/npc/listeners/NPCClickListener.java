package me.nakilex.levelplugin.npc.listeners;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.fakeblock.QuestGateManager;
import me.nakilex.levelplugin.horse.gui.HorseGUI;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.quests.def.DungeonGuardQuest;
import me.nakilex.levelplugin.quests.gui.QuestState;
import me.nakilex.levelplugin.quests.data.PlayerQuestProgress;
import me.nakilex.levelplugin.quests.def.SerasQuest;
import me.nakilex.levelplugin.quests.def.SerasSlimeKingQuest;
import me.nakilex.levelplugin.quests.def.SharpestSecretQuest;
import me.nakilex.levelplugin.quests.def.SalvagersLessonQuest;
import me.nakilex.levelplugin.quests.def.HawieHermitCrabQuest;
import me.nakilex.levelplugin.quests.def.MarketBeginningsQuest;
import me.nakilex.levelplugin.quests.def.FieldworkFavorQuest;
import me.nakilex.levelplugin.quests.def.WakePerryQuest;
import me.nakilex.levelplugin.npc.handlers.EssenceWeaverLessonNpcHandler;
import me.nakilex.levelplugin.npc.handlers.FieldworkFavorNpcHandler;
import me.nakilex.levelplugin.npc.handlers.ForgeFundamentalsNpcHandler;
import me.nakilex.levelplugin.npc.handlers.GamblersGambitNpcHandler;
import me.nakilex.levelplugin.npc.handlers.HawieHermitCrabNpcHandler;
import me.nakilex.levelplugin.npc.handlers.MarketBeginningsNpcHandler;
import me.nakilex.levelplugin.npc.handlers.AbandonedCastleNpcHandler;
import me.nakilex.levelplugin.npc.handlers.QuestNpcInteractionRegistry;
import me.nakilex.levelplugin.npc.handlers.SalvagersLessonNpcHandler;
import me.nakilex.levelplugin.npc.handlers.SerasQuestNpcHandler;
import me.nakilex.levelplugin.npc.handlers.SerasSlimeKingNpcHandler;
import me.nakilex.levelplugin.npc.handlers.SharpestSecretNpcHandler;
import me.nakilex.levelplugin.npc.handlers.StableKeeperNpcHandler;
import me.nakilex.levelplugin.npc.handlers.ZoyaDungeonNpcHandler;
import me.nakilex.levelplugin.npc.dialog.NPCDialogManager;
import me.nakilex.levelplugin.storage.StorageManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.CurrencyMessageUtil;
import me.nakilex.levelplugin.utils.NpcNameUtil;
import me.nakilex.levelplugin.enchanting.gui.EnchantGUI;
import me.nakilex.levelplugin.auctionhouse.AuctionHouseGUI;
import me.nakilex.levelplugin.salvage.gui.SalvageGUI;
import me.nakilex.levelplugin.quests.util.QuestServiceAccessTracker;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.npc.system.NpcApi;
import me.nakilex.levelplugin.npc.system.NPC;
import net.citizensnpcs.api.CitizensAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import java.util.List;
import java.util.UUID;

public class NPCClickListener implements Listener {

    private static final int STORAGE_REGISTRATION_COST = 100;
    private static final List<String> STORAGE_INTRO_DIALOG = List.of(
            "Storage Manager|Looking to keep your belongings safe?",
            "Storage Manager|I can register a personal storage for you for " + STORAGE_REGISTRATION_COST + " coins."
    );
    private static final List<String> STORAGE_DECLINE_DIALOG = List.of(
            "Storage Manager|No worries. Come back if you change your mind."
    );
    private static final List<String> STORAGE_CREATED_DIALOG = List.of(
            "Storage Manager|All set. Your personal storage is ready whenever you need it."
    );
    private static final List<String> STORAGE_FUNDS_DIALOG = List.of(
            "Storage Manager|You'll need " + STORAGE_REGISTRATION_COST + " coins before I can register your personal storage."
    );

    private final EconomyManager economyManager;
    private final QuestManager questManager;
    private final NPCDialogManager dialogManager;
    private final HorseGUI horseGUI;
    private final EnchantGUI enchantGUI;
    private final AuctionHouseGUI auctionGUI;
    private final StorageManager storageManager;
    private final QuestNpcInteractionRegistry questHandlerRegistry;

    // Constructor to get the EconomyManager instance
    public NPCClickListener(EconomyManager economyManager, QuestManager questManager, NPCDialogManager dialogManager,
                            HorseGUI horseGUI, EnchantGUI enchantGUI, AuctionHouseGUI auctionGUI,
                            StorageManager storageManager) {
        this.economyManager = economyManager;
        this.questManager = questManager;
        this.dialogManager = dialogManager;
        this.horseGUI = horseGUI;
        this.enchantGUI = enchantGUI;
        this.auctionGUI = auctionGUI;
        this.storageManager = storageManager;
        this.questHandlerRegistry = new QuestNpcInteractionRegistry();
        registerQuestHandlers();
    }

    @EventHandler
    public void onNPCClick(PlayerInteractEntityEvent event) {
        // Ignore offhand interactions
        if (event.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND) {
            return; // Ignore offhand clicks
        }

        Player player = event.getPlayer();
        NPC npc = NpcApi.getRegistry().getNPC(event.getRightClicked());
        net.citizensnpcs.api.npc.NPC citizensNpc = npc == null
                ? CitizensAPI.getNPCRegistry().getNPC(event.getRightClicked())
                : null;
        if (npc == null && citizensNpc == null) {
            return;
        }

        int npcId = npc != null ? npc.getId() : citizensNpc.getId();
        String npcName = npc != null ? npc.getName() : citizensNpc.getName();

            var serverSelection = Main.getInstance().getServerSelectionManager();
            if (serverSelection != null && npc != null && serverSelection.handleSelectorClick(player, npc)) {
                return;
            }

            dialogManager.recordDialogCooldown(player);

            if (npc != null && WakePerryQuest.handleNpcInteraction(player, npc, event.getHand())) {
                return;
            }

            String stripped = org.bukkit.ChatColor.stripColor(npcName);
            if (npcId == 546) {
                Main.getInstance().getCodexManager().recordNpc(player, stripped);
            }

            if (isNpcName(npcName, SalvagersLessonQuest.NPC_NAME)
                    && questManager.hasCompleted(player.getUniqueId(), SalvagersLessonQuest.ID)) {
                if (QuestServiceAccessTracker.isCoolingDown(player.getUniqueId(), QuestServiceAccessTracker.Service.SALVAGE)) {
                    ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                            "Give the salvager a moment before reopening the bench.");
                    return;
                }
                SalvageGUI.openMerchantGUI(player);
                return;
            }

            if (npcId == 1089 && questManager.hasCompleted(player.getUniqueId(), HawieHermitCrabQuest.ID)) {
                player.performCommand("fishrewards");
                return;
            }

            if (npcId == FieldworkFavorQuest.NPC_ID
                    && questManager.hasCompleted(player.getUniqueId(), FieldworkFavorQuest.ID)) {
                player.performCommand("farmrewards");
                return;
            }

            if (isNpcName(npcName, "Fisherman")) {
                player.performCommand("fishrewards");
                return;
            }

            if (isNpcName(npcName, "Farmer") || isNpcName(npcName, "Baker")) {
                if (npcId == FieldworkFavorQuest.NPC_ID
                        && !questManager.hasCompleted(player.getUniqueId(), FieldworkFavorQuest.ID)) {
                    // Allow the quest handler to run before opening farm rewards.
                } else {
                    player.performCommand("farmrewards");
                    return;
                }
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

            if (isNpcName(npcName, "Tool Merchant")) {
                player.performCommand("merchant tool");
                return;
            }

            if (npcId == 546 &&
                    questManager.hasCompleted(player.getUniqueId(), "newbeginning")) {
                if (!dialogManager.hasSession(player)) {
                    NPC seras = NpcApi.getRegistry().getById(823);
                    net.citizensnpcs.api.npc.NPC serasCitizen = CitizensAPI.getNPCRegistry().getById(823);
                    String coords = "unknown";
                    if (seras != null) {
                        Location l = seras.isSpawned() ? seras.getEntity().getLocation() : seras.getStoredLocation();
                        if (l != null) {
                            coords = l.getBlockX() + ", " + l.getBlockY() + ", " + l.getBlockZ();
                        }
                    } else if (serasCitizen != null) {
                        Location l = serasCitizen.isSpawned() ? serasCitizen.getEntity().getLocation() : serasCitizen.getStoredLocation();
                        if (l != null) {
                            coords = l.getBlockX() + ", " + l.getBlockY() + ", " + l.getBlockZ();
                        }
                    }
                    String line = "Piwan|You should talk to Seras at §8[§e" + coords + "§8]§f, I'm sure she has plenty of tasks for you, though be wary she's a fiery one.";
                    startDialog(player, java.util.List.of(line), npc, citizensNpc, null);
                }
            }

            if (dialogManager.hasSession(player)) {
                if (dialogManager.isSessionNpc(player, npcId)) {
                    dialogManager.advanceDialog(player, questManager);
                }
                return;
            }

            if (isNpcName(npcName, "Storage Manager")) {
                handleStorageManagerInteraction(player, npc, citizensNpc);
                return;
            }

            if (npcId == DungeonGuardQuest.NPC_ID) {
                handleDungeonGuard(player, npc, citizensNpc);
                return;
            }

            Quest quest = npc != null
                    ? questManager.getQuestByNpc(npc, player)
                    : questManager.getQuestByNpc(citizensNpc, player);
            if (npcId == SerasQuest.NPC_ID) {
                Quest serasPartTwo = questManager.getQuestById(SerasSlimeKingQuest.ID);
                if (serasPartTwo != null && !questManager.hasCompleted(player.getUniqueId(), serasPartTwo.getId())) {
                    PlayerQuestProgress partTwoProgress = questManager.getProgress(player.getUniqueId(), serasPartTwo.getId());
                    QuestState partTwoState = questManager.getQuestState(player, serasPartTwo);
                    if (partTwoProgress != null || partTwoState == QuestState.AVAILABLE) {
                        quest = serasPartTwo;
                    }
                }

                if (quest == null) {
                    quest = questManager.getQuestById(SerasQuest.ID);
                }
            }
            if (quest == null && isNpcName(npcName, SalvagersLessonQuest.NPC_NAME)) {
                quest = questManager.getQuestById(SalvagersLessonQuest.ID);
            }
            if (quest != null) {
                QuestState state = questManager.getQuestState(player, quest);
                if (npc != null) {
                    if (questHandlerRegistry.handle(player, npc, quest, state, questManager, dialogManager)) {
                        return;
                    }
                }

                questManager.handleTalk(player, resolveTalkTarget(player.getUniqueId(), quest, npcId, npcName));
                switch (state) {
                    case AVAILABLE -> startDialog(player, quest, npc, citizensNpc);
                    case LOCKED -> questManager.meetsRequirements(player, quest);
                    case ACCEPTED, IN_PROGRESS -> player.sendMessage("§cComplete the quest first!");
                    default -> {}
                }
            }
    }

    private void handleStorageManagerInteraction(Player player, NPC npc, net.citizensnpcs.api.npc.NPC citizensNpc) {
        if (storageManager == null) {
            return;
        }

        if (storageManager.hasStorage(player.getUniqueId())) {
            storageManager.openStorage(player);
            return;
        }

        startDialog(player, STORAGE_INTRO_DIALOG, npc, citizensNpc,
                () -> Bukkit.getScheduler().runTaskLater(Main.getInstance(), () ->
                        startChoiceDialog(player, npc, citizensNpc, List.of("Yes", "No"), choice -> {
                            if (choice == 0) {
                                completeStorageRegistration(player, npc, citizensNpc);
                            } else {
                                startDialog(player, STORAGE_DECLINE_DIALOG, npc, citizensNpc, null);
                            }
                        }), 1L));
    }

    private void completeStorageRegistration(Player player, NPC npc, net.citizensnpcs.api.npc.NPC citizensNpc) {
        if (storageManager.hasStorage(player.getUniqueId())) {
            storageManager.openStorage(player);
            return;
        }

        if (economyManager.getBalance(player) < STORAGE_REGISTRATION_COST) {
            startDialog(player, STORAGE_FUNDS_DIALOG, npc, citizensNpc, null);
            return;
        }

        economyManager.deductCoins(player, STORAGE_REGISTRATION_COST);
        CurrencyMessageUtil.sendLoss(player, CurrencyMessageUtil.Currency.COINS, STORAGE_REGISTRATION_COST);
        storageManager.createStorage(player.getUniqueId());
        startDialog(player, STORAGE_CREATED_DIALOG, npc, citizensNpc,
                () -> storageManager.openStorage(player));
    }

    private void handleDungeonGuard(Player player, NPC npc, net.citizensnpcs.api.npc.NPC citizensNpc) {
        if (StatsManager.getInstance().getLevel(player) < DungeonGuardQuest.REQUIRED_LEVEL) {
            startDialog(player, DungeonGuardQuest.getTooWeakDialog(), npc, citizensNpc, null);
            return;
        }

        ensureDungeonGuardQuestStarted(player);

        if (hasUnlockedDungeonEntrance(player)) {
            startDialog(player,
                    DungeonGuardQuest.getApprovalDialog(),
                    npc,
                    citizensNpc,
                    () -> openDungeonGate(player));
            return;
        }

        if (resumePendingChoice(player, npc, citizensNpc)) {
            return;
        }

        startDialog(player, DungeonGuardQuest.getIntroDialog(), npc, citizensNpc,
                () -> Bukkit.getScheduler().runTaskLater(Main.getInstance(), () ->
                        startChoiceDialog(player, npc, citizensNpc,
                                java.util.List.of("Yes", "No"),
                                DungeonGuardQuest.QUEST_ID,
                                "dungeonguard_choice_",
                                choice -> {
                                    if (choice == 0) {
                                        processDungeonEntryPurchase(player, npc, citizensNpc);
                                    } else {
                                        startDialog(player,
                                                DungeonGuardQuest.getDeclineDialog(),
                                                npc,
                                                citizensNpc,
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

    private void processDungeonEntryPurchase(Player player, NPC npc, net.citizensnpcs.api.npc.NPC citizensNpc) {
        int balance = economyManager.getBalance(player);
        if (balance < DungeonGuardQuest.ENTRY_FEE) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "You need " + DungeonGuardQuest.ENTRY_FEE + " coins to pay the entrance fee.");
            startDialog(player, DungeonGuardQuest.getDeclineDialog(), npc, citizensNpc, null);
            return;
        }

        economyManager.deductCoins(player, DungeonGuardQuest.ENTRY_FEE);
        CurrencyMessageUtil.sendLoss(player, CurrencyMessageUtil.Currency.COINS, DungeonGuardQuest.ENTRY_FEE);
        startDialog(player, DungeonGuardQuest.getApprovalDialog(), npc, citizensNpc, () -> {
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

    private void registerQuestHandlers() {
        questHandlerRegistry
                .register(new ForgeFundamentalsNpcHandler(questManager, dialogManager))
                .register(new EssenceWeaverLessonNpcHandler(questManager, dialogManager))
                .register(new GamblersGambitNpcHandler(questManager, dialogManager, economyManager))
                .register(new AbandonedCastleNpcHandler(questManager, dialogManager))
                .register(new HawieHermitCrabNpcHandler(questManager, dialogManager))
                .register(new FieldworkFavorNpcHandler(questManager, dialogManager))
                .register(new MarketBeginningsNpcHandler(questManager, dialogManager, auctionGUI))
                .register(new SalvagersLessonNpcHandler(questManager, dialogManager))
                .register(new SerasQuestNpcHandler(questManager, dialogManager))
                .register(new SerasSlimeKingNpcHandler(questManager, dialogManager))
                .register(new SharpestSecretNpcHandler(questManager, dialogManager, enchantGUI))
                .register(new StableKeeperNpcHandler(questManager, dialogManager, horseGUI))
                .register(new ZoyaDungeonNpcHandler(questManager, dialogManager));
    }

    private boolean isNpcName(String npcName, String expectedName) {
        if (npcName == null || expectedName == null) {
            return false;
        }
        return NpcNameUtil.equalsNormalized(npcName, expectedName);
    }

    private String resolveTalkTarget(UUID playerId, Quest quest, int npcId, String npcName) {
        if (quest != null && SharpestSecretQuest.ID.equals(quest.getId())) {
            if (isNpcName(npcName, SharpestSecretQuest.NPC_KAZAN_NAME)) {
                return SharpestSecretQuest.NPC_INTRO_TARGET;
            }
            if (isNpcName(npcName, SharpestSecretQuest.NPC_OSIRIS_NAME)) {
                return SharpestSecretQuest.NPC_OSIRIS_TARGET;
            }
        }
        if (quest != null && SalvagersLessonQuest.ID.equals(quest.getId())
                && isNpcName(npcName, SalvagersLessonQuest.NPC_NAME)) {
            PlayerQuestProgress progress = questManager.getProgress(playerId, SalvagersLessonQuest.ID);
            if (progress != null &&
                    progress.getProgress(SalvagersLessonQuest.SALVAGE_INDEX) >= SalvagersLessonQuest.SALVAGE_AMOUNT) {
                return SalvagersLessonQuest.RETURN_TARGET;
            }
            return SalvagersLessonQuest.INTRO_TARGET;
        }
        if (quest != null && MarketBeginningsQuest.ID.equals(quest.getId())
                && isNpcName(npcName, MarketBeginningsQuest.NPC_NAME)) {
            PlayerQuestProgress progress = questManager.getProgress(playerId, MarketBeginningsQuest.ID);
            if (progress != null && progress.getProgress(MarketBeginningsQuest.BID_INDEX) >= 1
                    && progress.getProgress(MarketBeginningsQuest.LIST_INDEX) >= 1) {
                return MarketBeginningsQuest.RETURN_TARGET;
            }
            return MarketBeginningsQuest.INTRO_TARGET;
        }
        return "npc" + npcId;
    }

    private void startDialog(Player player, Quest quest, NPC npc, net.citizensnpcs.api.npc.NPC citizensNpc) {
        if (npc != null) {
            dialogManager.startDialog(player, quest, npc);
        } else {
            dialogManager.startDialog(player, quest, citizensNpc);
        }
    }

    private void startDialog(Player player, List<String> lines, NPC npc, net.citizensnpcs.api.npc.NPC citizensNpc,
                             Runnable finish) {
        if (npc != null) {
            dialogManager.startDialog(player, lines, npc, finish);
        } else {
            dialogManager.startDialog(player, lines, citizensNpc, finish);
        }
    }

    private void startChoiceDialog(Player player, NPC npc, net.citizensnpcs.api.npc.NPC citizensNpc,
                                   List<String> options, java.util.function.Consumer<Integer> callback) {
        if (npc != null) {
            dialogManager.startChoiceDialog(player, npc, options, callback);
        } else {
            dialogManager.startChoiceDialog(player, citizensNpc, options, callback);
        }
    }

    private void startChoiceDialog(Player player, NPC npc, net.citizensnpcs.api.npc.NPC citizensNpc,
                                   List<String> options, String questId, String flagBase,
                                   java.util.function.Consumer<Integer> callback) {
        if (npc != null) {
            dialogManager.startChoiceDialog(player, npc, options, questId, flagBase, callback);
        } else {
            dialogManager.startChoiceDialog(player, citizensNpc, options, questId, flagBase, callback);
        }
    }

    private boolean resumePendingChoice(Player player, NPC npc, net.citizensnpcs.api.npc.NPC citizensNpc) {
        if (npc != null) {
            return dialogManager.resumePendingChoice(player, npc);
        }
        return dialogManager.resumePendingChoice(player, citizensNpc);
    }
}
