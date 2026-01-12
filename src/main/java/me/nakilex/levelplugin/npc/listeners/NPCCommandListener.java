package me.nakilex.levelplugin.npc.listeners;

import me.nakilex.levelplugin.npc.system.NPC;
import me.nakilex.levelplugin.npc.system.NpcApi;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.HashMap;
import java.util.Map;

public class NPCCommandListener implements Listener {

    // Map NPC names to commands
    private final Map<String, String> npcCommands = new HashMap<>();

    // Constructor - initialize NPC-to-command mapping
    public NPCCommandListener() {
//        npcCommands.put("Blacksmith", "blacksmith"); // NPC "Blacksmith" runs /blacksmith
//        npcCommands.put("Merchant", "");     // NPC "Merchant" runs /balance
//        npcCommands.put("Stable Keeper", "horse reroll");  // NPC "Stable" runs /horse reroll
    }

    @EventHandler
    public void onNPCRightClick(PlayerInteractEntityEvent event) {
        if (!NpcApi.getRegistry().isNPC(event.getRightClicked())) {
            return;
        }
        Player player = event.getPlayer();
        NPC npc = NpcApi.getRegistry().getNPC(event.getRightClicked());
        if (npc == null) {
            return;
        }

        // Check if the NPC's name matches any key in the map
        String npcName = npc.getName();
        if (npcCommands.containsKey(npcName)) {
            // Get the associated command
            String command = npcCommands.get(npcName);

            // Execute the command as if the player typed it
            Bukkit.dispatchCommand(player, command);

            // Notify the player (optional)
            //player.sendMessage("You interacted with " + npcName + " and executed: /" + command);
        }
    }
}
