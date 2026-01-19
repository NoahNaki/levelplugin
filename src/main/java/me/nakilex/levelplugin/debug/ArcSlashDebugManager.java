package me.nakilex.levelplugin.debug;

import java.util.List;
import java.util.Map;
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
    private final Map<UUID, ArcSlashVariant> activeVariants = new java.util.concurrent.ConcurrentHashMap<>();

    public ArcSlashDebugManager(Main plugin) {
        this.plugin = plugin;
    }

    public void toggle(Player player, ArcSlashVariant variant) {
        UUID id = player.getUniqueId();
        ArcSlashVariant current = activeVariants.get(id);
        if (current == variant && enabledPlayers.remove(id)) {
            activeVariants.remove(id);
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "Arc slash preview disabled.");
            return;
        }
        enabledPlayers.add(id);
        activeVariants.put(id, variant);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Arc slash preview enabled (" + variant.id() + "). Left click to spawn slashes.");
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
        UUID id = event.getPlayer().getUniqueId();
        enabledPlayers.remove(id);
        activeVariants.remove(id);
    }

    private void spawnSlashBurst(Player player) {
        ArcSlashVariant variant = activeVariants.get(player.getUniqueId());
        if (variant == null) {
            return;
        }
        ArcSlashConfig config = variant.config();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double baseTilt = random.nextDouble(config.baseTiltMin(), config.baseTiltMax());
        double radius = random.nextDouble(config.radiusMin(), config.radiusMax());
        double innerRadius = radius * random.nextDouble(config.innerRatioMin(), config.innerRatioMax());
        double offset = radius * random.nextDouble(config.offsetRatioMin(), config.offsetRatioMax());
        double rotationSpeed = 0.0;
        Vector localOffset = new Vector(-offset * config.centerShiftFactor(), 0, 0);

        List<ParticlePattern> patterns = List.of(
                new CrescentPattern(config.particle(), null, radius, innerRadius, offset, localOffset, rotationSpeed,
                        ParticlePlane.LOOK_VERTICAL, baseTilt - config.layerTiltStep(), ParticleRotationAxis.LOOK),
                new CrescentPattern(config.particle(), null, radius, innerRadius, offset, localOffset, rotationSpeed,
                        ParticlePlane.LOOK_VERTICAL, baseTilt, ParticleRotationAxis.LOOK),
                new CrescentPattern(config.particle(), null, radius, innerRadius, offset, localOffset, rotationSpeed,
                        ParticlePlane.LOOK_VERTICAL, baseTilt + config.layerTiltStep(), ParticleRotationAxis.LOOK)
        );

        Location orientation = player.getLocation().clone();
        orientation.setPitch(0f);
        Vector direction = orientation.getDirection().normalize();
        Location baseCenter = player.getEyeLocation().clone().add(direction.clone().multiply(config.startDistance()));

        new BukkitRunnable() {
            private int tick = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                double progress = config.ticks() <= 1 ? 1.0 : (double) tick / (config.ticks() - 1);
                Vector travel = direction.clone().multiply(config.travelDistance() * progress);
                ParticleRenderContext context = new ParticleRenderContext(
                        player,
                        baseCenter.clone().add(travel),
                        orientation,
                        config.points(),
                        tick,
                        config.ticks()
                );
                for (ParticlePattern pattern : patterns) {
                    pattern.render(context);
                }
                tick++;
                if (tick >= config.ticks()) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public static List<String> getVariantIds() {
        return ArcSlashVariant.ids();
    }

    public enum ArcSlashVariant {
        ARC1("arc1", new ArcSlashConfig(Particle.END_ROD, 14, 4, 2.0, 1.4, 1.6, 2.0,
                0.74, 0.8, 0.22, 0.28, -18.0, 18.0, 16.0, 0.45)),
        ARC2("arc2", new ArcSlashConfig(Particle.CRIT, 20, 7, 2.4, 2.6, 2.2, 2.9,
                0.68, 0.74, 0.26, 0.34, -22.0, 22.0, 26.0, 0.6)),
        ARC3("arc3", new ArcSlashConfig(Particle.ENCHANT, 18, 6, 2.1, 2.0, 1.9, 2.5,
                0.7, 0.76, 0.24, 0.32, -28.0, 28.0, 24.0, 0.55)),
        ARC4("arc4", new ArcSlashConfig(Particle.CLOUD, 22, 8, 2.3, 2.8, 2.4, 3.1,
                0.66, 0.72, 0.28, 0.36, -16.0, 16.0, 20.0, 0.65)),
        ARC5("arc5", new ArcSlashConfig(Particle.END_ROD, 12, 4, 2.6, 1.2, 1.5, 1.9,
                0.76, 0.82, 0.2, 0.26, -12.0, 12.0, 14.0, 0.4)),
        ARC6("arc6", new ArcSlashConfig(Particle.CRIT, 24, 6, 2.0, 2.4, 2.6, 3.3,
                0.64, 0.7, 0.3, 0.38, -32.0, 32.0, 28.0, 0.7));

        private final String id;
        private final ArcSlashConfig config;

        ArcSlashVariant(String id, ArcSlashConfig config) {
            this.id = id;
            this.config = config;
        }

        public String id() {
            return id;
        }

        public ArcSlashConfig config() {
            return config;
        }

        public static ArcSlashVariant fromId(String id) {
            for (ArcSlashVariant variant : values()) {
                if (variant.id.equalsIgnoreCase(id)) {
                    return variant;
                }
            }
            return null;
        }

        public static List<String> ids() {
            return java.util.Arrays.stream(values()).map(ArcSlashVariant::id).toList();
        }
    }

    public record ArcSlashConfig(Particle particle, int points, int ticks, double startDistance,
                                 double travelDistance, double radiusMin, double radiusMax,
                                 double innerRatioMin, double innerRatioMax, double offsetRatioMin,
                                 double offsetRatioMax, double baseTiltMin, double baseTiltMax,
                                 double layerTiltStep, double centerShiftFactor) {}
}
