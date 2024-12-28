package me.nakilex.levelplugin.party;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.entity.Player;
import java.util.UUID;

public class PartyChatListener implements Listener {

    private final PartyManager partyManager;

    public PartyChatListener(PartyManager partyManager) {
        this.partyManager = partyManager;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Check if player is in a party
        Party party = partyManager.getParty(playerId);
        if (party != null && party.isChatEnabled()) {
            event.setCancelled(true); // Cancel the normal chat event

            // Send the message only to party members
            String message = ChatColor.GREEN + "[Party] " + player.getName() + ": " + event.getMessage();
            for (UUID memberId : party.getMembers()) {
                Player member = Bukkit.getPlayer(memberId);
                if (member != null && member.isOnline()) {
                    member.sendMessage(message);
                }
            }
        }
    }
}
