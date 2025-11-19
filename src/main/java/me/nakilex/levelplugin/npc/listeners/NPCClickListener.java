package me.nakilex.levelplugin.npc.listeners;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.horse.gui.HorseGUI;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.quests.def.ZoyaDungeonQuest;
import me.nakilex.levelplugin.quests.gui.QuestState;
import me.nakilex.levelplugin.quests.data.PlayerQuestProgress;
import me.nakilex.levelplugin.quests.def.SerasQuest;
import me.nakilex.levelplugin.quests.def.SharpestSecretQuest;
import me.nakilex.levelplugin.quests.def.StableKeeperQuest;
import me.nakilex.levelplugin.npc.dialog.NPCDialogManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
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
    private final HorseGUI horseGUI;

    // Constructor to get the EconomyManager instance
    public NPCClickListener(EconomyManager economyManager, QuestManager questManager, NPCDialogManager dialogManager,
                            HorseGUI horseGUI) {
        this.economyManager = economyManager;
        this.questManager = questManager;
        this.dialogManager = dialogManager;
        this.horseGUI = horseGUI;
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

    private boolean handleSharpestSecret(Player player, NPC npc) {
        java.util.UUID uuid = player.getUniqueId();
        boolean completed = questManager.hasCompleted(uuid, SharpestSecretQuest.ID);
        PlayerQuestProgress progress = questManager.getProgress(uuid, SharpestSecretQuest.ID);

        if (npc.getId() == SharpestSecretQuest.NPC_KAZAN_ID) {
            if (completed) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                        "Osiris owes you a tasting whenever you need another edge.");
                return true;
            }
            if (progress == null) {
                return false;
            }

            boolean waitDone = progress.getProgress(SharpestSecretQuest.WAIT_FOR_NIGHT_INDEX) >= 1;
            boolean orchidFound = progress.getProgress(SharpestSecretQuest.FIND_ORCHID_INDEX) >= 1;
            boolean returned = progress.getProgress(SharpestSecretQuest.TALK_RETURN_INDEX) >= 1;
            boolean osirisSpoken = progress.getProgress(SharpestSecretQuest.TALK_OSIRIS_INDEX) >= 1;

            if (!waitDone) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                        "Wait for nightfall within the city walls before the orchid reveals itself.");
                return true;
            }

            if (!orchidFound) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                        "Search the courtyards near the west gate while the moon is high.");
                return true;
            }

            if (orchidFound && !returned) {
                dialogManager.startDialog(player,
                        SharpestSecretQuest.getReturnDialog(),
                        npc,
                        () -> questManager.handleTalk(player, SharpestSecretQuest.NPC_RETURN_TARGET));
                return true;
            }

            if (returned && !osirisSpoken) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                        "Find Osiris at the west entrance and tell him you're here for the tasting.");
                return true;
            }

            if (osirisSpoken) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                        "Osiris is waiting at his table—place an item on it and type /enchant.");
                return true;
            }
        }

        if (npc.getId() == SharpestSecretQuest.NPC_OSIRIS_ID) {
            if (completed) {
                dialogManager.startDialog(player,
                        SharpestSecretQuest.getOsirisReminderDialog(),
                        npc,
                        null);
                return true;
            }

            if (progress == null) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                        "Osiris glances past you—Kazan hasn't vouched for you yet.");
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
                                                questManager.handleTalk(player, SharpestSecretQuest.NPC_OSIRIS_TARGET);
                                                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                                                        "Correct. Use /enchant to open the workshop.");
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
                    null);
            return true;
        }

        return false;
    }
}
