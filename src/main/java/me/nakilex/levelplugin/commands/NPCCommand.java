package me.nakilex.levelplugin.commands;

import me.nakilex.levelplugin.managers.NPCManager;
import me.nakilex.levelplugin.npc.CustomNPC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.UUID;

public class NPCCommand implements CommandExecutor {
    private final NPCManager npcManager;

    public NPCCommand(NPCManager npcManager) {
        this.npcManager = npcManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length < 2) {
            player.sendMessage("Usage: /npc <spawn|despawn> <npc_id>");
            return true;
        }

        String action = args[0].toLowerCase();
        String npcID = args[1];

        switch (action) {
            case "spawn":
                spawnNPC(player, npcID);
                break;

            case "despawn":
                despawnNearestNPC(player, npcID);
                break;

            default:
                player.sendMessage("Invalid action. Use 'spawn' or 'despawn'.");
        }

        return true;
    }

    private void spawnNPC(Player player, String npcID) {
        CustomNPC npc = npcManager.getNPCById(npcID);

        if (npc == null) {
            player.sendMessage("No NPC found with ID: " + npcID);
            return;
        }

        // Spawn the NPC at the player's current location
        Location location = player.getLocation();
        npc.setPosition(location); // Update the NPC's position dynamically
        npcManager.spawnNPC(npc);

        player.sendMessage("NPC " + npcID + " has been spawned at your location.");
    }

    private void despawnNearestNPC(Player player, String npcID) {
        CustomNPC npc = npcManager.getNPCById(npcID);

        if (npc == null) {
            player.sendMessage("No NPC found with ID: " + npcID);
            return;
        }

        // Get the nearest NPC with the specified ID
        UUID nearestNPC = npcManager.getNearestNPC(player.getLocation(), npcID);

        if (nearestNPC == null) {
            player.sendMessage("No active NPC with ID: " + npcID + " was found nearby.");
            return;
        }

        npcManager.despawnNPC(nearestNPC);
        player.sendMessage("The nearest NPC with ID " + npcID + " has been despawned.");
    }
}
