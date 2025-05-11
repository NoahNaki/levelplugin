package me.nakilex.levelplugin.utils;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayVoidListener implements Listener {

    private static final double VOID_THRESHOLD = 0.0; // Y-level under which we consider “void”

    @EventHandler
    public void onPlayerFallIntoVoid(PlayerMoveEvent event) {
        // If they haven’t actually moved to a new block, ignore
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
            && event.getFrom().getBlockY() == event.getTo().getBlockY()
            && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        if (player.getLocation().getY() <= VOID_THRESHOLD) {
            // kill the player
            player.setHealth(0.0);
            player.sendMessage(ChatColor.RED + "idk how you did it but you fell into the void!");
        }
    }
}
