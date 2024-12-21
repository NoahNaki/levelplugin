package me.nakilex.levelplugin.listeners;

import me.nakilex.levelplugin.managers.NPCManager;
import me.nakilex.levelplugin.npc.CustomNPC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.UUID;

public class NPCClickListener implements Listener {
    private final NPCManager npcManager;

    public NPCClickListener(NPCManager npcManager) {
        this.npcManager = npcManager;
    }

    @EventHandler
    public void onNPCClick(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity clickedEntity = event.getRightClicked();

        // Check if the clicked entity is an NPC
        CustomNPC clickedNPC = npcManager.getNPCByUUID(clickedEntity.getUniqueId());
        if (clickedNPC == null) {
            return; // Not an NPC, do nothing
        }

        event.setCancelled(true); // Prevent default interaction behavior

        // Execute NPC behavior
        handleNPCInteraction(player, clickedNPC);
    }

    private void handleNPCInteraction(Player player, CustomNPC npc) {
        // Display interact message, if any
        if (npc.hasInteractMessage()) {
            player.sendMessage(ChatColor.GOLD + npc.getInteractMessage());
        }

        // Execute command, if any
        if (npc.hasCommand()) {
            String command = npc.getOnRightClickCommand();
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
        }

        // Optional: Add cooldown handling logic here
    }
}
