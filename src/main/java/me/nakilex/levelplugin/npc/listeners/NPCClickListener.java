package me.nakilex.levelplugin.npc.listeners;

import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.quests.gui.QuestState;
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

    @EventHandler
    public void onNPCClick(PlayerInteractEntityEvent event) {
        // Ignore offhand interactions
        if (event.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND) {
            return; // Ignore offhand clicks
        }

        if (CitizensAPI.getNPCRegistry().isNPC(event.getRightClicked())) {
            Player player = event.getPlayer();
            NPC npc = CitizensAPI.getNPCRegistry().getNPC(event.getRightClicked());

            if (dialogManager.hasSession(player)) {
                dialogManager.advanceDialog(player, questManager);
                return;
            }

            Quest quest = questManager.getQuestByNpcId(npc.getId());
            if (quest != null) {
                questManager.handleTalk(player, "npc" + npc.getId());
                if (questManager.getQuestState(player, quest) == QuestState.AVAILABLE) {
                    dialogManager.startDialog(player, quest);
                }
            }
        }
    }
}
