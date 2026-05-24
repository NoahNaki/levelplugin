package me.nakilex.levelplugin.npc.listeners;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.fakeblock.QuestGateManager;
import me.nakilex.levelplugin.horse.gui.HorseGUI;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.data.QuestObjective;
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
import me.nakilex.levelplugin.quests.util.QuestNavigationUtil;
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

    @EventHandler(ignoreCancelled = true)
    public void onNPCClick(PlayerInteractEntityEvent event) {
        // Ignore offhand interactions
        if (event.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND) {
            return; // Ignore offhand clicks
        }

        Player player = event.getPlayer();
        NPC npc = NpcApi.getRegistry().getNPC(event.getRightClicked());
        if (npc == null) {
            return;
        }
        net.citizensnpcs.api.npc.NPC citizensNpc = null;

        int npcId = npc.getId();
        String npcName = npc.getName();

        if (questManager.isDebug()) {
            logQuestNpcClickDebug(player, npc, citizensNpc, npcId, npcName);
        }

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
                long remain = Math.max(1L, Math.round(QuestServiceAccessTracker.getRemainingMs(
                        player.getUniqueId(), QuestServiceAccessTracker.Service.SALVAGE) / 1000.0));
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                        "Give the salvager a moment before reopening the bench (" + remain + "s).");
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
                String coords = "unknown";
                if (seras != null) {
                    Location l = seras.isSpawned() ? seras.getEntity().getLocation() : seras.getStoredLocation();
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

        Quest quest = questManager.getQuestByNpc(npc, player);
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
            if (questHandlerRegistry.handle(player, npc, citizensNpc, quest, state, questManager, dialogManager)) {
                return;
            }

            switch (state) {
                case AVAILABLE -> {
                    questManager.handleTalk(player, resolveTalkTarget(player.getUniqueId(), quest, npcId, npcName));
                    startDialog(player, quest, npc, citizensNpc);
                }
                case LOCKED -> questManager.meetsRequirements(player, quest);
                case ACCEPTED, IN_PROGRESS -> {
                    int objIndex = QuestNavigationUtil.resolveObjectiveIndex(quest,
                            questManager.getProgress(player.getUniqueId(), quest.getId()));
                    if (objIndex >= 0 && objIndex < quest.getObjectives().size()) {
                        QuestObjective obj = quest.getObjectives().get(objIndex);
                        if (questManager.isTalkObjectiveForNpc(obj, npcId, npcName)) {
                            questManager.handleTalk(player, resolveTalkTarget(player.getUniqueId(), quest, npcId, npcName));
                            return;
                        }
                    }
                    player.sendMessage("§cComplete the quest first!");
                }
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
        if (quest != null) {
            PlayerQuestProgress progress = questManager.getProgress(playerId, quest.getId());
            int objectiveIndex = QuestNavigationUtil.resolveObjectiveIndex(quest, progress);
            if (objectiveIndex >= 0 && objectiveIndex < quest.getObjectives().size()) {
                QuestObjective objective = quest.getObjectives().get(objectiveIndex);
                if (questManager.isTalkObjectiveForNpc(objective, npcId, npcName)) {
                    return objective.getTarget();
                }
            }
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

    private void logQuestNpcClickDebug(Player player, NPC npc, net.citizensnpcs.api.npc.NPC citizensNpc,
                                       int npcId, String npcName) {
        String source = npc != null ? "level" : "citizens";
        Quest questForNpc = npc != null
                ? questManager.getQuestByNpc(npc, player)
                : questManager.getQuestByNpc(citizensNpc, player);
        String questForNpcId = questForNpc != null ? questForNpc.getId() : "none";
        String trackedId = questManager.getTrackedQuest(player.getUniqueId());
        Quest trackedQuest = trackedId != null ? questManager.getQuest(trackedId) : null;
        PlayerQuestProgress progress = trackedId != null
                ? questManager.getProgress(player.getUniqueId(), trackedId)
                : null;
        int objectiveIndex = trackedQuest != null
                ? QuestNavigationUtil.resolveObjectiveIndex(trackedQuest, progress)
                : -1;
        String objectiveTarget = trackedQuest != null && objectiveIndex >= 0
                ? trackedQuest.getObjectives().get(objectiveIndex).getTarget()
                : null;
        Integer objectiveNpcId = parseNpcId(objectiveTarget);
        String normalizedObjectiveName = objectiveNpcId == null ? NpcNameUtil.normalize(objectiveTarget) : null;
        if (trackedQuest != null && objectiveIndex >= 0) {
            var beaconTarget = trackedQuest.getObjectives().get(objectiveIndex).getBeaconTarget();
            if (beaconTarget instanceof me.nakilex.levelplugin.quests.data.NpcBeaconTarget npcBeaconTarget) {
                if (npcBeaconTarget.getNpcId() != null) {
                    objectiveNpcId = npcBeaconTarget.getNpcId();
                    normalizedObjectiveName = null;
                } else if (npcBeaconTarget.getNormalizedName() != null) {
                    normalizedObjectiveName = npcBeaconTarget.getNormalizedName();
                }
            }
        }
        String normalizedNpcName = NpcNameUtil.normalize(npcName);
        boolean idMatches = objectiveNpcId != null && objectiveNpcId == npcId;
        boolean nameMatches = normalizedObjectiveName != null && normalizedNpcName != null
                && normalizedObjectiveName.equals(normalizedNpcName);
        Main.getInstance().getLogger().info("[QuestDebug] NPC click player=" + player.getName()
                + " source=" + source
                + " npcId=" + npcId
                + " npcName=" + npcName
                + " questForNpc=" + questForNpcId
                + " trackedQuest=" + (trackedQuest != null ? trackedQuest.getId() : "none")
                + " trackedObjectiveIndex=" + objectiveIndex
                + " trackedObjectiveTarget=" + (objectiveTarget != null ? objectiveTarget : "none")
                + " trackedObjectiveNpcId=" + (objectiveNpcId != null ? objectiveNpcId : "none")
                + " idMatchesTracked=" + idMatches
                + " nameMatchesTracked=" + nameMatches);
    }

    private Integer parseNpcId(String target) {
        if (target == null) {
            return null;
        }
        String lower = target.toLowerCase();
        if (!lower.startsWith("npc")) {
            return null;
        }
        String idPart = lower.substring(3);
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < idPart.length(); i++) {
            char c = idPart.charAt(i);
            if (Character.isDigit(c)) {
                digits.append(c);
            } else {
                break;
            }
        }
        if (digits.length() == 0) {
            return null;
        }
        try {
            return Integer.parseInt(digits.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
