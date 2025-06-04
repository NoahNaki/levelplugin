package me.nakilex.levelplugin.npc.listeners;

import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.quests.gui.QuestState;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class NPCClickListener implements Listener {

    private EconomyManager economyManager;

    // Constructor to get the EconomyManager instance
    public NPCClickListener(EconomyManager economyManager) {
        this.economyManager = economyManager;
    }

    @EventHandler
    public void onNPCClick(PlayerInteractEntityEvent event) {
        // Ignore offhand interactions
        if (event.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND) {
            return; // Ignore offhand clicks
        }

        // Check if the entity clicked is an NPC
        if (CitizensAPI.getNPCRegistry().isNPC(event.getRightClicked())) {

            // Get the player who clicked
            Player player = event.getPlayer();

            // Retrieve the NPC that was clicked
            NPC npc = CitizensAPI.getNPCRegistry().getNPC(event.getRightClicked());

            QuestManager qm = me.nakilex.levelplugin.Main.getInstance().getQuestManager();
            Quest quest = qm.getQuestByNpcId(npc.getId());
            if (quest != null) {
                qm.handleTalk(player, "npc" + npc.getId());
                if (qm.getQuestState(player, quest) == QuestState.AVAILABLE) {
                    qm.startQuest(player, quest.getId());
                }
            }
        }
    }
}
