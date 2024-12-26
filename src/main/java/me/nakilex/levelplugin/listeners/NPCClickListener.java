package me.nakilex.levelplugin.listeners;

import me.nakilex.levelplugin.managers.EconomyManager;
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

            // Check for a specific NPC by ID
            if (npc.getId() == 1) { // Replace '1' with your NPC ID

                // Give the player 10 coins when interacting with this NPC
                economyManager.addCoins(player, 10);

                // Notify the player
                player.sendMessage("You received 10 coins for interacting with NPC ID " + npc.getId() + "!");
            }
        }
    }
}
