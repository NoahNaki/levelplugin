package me.nakilex.levelplugin.fasttravel.listeners;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.fasttravel.FastTravelManager;
import me.nakilex.levelplugin.fasttravel.data.FastTravelPoint;
import me.nakilex.levelplugin.music.LocationMusicManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ExplorationListener implements Listener {
    private final FastTravelManager manager;
    private final LocationMusicManager musicManager;
    private final Map<UUID, String> current = new HashMap<>();

    public ExplorationListener(FastTravelManager manager, LocationMusicManager musicManager) {
        this.manager = manager;
        this.musicManager = musicManager;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        Location from = event.getFrom();
        if (to == null || (to.getBlockX() == from.getBlockX() && to.getBlockZ() == from.getBlockZ())) return;

        FastTravelPoint pt = manager.getPointAt(to);
        String prev = current.get(player.getUniqueId());
        if (pt != null) {
            String name = pt.getName();
            if (!name.equalsIgnoreCase(prev)) {
                Main.getInstance().getLogger().info("Exploration debug: " + player.getName() + " entered " + name + " (prev=" + prev + ")");
                current.put(player.getUniqueId(), name);
                if (!manager.isUnlocked(player, name)) {
                    manager.unlock(player, name);
                    player.sendTitle(pt.getColor() + pt.getName(), ChatColor.GRAY + pt.getDescription(), 10, 60, 10);
                }
                // Trigger music only when entering a new location
                musicManager.update(player, pt);
            }
        } else if (prev != null) {
            current.remove(player.getUniqueId());
        }
    }
}
