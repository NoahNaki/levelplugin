package me.nakilex.levelplugin.npc.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import net.citizensnpcs.api.event.NPCRightClickEvent; // Fix: Use NPCRightClickEvent
import net.citizensnpcs.api.npc.NPC;

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
    public void onNPCRightClick(NPCRightClickEvent event) { // Fix: Use NPCRightClickEvent
        // Get the player who clicked and the NPC clicked
        Player player = event.getClicker();
        NPC npc = event.getNPC();

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
