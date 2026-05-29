package me.nakilex.levelplugin.spells;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.debug.ArcSlashDebugManager;
import me.nakilex.levelplugin.particles.ParticlePlane;
import me.nakilex.levelplugin.particles.ParticleRenderContext;
import me.nakilex.levelplugin.particles.ParticleRotationAxis;
import me.nakilex.levelplugin.particles.patterns.EllipseArcPattern;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ArmorStand;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/** Shared arc-slash visual + damage helpers for rogue-style melee spells. */
public final class ArcSlashCombatUtil {
    private static final ArcSlashDebugManager.ArcSlashConfig ARC_PRESET =
            ArcSlashDebugManager.ArcSlashConfig.defaultConfig();

    private ArcSlashCombatUtil() {
    }

    public static void strike(Player caster,
                              Location center,
                              Location orientation,
                              double damage,
                              double damageRadius) {
        strike(caster, center, orientation, damage, damageRadius, 1.0, 1.0);
    }

    public static void strike(Player caster,
                              Location center,
                              Location orientation,
                              double damage,
                              double damageRadius,
                              double travelScale,
                              double radiusScale) {
        if (caster == null || center == null || orientation == null || center.getWorld() == null) {
            return;
        }
        ArcSlashDebugManager.ArcSlashConfig config = ARC_PRESET.copy();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double safeRadiusScale = Math.max(0.35, radiusScale);
        double safeTravelScale = Math.max(0.35, travelScale);
        double radiusX = random.nextDouble(config.radiusXMin(), config.radiusXMax()) * safeRadiusScale;
        double radiusZ = random.nextDouble(config.radiusZMin(), config.radiusZMax()) * safeRadiusScale;
        double baseTilt = random.nextDouble(config.baseTiltMin(), config.baseTiltMax());
        double travelDistance = config.travelDistance() * 0.125 * safeTravelScale;
        int renderTicks = Math.max(1, (int) Math.round(config.ticks() * 0.7));
        Vector direction = orientation.getDirection().clone().setY(0.0);
        if (direction.lengthSquared() <= 0.0001) {
            direction = caster.getLocation().getDirection().clone().setY(0.0);
        }
        direction.normalize();
        final Vector renderDirection = direction;
        Vector right = new Vector(0, 1, 0).crossProduct(direction).normalize();
        Vector up = new Vector(0, 1, 0);
        double sideShift = radiusX * config.sideShiftFactor() + config.rightOffset();
        Location baseCenter = center.clone()
                .add(renderDirection.clone().multiply(config.startDistance() + config.forwardOffset()))
                .add(right.clone().multiply(sideShift))
                .add(up.clone().multiply(config.upOffset()));

        EllipseArcPattern arcPreset = new EllipseArcPattern(config.particle(), null, radiusX, radiusZ, config.width(),
                config.startAngleDegrees(), config.endAngleDegrees(), 0.0, ParticlePlane.LOOK_VERTICAL,
                baseTilt, ParticleRotationAxis.LOOK_RIGHT,
                config.rotateXDegrees(), config.rotateYDegrees(), config.rotateZDegrees());

        Location finalImpact = baseCenter.clone().add(renderDirection.clone().multiply(travelDistance));
        Set<java.util.UUID> coneHits = new HashSet<>();
        applyConeDamage(caster, baseCenter, renderDirection, Math.max(2.5, travelDistance + 1.0),
                70.0, Math.max(damageRadius, 2.0), damage, coneHits);

        Main plugin = Main.getInstance();
        if (plugin == null) {
            ParticleRenderContext context = new ParticleRenderContext(caster, finalImpact, orientation,
                    Math.max(1, config.points()), 0, 1);
            arcPreset.render(context);
            SpellEffectUtil.applyAreaDamage(caster, finalImpact, damageRadius, damage);
            return;
        }

        new BukkitRunnable() {
            private int tick;
            private final Set<java.util.UUID> hitTargets = new HashSet<>();
            private Location previousCenter = baseCenter.clone();
            private final ArmorStand collisionProbe = spawnCollisionProbe(baseCenter);
            private final LivingEntity guaranteedTarget = SpellTargetingUtil.resolveTargetLivingEntity(caster, 14.0, 0.6,
                    living -> !living.equals(caster));

            @Override
            public void run() {
                if (!caster.isOnline()) {
                    removeProbe(collisionProbe);
                    cancel();
                    return;
                }
                int frameStep = Math.max(1, config.frameStep());
                if (tick % frameStep != 0) {
                    tick++;
                    if (tick >= renderTicks) {
                        removeProbe(collisionProbe);
                        cancel();
                    }
                    return;
                }
                int frameCount = (int) Math.ceil((double) renderTicks / frameStep);
                int frameIndex = Math.min(frameCount - 1, tick / frameStep);
                double progress = frameCount <= 1 ? 1.0 : (double) frameIndex / (frameCount - 1);
                Location frameCenter = baseCenter.clone().add(renderDirection.clone().multiply(travelDistance * progress));
                ParticleRenderContext context = new ParticleRenderContext(caster, frameCenter, orientation,
                        Math.max(1, config.points()), tick, renderTicks);
                arcPreset.render(context);
                applyCollisionDamage(caster, frameCenter, damageRadius, damage, hitTargets);
                applyProbeCollisionDamage(caster, collisionProbe, frameCenter, damageRadius, damage, hitTargets);
                applySegmentCollisionDamage(caster, previousCenter, frameCenter, damageRadius, damage, hitTargets);
                previousCenter = frameCenter;
                tick++;
                if (tick >= renderTicks) {
                    applyCollisionDamage(caster, finalImpact, damageRadius, damage, hitTargets);
                    applyProbeCollisionDamage(caster, collisionProbe, finalImpact, damageRadius, damage, hitTargets);
                    applySegmentCollisionDamage(caster, previousCenter, finalImpact, damageRadius, damage, hitTargets);
                    applyGuaranteedHitFallback(caster, guaranteedTarget, damage, hitTargets);
                    removeProbe(collisionProbe);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public static void launchSwordPathSlash(Player caster,
                                             List<Location> swordPath,
                                             Vector forward,
                                             double damage,
                                             double damageRadius,
                                             double travelDistance,
                                             int travelTicks) {
        if (caster == null || swordPath == null || swordPath.isEmpty() || damage <= 0.0) {
            return;
        }
        Main plugin = Main.getInstance();
        if (plugin == null) {
            return;
        }
        List<Vector> localShape = toLocalSwordPathShape(swordPath);
        if (localShape.isEmpty()) {
            return;
        }
        Vector travelDirection = resolveHorizontalDirection(caster, forward);
        if (travelDirection == null) {
            return;
        }
        Vector right = new Vector(0, 1, 0).crossProduct(travelDirection).normalize();
        Vector up = new Vector(0, 1, 0);
        Location origin = averageLocation(swordPath);
        if (origin == null) {
            return;
        }
        int safeTicks = Math.max(1, travelTicks);
        double safeTravel = Math.max(0.1, travelDistance);
        double safeRadius = Math.max(0.35, damageRadius);

        new BukkitRunnable() {
            private final Set<java.util.UUID> hitTargets = new HashSet<>();
            private int tick;

            @Override
            public void run() {
                if (!caster.isOnline()) {
                    cancel();
                    return;
                }
                double progress = safeTicks <= 1 ? 1.0 : tick / (double) (safeTicks - 1);
                Location center = origin.clone().add(travelDirection.clone().multiply(safeTravel * progress));
                for (Vector local : localShape) {
                    Location point = center.clone()
                            .add(right.clone().multiply(local.getX()))
                            .add(up.clone().multiply(local.getY()))
                            .add(travelDirection.clone().multiply(local.getZ()));
                    if (point.getWorld() == null) {
                        continue;
                    }
                    spawnSwordPathSlashParticles(point);
                    applyCollisionDamage(caster, point, safeRadius, damage, hitTargets, 0.35);
                }
                tick++;
                if (tick >= safeTicks) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public static void launchSwordPathSlashPoint(Player caster,
                                                 Location swordPoint,
                                                 Vector forward,
                                                 double damage,
                                                 double damageRadius,
                                                 double travelDistance,
                                                 int travelTicks,
                                                 Set<java.util.UUID> hitTargets) {
        if (caster == null || swordPoint == null || swordPoint.getWorld() == null || damage <= 0.0) {
            return;
        }
        Main plugin = Main.getInstance();
        if (plugin == null) {
            return;
        }
        Vector travelDirection = resolveHorizontalDirection(caster, forward);
        if (travelDirection == null) {
            return;
        }
        Location origin = swordPoint.clone();
        int safeTicks = Math.max(1, travelTicks);
        double safeTravel = Math.max(0.1, travelDistance);
        double safeRadius = Math.max(0.35, damageRadius);
        Set<java.util.UUID> activeHitTargets = hitTargets == null ? new HashSet<>() : hitTargets;

        new BukkitRunnable() {
            private int tick;
            private Location previousPoint = origin.clone();

            @Override
            public void run() {
                if (!caster.isOnline()) {
                    cancel();
                    return;
                }
                double progress = safeTicks <= 1 ? 1.0 : tick / (double) (safeTicks - 1);
                Location point = origin.clone().add(travelDirection.clone().multiply(safeTravel * progress));
                spawnSwordPathSlashParticles(point);
                applyCollisionDamage(caster, point, safeRadius, damage, activeHitTargets, 0.35);
                applySegmentCollisionDamage(caster, previousPoint, point, safeRadius, damage, activeHitTargets);
                previousPoint = point;
                tick++;
                if (tick >= safeTicks) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private static List<Vector> toLocalSwordPathShape(List<Location> swordPath) {
        Location center = averageLocation(swordPath);
        if (center == null) {
            return List.of();
        }
        List<Vector> local = new ArrayList<>();
        for (Location point : swordPath) {
            if (point == null || point.getWorld() == null || !point.getWorld().equals(center.getWorld())) {
                continue;
            }
            local.add(point.toVector().subtract(center.toVector()));
        }
        return local;
    }

    private static Location averageLocation(List<Location> points) {
        if (points == null || points.isEmpty()) {
            return null;
        }
        Location first = null;
        double x = 0.0;
        double y = 0.0;
        double z = 0.0;
        int count = 0;
        for (Location point : points) {
            if (point == null || point.getWorld() == null) {
                continue;
            }
            if (first == null) {
                first = point;
            }
            if (!point.getWorld().equals(first.getWorld())) {
                continue;
            }
            x += point.getX();
            y += point.getY();
            z += point.getZ();
            count++;
        }
        return count == 0 ? null : new Location(first.getWorld(), x / count, y / count, z / count);
    }

    private static Vector resolveHorizontalDirection(Player caster, Vector forward) {
        if (caster == null) {
            return null;
        }
        Vector direction = forward == null ? caster.getLocation().getDirection() : forward.clone();
        direction.setY(0.0);
        if (direction.lengthSquared() <= 0.0001) {
            direction = caster.getLocation().getDirection().setY(0.0);
        }
        if (direction.lengthSquared() <= 0.0001) {
            return null;
        }
        return direction.normalize();
    }

    private static void spawnSwordPathSlashParticles(Location point) {
        if (point == null || point.getWorld() == null) {
            return;
        }
        point.getWorld().spawnParticle(Particle.GLOW, point, 1, 0.02, 0.02, 0.02, 0.0);
        point.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, point, 1, 0.015, 0.015, 0.015, 0.0);
    }

    public static void strikeForward(Player caster,
                                     double forwardDistance,
                                     double upOffset,
                                     double damage,
                                     double damageRadius) {
        strikeForward(caster, forwardDistance, upOffset, damage, damageRadius, 1.0, 1.0);
    }

    public static void strikeForward(Player caster,
                                     double forwardDistance,
                                     double upOffset,
                                     double damage,
                                     double damageRadius,
                                     double travelScale,
                                     double radiusScale) {
        if (caster == null) {
            return;
        }
        Vector forward = caster.getLocation().getDirection().setY(0.0);
        if (forward.lengthSquared() <= 0.0001) {
            return;
        }
        forward.normalize();
        Location center = caster.getLocation().clone()
                .add(forward.multiply(forwardDistance))
                .add(0.0, upOffset, 0.0);
        Location orientation = caster.getLocation().clone();
        orientation.setDirection(center.toVector().subtract(caster.getLocation().toVector()));
        strike(caster, center, orientation, damage, damageRadius, travelScale, radiusScale);
    }

    public static int applyConeDamage(Player caster,
                                      Location origin,
                                      Vector forward,
                                      double range,
                                      double halfAngleDegrees,
                                      double radius,
                                      double damage) {
        return applyConeDamage(caster, origin, forward, range, halfAngleDegrees, radius, damage, new HashSet<>());
    }

    public static int applyConeDamage(Player caster,
                                      Location origin,
                                      Vector forward,
                                      double range,
                                      double halfAngleDegrees,
                                      double radius,
                                      double damage,
                                      Set<java.util.UUID> hitTargets) {
        if (caster == null || origin == null || origin.getWorld() == null || forward == null || damage <= 0.0) {
            return 0;
        }
        Vector dir = forward.clone().setY(0.0);
        if (dir.lengthSquared() <= 0.0001) {
            dir = caster.getLocation().getDirection().setY(0.0);
        }
        dir.normalize();
        double safeRange = Math.max(0.5, range);
        double safeRadius = Math.max(0.1, radius);
        double cosThreshold = Math.cos(Math.toRadians(Math.max(1.0, halfAngleDegrees)));
        int hits = 0;
        for (LivingEntity living : SpellEffectUtil.getLivingTargets(origin, safeRange + safeRadius, t -> !t.equals(caster))) {
            if (!hitTargets.add(living.getUniqueId())) {
                continue;
            }
            Vector toTarget = living.getLocation().toVector().subtract(origin.toVector());
            double forwardDistance = toTarget.dot(dir);
            if (forwardDistance < 0.0 || forwardDistance > safeRange) {
                continue;
            }
            if (toTarget.setY(0.0).lengthSquared() > safeRange * safeRange) {
                continue;
            }
            Vector flat = toTarget.clone().setY(0.0);
            if (flat.lengthSquared() <= 0.0001) {
                continue;
            }
            if (flat.normalize().dot(dir) < cosThreshold) {
                continue;
            }
            SpellEffectUtil.applyDirectSpellDamage(Main.getInstance(), caster, living, damage, true);
            hits++;
        }
        return hits;
    }

    private static void applyCollisionDamage(Player caster,
                                             Location center,
                                             double radius,
                                             double damage,
                                             Set<java.util.UUID> hitTargets) {
        applyCollisionDamage(caster, center, radius, damage, hitTargets, 5.0);
    }

    private static void applyCollisionDamage(Player caster,
                                             Location center,
                                             double radius,
                                             double damage,
                                             Set<java.util.UUID> hitTargets,
                                             double minimumRadius) {
        if (caster == null || center == null || radius <= 0.0 || damage <= 0.0) {
            return;
        }
        double effectiveRadius = Math.max(radius, minimumRadius);
        for (LivingEntity target : SpellEffectUtil.getLivingTargets(center, effectiveRadius, living -> !living.equals(caster))) {
            if (!hitTargets.add(target.getUniqueId())) {
                continue;
            }
            SpellEffectUtil.applyDirectSpellDamage(Main.getInstance(), caster, target, damage, true);
        }
    }

    private static void applySegmentCollisionDamage(Player caster,
                                                    Location from,
                                                    Location to,
                                                    double radius,
                                                    double damage,
                                                    Set<java.util.UUID> hitTargets) {
        if (caster == null || from == null || to == null || from.getWorld() == null || to.getWorld() == null) {
            return;
        }
        Vector segment = to.toVector().subtract(from.toVector());
        if (segment.lengthSquared() <= 0.0001) {
            return;
        }
        LivingEntity hit = SpellTargetingUtil.rayTraceLivingEntity(from, segment, Math.max(0.35, radius * 0.6),
                living -> !living.equals(caster));
        if (hit == null || !hitTargets.add(hit.getUniqueId())) {
            return;
        }
        SpellEffectUtil.applyDirectSpellDamage(Main.getInstance(), caster, hit, damage, true);
    }

    private static ArmorStand spawnCollisionProbe(Location at) {
        if (at == null || at.getWorld() == null) {
            return null;
        }
        return at.getWorld().spawn(at, ArmorStand.class, stand -> {
            stand.setInvisible(true);
            stand.setGravity(false);
            stand.setInvulnerable(true);
            stand.setMarker(false);
            stand.setAI(false);
            stand.setSilent(true);
            stand.setCollidable(false);
            stand.setSmall(true);
            stand.setPersistent(false);
        });
    }

    private static void removeProbe(ArmorStand probe) {
        if (probe != null && probe.isValid()) {
            probe.remove();
        }
    }

    private static void applyProbeCollisionDamage(Player caster,
                                                  ArmorStand probe,
                                                  Location at,
                                                  double radius,
                                                  double damage,
                                                  Set<java.util.UUID> hitTargets) {
        if (caster == null || probe == null || !probe.isValid() || at == null || at.getWorld() == null) {
            return;
        }
        probe.teleport(at);
        double effectiveRadius = Math.max(radius, 5.0);
        BoundingBox probeBox = probe.getBoundingBox().expand(Math.max(1.25, effectiveRadius));
        for (var entity : at.getWorld().getNearbyEntities(at, effectiveRadius, effectiveRadius, effectiveRadius)) {
            if (!(entity instanceof LivingEntity living) || living.equals(caster) || living.isDead()) {
                continue;
            }
            if (!probeBox.overlaps(living.getBoundingBox())) {
                continue;
            }
            if (!hitTargets.add(living.getUniqueId())) {
                continue;
            }
            SpellEffectUtil.applyDirectSpellDamage(Main.getInstance(), caster, living, damage, true);
        }
    }

    private static void applyGuaranteedHitFallback(Player caster,
                                                   LivingEntity guaranteedTarget,
                                                   double damage,
                                                   Set<java.util.UUID> hitTargets) {
        if (caster == null || guaranteedTarget == null || guaranteedTarget.isDead() || !guaranteedTarget.isValid()) {
            return;
        }
        if (!hitTargets.isEmpty() || !hitTargets.add(guaranteedTarget.getUniqueId())) {
            return;
        }
        SpellEffectUtil.applyDirectSpellDamage(Main.getInstance(), caster, guaranteedTarget, damage, true);
    }
}
