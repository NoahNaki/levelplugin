package me.nakilex.levelplugin.debug.particles;

import hm.zelha.particlesfx.particles.parents.Particle;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ParticleDebugManager {
    private static final int PREVIEW_DURATION_TICKS = 20 * 10;
    private final JavaPlugin plugin;
    private final Map<UUID, BukkitTask> activePreviews = new HashMap<>();

    public ParticleDebugManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void startPreview(Player player, Particle particle, Location location) {
        if (player == null || particle == null || location == null) {
            return;
        }
        UUID id = player.getUniqueId();
        cancelPreview(id);
        Location origin = location.clone();
        BukkitTask task = new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || ticks >= PREVIEW_DURATION_TICKS) {
                    cancelPreview(id);
                    return;
                }
                particle.displayForPlayers(origin, player);
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
        activePreviews.put(id, task);
    }

    public void cancelPreview(UUID playerId) {
        BukkitTask task = activePreviews.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }
}
