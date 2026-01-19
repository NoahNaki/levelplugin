package me.nakilex.levelplugin.debug;

import java.util.List;
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
    private static final String ARC_PRESET_ID = "arc";

    private final Main plugin;
    private final Set<UUID> enabledPlayers = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private ArcSlashConfig config = ArcSlashConfig.defaultConfig();

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
        UUID id = event.getPlayer().getUniqueId();
        enabledPlayers.remove(id);
    }

    private void spawnSlashBurst(Player player) {
        if (!enabledPlayers.contains(player.getUniqueId())) {
            return;
        }
        ArcSlashConfig config = config();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double baseTilt = random.nextDouble(config.baseTiltMin(), config.baseTiltMax());
        double radiusX = random.nextDouble(config.radiusXMin(), config.radiusXMax());
        double radiusZ = random.nextDouble(config.radiusZMin(), config.radiusZMax());
        double rotationSpeed = 0.0;

        List<ParticlePattern> patterns = List.of(
                new EllipseArcPattern(config.particle(), null, radiusX, radiusZ, config.width(),
                        config.startAngleDegrees(), config.endAngleDegrees(), rotationSpeed,
                        ParticlePlane.LOOK_VERTICAL, baseTilt - config.layerTiltStep(),
                        ParticleRotationAxis.LOOK_RIGHT, config.rotateXDegrees(),
                        config.rotateYDegrees(), config.rotateZDegrees()),
                new EllipseArcPattern(config.particle(), null, radiusX, radiusZ, config.width(),
                        config.startAngleDegrees(), config.endAngleDegrees(), rotationSpeed,
                        ParticlePlane.LOOK_VERTICAL, baseTilt, ParticleRotationAxis.LOOK_RIGHT,
                        config.rotateXDegrees(), config.rotateYDegrees(), config.rotateZDegrees()),
                new EllipseArcPattern(config.particle(), null, radiusX, radiusZ, config.width(),
                        config.startAngleDegrees(), config.endAngleDegrees(), rotationSpeed,
                        ParticlePlane.LOOK_VERTICAL, baseTilt + config.layerTiltStep(),
                        ParticleRotationAxis.LOOK_RIGHT, config.rotateXDegrees(),
                        config.rotateYDegrees(), config.rotateZDegrees())
        );

        Location orientation = player.getLocation().clone();
        orientation.setPitch(0f);
        Vector direction = orientation.getDirection().normalize();
        Vector right = new Vector(0, 1, 0).crossProduct(direction).normalize();
        Vector up = new Vector(0, 1, 0);
        double sideShift = radiusX * config.sideShiftFactor() + config.rightOffset();
        Location baseCenter = player.getEyeLocation().clone()
                .add(direction.clone().multiply(config.startDistance() + config.forwardOffset()))
                .add(right.multiply(sideShift))
                .add(up.clone().multiply(config.upOffset()));

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

    public ArcSlashConfig config() {
        return config.copy();
    }

    public void applyConfig(ArcSlashConfig config) {
        this.config = config.copy();
    }

    public void logConfig(ArcSlashConfig config) {
        plugin.getLogger().info(() -> "[ArcSlashDebug] " + config.describe());
    }

    public static String getArcPresetId() {
        return ARC_PRESET_ID;
    }

    public static List<String> getPresetIds() {
        return List.of(ARC_PRESET_ID);
    }

    public static class ArcSlashConfig {
        private Particle particle;
        private int points;
        private int ticks;
        private int frameStep;
        private double startDistance;
        private double travelDistance;
        private double radiusXMin;
        private double radiusXMax;
        private double radiusZMin;
        private double radiusZMax;
        private double width;
        private double startAngleDegrees;
        private double endAngleDegrees;
        private double baseTiltMin;
        private double baseTiltMax;
        private double layerTiltStep;
        private double sideShiftFactor;
        private double forwardOffset;
        private double rightOffset;
        private double upOffset;
        private double rotateXDegrees;
        private double rotateYDegrees;
        private double rotateZDegrees;

        public ArcSlashConfig(Particle particle, int points, int ticks, int frameStep, double startDistance,
                              double travelDistance, double radiusXMin, double radiusXMax,
                              double radiusZMin, double radiusZMax, double width,
                              double startAngleDegrees, double endAngleDegrees, double baseTiltMin,
                              double baseTiltMax, double layerTiltStep, double sideShiftFactor,
                              double forwardOffset, double rightOffset, double upOffset,
                              double rotateXDegrees, double rotateYDegrees, double rotateZDegrees) {
            this.particle = particle;
            this.points = points;
            this.ticks = ticks;
            this.frameStep = frameStep;
            this.startDistance = startDistance;
            this.travelDistance = travelDistance;
            this.radiusXMin = radiusXMin;
            this.radiusXMax = radiusXMax;
            this.radiusZMin = radiusZMin;
            this.radiusZMax = radiusZMax;
            this.width = width;
            this.startAngleDegrees = startAngleDegrees;
            this.endAngleDegrees = endAngleDegrees;
            this.baseTiltMin = baseTiltMin;
            this.baseTiltMax = baseTiltMax;
            this.layerTiltStep = layerTiltStep;
            this.sideShiftFactor = sideShiftFactor;
            this.forwardOffset = forwardOffset;
            this.rightOffset = rightOffset;
            this.upOffset = upOffset;
            this.rotateXDegrees = rotateXDegrees;
            this.rotateYDegrees = rotateYDegrees;
            this.rotateZDegrees = rotateZDegrees;
        }

        public static ArcSlashConfig defaultConfig() {
            return new ArcSlashConfig(Particle.CRIT, 28, 7, 2, 2.4, 2.6, 2.2, 2.8, 1.1, 1.6,
                    0.3, -75.0, 75.0, -22.0, 22.0, 26.0, 0.0, 0.0, 0.0, 0.0,
                    0.0, 0.0, 0.0);
        }

        public ArcSlashConfig copy() {
            return new ArcSlashConfig(particle, points, ticks, frameStep, startDistance, travelDistance,
                    radiusXMin, radiusXMax, radiusZMin, radiusZMax, width, startAngleDegrees,
                    endAngleDegrees, baseTiltMin, baseTiltMax, layerTiltStep, sideShiftFactor,
                    forwardOffset, rightOffset, upOffset, rotateXDegrees, rotateYDegrees,
                    rotateZDegrees);
        }

        public String describe() {
            return "particle=" + particle
                    + ", points=" + points
                    + ", ticks=" + ticks
                    + ", frameStep=" + frameStep
                    + ", startDistance=" + startDistance
                    + ", travelDistance=" + travelDistance
                    + ", radiusXMin=" + radiusXMin
                    + ", radiusXMax=" + radiusXMax
                    + ", radiusZMin=" + radiusZMin
                    + ", radiusZMax=" + radiusZMax
                    + ", width=" + width
                    + ", startAngleDegrees=" + startAngleDegrees
                    + ", endAngleDegrees=" + endAngleDegrees
                    + ", baseTiltMin=" + baseTiltMin
                    + ", baseTiltMax=" + baseTiltMax
                    + ", layerTiltStep=" + layerTiltStep
                    + ", sideShiftFactor=" + sideShiftFactor
                    + ", forwardOffset=" + forwardOffset
                    + ", rightOffset=" + rightOffset
                    + ", upOffset=" + upOffset
                    + ", rotateXDegrees=" + rotateXDegrees
                    + ", rotateYDegrees=" + rotateYDegrees
                    + ", rotateZDegrees=" + rotateZDegrees;
        }

        public Particle particle() {
            return particle;
        }

        public void setParticle(Particle particle) {
            this.particle = particle;
        }

        public int points() {
            return points;
        }

        public void setPoints(int points) {
            this.points = points;
        }

        public int ticks() {
            return ticks;
        }

        public void setTicks(int ticks) {
            this.ticks = ticks;
        }

        public int frameStep() {
            return frameStep;
        }

        public void setFrameStep(int frameStep) {
            this.frameStep = frameStep;
        }

        public double startDistance() {
            return startDistance;
        }

        public void setStartDistance(double startDistance) {
            this.startDistance = startDistance;
        }

        public double travelDistance() {
            return travelDistance;
        }

        public void setTravelDistance(double travelDistance) {
            this.travelDistance = travelDistance;
        }

        public double radiusXMin() {
            return radiusXMin;
        }

        public void setRadiusXMin(double radiusXMin) {
            this.radiusXMin = radiusXMin;
        }

        public double radiusXMax() {
            return radiusXMax;
        }

        public void setRadiusXMax(double radiusXMax) {
            this.radiusXMax = radiusXMax;
        }

        public double radiusZMin() {
            return radiusZMin;
        }

        public void setRadiusZMin(double radiusZMin) {
            this.radiusZMin = radiusZMin;
        }

        public double radiusZMax() {
            return radiusZMax;
        }

        public void setRadiusZMax(double radiusZMax) {
            this.radiusZMax = radiusZMax;
        }

        public double width() {
            return width;
        }

        public void setWidth(double width) {
            this.width = width;
        }

        public double startAngleDegrees() {
            return startAngleDegrees;
        }

        public void setStartAngleDegrees(double startAngleDegrees) {
            this.startAngleDegrees = startAngleDegrees;
        }

        public double endAngleDegrees() {
            return endAngleDegrees;
        }

        public void setEndAngleDegrees(double endAngleDegrees) {
            this.endAngleDegrees = endAngleDegrees;
        }

        public double baseTiltMin() {
            return baseTiltMin;
        }

        public void setBaseTiltMin(double baseTiltMin) {
            this.baseTiltMin = baseTiltMin;
        }

        public double baseTiltMax() {
            return baseTiltMax;
        }

        public void setBaseTiltMax(double baseTiltMax) {
            this.baseTiltMax = baseTiltMax;
        }

        public double layerTiltStep() {
            return layerTiltStep;
        }

        public void setLayerTiltStep(double layerTiltStep) {
            this.layerTiltStep = layerTiltStep;
        }

        public double sideShiftFactor() {
            return sideShiftFactor;
        }

        public void setSideShiftFactor(double sideShiftFactor) {
            this.sideShiftFactor = sideShiftFactor;
        }

        public double forwardOffset() {
            return forwardOffset;
        }

        public void setForwardOffset(double forwardOffset) {
            this.forwardOffset = forwardOffset;
        }

        public double rightOffset() {
            return rightOffset;
        }

        public void setRightOffset(double rightOffset) {
            this.rightOffset = rightOffset;
        }

        public double upOffset() {
            return upOffset;
        }

        public void setUpOffset(double upOffset) {
            this.upOffset = upOffset;
        }

        public double rotateXDegrees() {
            return rotateXDegrees;
        }

        public void setRotateXDegrees(double rotateXDegrees) {
            this.rotateXDegrees = rotateXDegrees;
        }

        public double rotateYDegrees() {
            return rotateYDegrees;
        }

        public void setRotateYDegrees(double rotateYDegrees) {
            this.rotateYDegrees = rotateYDegrees;
        }

        public double rotateZDegrees() {
            return rotateZDegrees;
        }

        public void setRotateZDegrees(double rotateZDegrees) {
            this.rotateZDegrees = rotateZDegrees;
        }
    }
}
