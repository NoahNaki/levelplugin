package me.nakilex.levelplugin.npc.core;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ViewerTracker extends BukkitRunnable {
    private static final double TRACK_RADIUS = 48.0;
    private static final double TRACK_RADIUS_SQ = TRACK_RADIUS * TRACK_RADIUS;

    private final Plugin plugin;
    private final NpcManager npcManager;
    private final NpcRegistry registry;
    private BukkitTask task;

    public ViewerTracker(Plugin plugin, NpcManager npcManager, NpcRegistry registry) {
        this.plugin = plugin;
        this.npcManager = npcManager;
        this.registry = registry;
    }

    public void start() {
        if (task == null) {
            task = runTaskTimer(plugin, 10L, 10L);
        }
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    @Override
    public void run() {
        for (PlayerNpc npc : registry.getAll()) {
            Location location = npc.getLocation();
            if (location == null || location.getWorld() == null) {
                continue;
            }
            Set<UUID> inRange = new HashSet<>();
            for (Player player : location.getWorld().getPlayers()) {
                if (player.getLocation().distanceSquared(location) <= TRACK_RADIUS_SQ) {
                    inRange.add(player.getUniqueId());
                    if (!npc.hasViewer(player.getUniqueId())) {
                        npcManager.spawnFor(player, npc);
                    }
                }
            }
            Set<UUID> currentViewers = new HashSet<>(npc.getViewers());
            for (UUID viewerId : currentViewers) {
                if (inRange.contains(viewerId)) {
                    continue;
                }
                Player viewer = Bukkit.getPlayer(viewerId);
                if (viewer != null) {
                    npcManager.despawnFor(viewer, npc);
                } else {
                    npc.removeViewer(viewerId);
                }
            }
        }
    }
}
