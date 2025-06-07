package me.nakilex.levelplugin.fasttravel.listeners;

import me.nakilex.levelplugin.fasttravel.FastTravelManager;
import me.nakilex.levelplugin.fasttravel.data.FastTravelPoint;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.Location;

public class ExplorationListener implements Listener {
    private final FastTravelManager manager;

    public ExplorationListener(FastTravelManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        Location from = event.getFrom();
        if (to == null || (to.getBlockX() == from.getBlockX() && to.getBlockZ() == from.getBlockZ())) return;

        for (FastTravelPoint pt : manager.getPoints()) {
            if (manager.isUnlocked(player, pt.getName())) continue;
            if (!pt.getLocation().getWorld().equals(to.getWorld())) continue;
            if (to.distance(pt.getLocation()) <= pt.getRadius()) {
                manager.unlock(player, pt.getName());
                player.sendTitle(pt.getColor() + pt.getName(), ChatColor.GRAY + pt.getDescription(), 10, 60, 10);
            }
        }
    }
}
