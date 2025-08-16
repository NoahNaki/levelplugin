package me.nakilex.levelplugin.npc.listeners;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.quests.gui.QuestState;
import me.nakilex.levelplugin.quests.data.PlayerQuestProgress;
import me.nakilex.levelplugin.quests.data.QuestObjective;
import me.nakilex.levelplugin.quests.data.QuestObjectiveType;
import me.nakilex.levelplugin.quests.def.SerasQuest;
import me.nakilex.levelplugin.npc.dialog.NPCDialogManager;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
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
                    dialogManager.startDialog(player,
                            java.util.List.of("Piwan|You should talk to Seras at <location>, I'm sure she has plenty of tasks for you, though be wary she's a fiery one."),
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
                        QuestObjective killObj = quest.getObjectives().get(0);
                        boolean killDone = killObj.getType() == QuestObjectiveType.KILL &&
                                progress.getProgress(0) >= killObj.getAmount();
                        if (killDone) {
                            if (progress.getProgress(1) < 1) {
                                dialogManager.startDialog(player,
                                        me.nakilex.levelplugin.quests.def.SerasQuest.getTurnInDialog(),
                                        npc,
                                        () -> questManager.handleTalk(player, "npc" + npc.getId()));
                            }
                            return;
                        }
                        // kill not yet done, block talk progress
                        QuestState state = questManager.getQuestState(player, quest);
                        switch (state) {
                            case AVAILABLE -> dialogManager.startDialog(player, quest, npc);
                            case LOCKED -> questManager.meetsRequirements(player, quest);
                            case ACCEPTED, IN_PROGRESS -> player.sendMessage("§cComplete the quest first!");
                            default -> {}
                        }
                        return;
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
}
