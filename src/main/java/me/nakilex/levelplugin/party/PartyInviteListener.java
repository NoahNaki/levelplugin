package me.nakilex.levelplugin.party;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.entity.Player;
import java.util.UUID;

public class PartyInviteListener implements Listener {

    private final PartyManager partyManager;

    public PartyInviteListener(PartyManager partyManager) {
        this.partyManager = partyManager;
    }

//    @EventHandler
//    public void onPlayerQuit(PlayerQuitEvent event) {
//        Player player = event.getPlayer();
//        UUID playerId = player.getUniqueId();
//
//        // Remove player from the party if they are in one
//        Party party = partyManager.getParty(playerId);
//        if (party != null) {
//            partyManager.removeMember(party.getLeader(), playerId);
//        }
//    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Remove player from the party if they are in one
        Party party = partyManager.getParty(playerId);
        if (party != null) {
            partyManager.removeMember(party.getLeader(), playerId);
        }
    }
}
