package me.nakilex.levelplugin.debug;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.particles.ParticlePlane;
import me.nakilex.levelplugin.particles.ParticleRenderContext;
import me.nakilex.levelplugin.particles.ParticleRotationAxis;
import me.nakilex.levelplugin.particles.patterns.CrescentPattern;
import me.nakilex.levelplugin.particles.patterns.ParticlePattern;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class ArcSlashDebugManager implements Listener {
    private final Main plugin;
    private final Set<UUID> enabledPlayers = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public ArcSlashDebugManager(Main plugin) {
        this.plugin = plugin;
    }

    public void toggle(Player player) {
        UUID id = player.getUniqueId();
        if (enabledPlayers.remove(id)) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "Arc slash preview disabled.");
            return;
        }
        enabledPlayers.add(id);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Arc slash preview enabled. Left click to spawn slashes.");
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwing(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) {
            return;
        }
        Player player = event.getPlayer();
        if (!enabledPlayers.contains(player.getUniqueId())) {
            return;
        }
        spawnSlashBurst(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        enabledPlayers.remove(event.getPlayer().getUniqueId());
    }

    private void spawnSlashBurst(Player player) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double baseTilt = random.nextDouble(-50.0, 50.0);
        double radius = random.nextDouble(1.3, 1.8);
        double innerRadius = radius * random.nextDouble(0.82, 0.9);
        double offset = radius * random.nextDouble(0.22, 0.32);
        double rotationSpeed = random.nextDouble(-12.0, -6.0);

        List<ParticlePattern> patterns = List.of(
                new CrescentPattern(Particle.CLOUD, null, radius, innerRadius, offset, rotationSpeed,
                        ParticlePlane.LOOK, baseTilt - 20.0, ParticleRotationAxis.Z),
                new CrescentPattern(Particle.CLOUD, null, radius, innerRadius, offset, rotationSpeed,
                        ParticlePlane.LOOK, baseTilt, ParticleRotationAxis.Z),
                new CrescentPattern(Particle.CLOUD, null, radius, innerRadius, offset, rotationSpeed,
                        ParticlePlane.LOOK, baseTilt + 20.0, ParticleRotationAxis.Z)
        );

        Location orientation = player.getLocation().clone();
        orientation.setPitch(0f);
        Vector direction = orientation.getDirection().normalize();
        Location baseCenter = player.getEyeLocation().clone().add(direction.clone().multiply(0.8));

        new BukkitRunnable() {
            private int tick = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                double progress = 6 <= 1 ? 1.0 : (double) tick / 5.0;
                Vector travel = direction.clone().multiply(2.0 * progress);
                ParticleRenderContext context = new ParticleRenderContext(
                        player,
                        baseCenter.clone().add(travel),
                        orientation,
                        12,
                        tick,
                        6
                );
                for (ParticlePattern pattern : patterns) {
                    pattern.render(context);
                }
                tick++;
                if (tick >= 6) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}
