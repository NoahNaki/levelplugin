package me.nakilex.levelplugin.trade.listeners;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.trade.data.ConfigValues;
import me.nakilex.levelplugin.utils.DealMaker;
import me.nakilex.levelplugin.trade.utils.MessageStrings;
import me.nakilex.levelplugin.trade.utils.Translations;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import me.nakilex.levelplugin.npc.system.NpcApi;
import me.nakilex.levelplugin.npc.system.NPC;

public class PlayerRightClicksPlayerListener implements Listener {
    @EventHandler
    public void onPlayerInteracts(PlayerInteractEntityEvent e) {
        MessageStrings messageStrings = Main.getPlugin().getMessageStrings();
        ConfigValues configValues = Main.getPlugin().getConfigValues();
        Player p = e.getPlayer();

        // Check if the entity clicked is a Player
        if (e.getRightClicked() instanceof Player) {
            // Check if the clicked entity is an NPC
            if (NpcApi.getRegistry().isNPC(e.getRightClicked())) {
                // Right-clicked entity is an NPC, so do nothing
                return;
            }

            // Shift right-click trading removed
            return;
        }
    }
}
