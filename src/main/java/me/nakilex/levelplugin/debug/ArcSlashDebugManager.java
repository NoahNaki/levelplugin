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
import me.nakilex.levelplugin.particles.patterns.EllipseArcPattern;
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
        double radiusX = random.nextDouble(config.radiusXMin(), config.radiusXMax());
        double radiusZ = random.nextDouble(config.radiusZMin(), config.radiusZMax());
        double rotationSpeed = 0.0;

        List<ParticlePattern> patterns = List.of(
                new EllipseArcPattern(config.particle(), null, radiusX, radiusZ, config.startAngleDegrees(),
                        config.endAngleDegrees(), rotationSpeed, ParticlePlane.LOOK_VERTICAL,
                        baseTilt - config.layerTiltStep(), ParticleRotationAxis.LOOK_RIGHT),
                new EllipseArcPattern(config.particle(), null, radiusX, radiusZ, config.startAngleDegrees(),
                        config.endAngleDegrees(), rotationSpeed, ParticlePlane.LOOK_VERTICAL, baseTilt,
                        ParticleRotationAxis.LOOK_RIGHT),
                new EllipseArcPattern(config.particle(), null, radiusX, radiusZ, config.startAngleDegrees(),
                        config.endAngleDegrees(), rotationSpeed, ParticlePlane.LOOK_VERTICAL,
                        baseTilt + config.layerTiltStep(), ParticleRotationAxis.LOOK_RIGHT)
        );

        Location orientation = player.getLocation().clone();
        orientation.setPitch(0f);
        Vector direction = orientation.getDirection().normalize();
        Vector right = new Vector(0, 1, 0).crossProduct(direction).normalize();
        double sideShift = radiusX * config.sideShiftFactor();
        Location baseCenter = player.getEyeLocation().clone()
                .add(direction.clone().multiply(config.startDistance()))
                .add(right.multiply(sideShift));

        new BukkitRunnable() {
            private int tick = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                int frameStep = Math.max(1, config.frameStep());
                if (tick % frameStep != 0) {
                    tick++;
                    if (tick >= config.ticks()) {
                        cancel();
                    }
                    return;
                }
                int frameCount = (int) Math.ceil((double) config.ticks() / frameStep);
                int frameIndex = Math.min(frameCount - 1, tick / frameStep);
                double progress = frameCount <= 1 ? 1.0 : (double) frameIndex / (frameCount - 1);
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
        ARC1("arc1", new ArcSlashConfig(Particle.END_ROD, 20, 5, 2, 2.0, 1.4, 1.6, 2.0, 0.9, 1.2,
                -70.0, 70.0, -18.0, 18.0, 16.0, 0.35)),
        ARC2("arc2", new ArcSlashConfig(Particle.CRIT, 28, 7, 2, 2.4, 2.6, 2.2, 2.8, 1.1, 1.6,
                -75.0, 75.0, -22.0, 22.0, 26.0, 0.4)),
        ARC3("arc3", new ArcSlashConfig(Particle.ENCHANT, 24, 6, 2, 2.1, 2.0, 1.9, 2.6, 1.0, 1.4,
                -80.0, 80.0, -28.0, 28.0, 24.0, 0.38)),
        ARC4("arc4", new ArcSlashConfig(Particle.CLOUD, 30, 8, 2, 2.3, 2.8, 2.4, 3.1, 1.2, 1.7,
                -70.0, 70.0, -16.0, 16.0, 20.0, 0.45)),
        ARC5("arc5", new ArcSlashConfig(Particle.END_ROD, 18, 4, 2, 2.6, 1.2, 1.5, 2.0, 0.8, 1.1,
                -65.0, 65.0, -12.0, 12.0, 14.0, 0.3)),
        ARC6("arc6", new ArcSlashConfig(Particle.CRIT, 32, 6, 2, 2.0, 2.4, 2.6, 3.3, 1.3, 1.8,
                -85.0, 85.0, -32.0, 32.0, 28.0, 0.5));

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

    public record ArcSlashConfig(Particle particle, int points, int ticks, int frameStep, double startDistance,
                                 double travelDistance, double radiusXMin, double radiusXMax,
                                 double radiusZMin, double radiusZMax, double startAngleDegrees,
                                 double endAngleDegrees, double baseTiltMin, double baseTiltMax,
                                 double layerTiltStep, double sideShiftFactor) {}
}
