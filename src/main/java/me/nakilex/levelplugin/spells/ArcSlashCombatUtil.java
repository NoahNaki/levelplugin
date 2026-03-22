package me.nakilex.levelplugin.spells;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.debug.ArcSlashDebugManager;
import me.nakilex.levelplugin.particles.ParticlePlane;
import me.nakilex.levelplugin.particles.ParticleRenderContext;
import me.nakilex.levelplugin.particles.ParticleRotationAxis;
import me.nakilex.levelplugin.particles.patterns.EllipseArcPattern;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ArmorStand;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.util.BoundingBox;

import java.util.HashSet;
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
        if (caster == null || center == null || orientation == null || center.getWorld() == null) {
            return;
        }
        ArcSlashDebugManager.ArcSlashConfig config = ARC_PRESET.copy();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double radiusX = random.nextDouble(config.radiusXMin(), config.radiusXMax());
        double radiusZ = random.nextDouble(config.radiusZMin(), config.radiusZMax());
        double baseTilt = random.nextDouble(config.baseTiltMin(), config.baseTiltMax());
        double travelDistance = config.travelDistance() * 0.125;
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

    public static void strikeForward(Player caster,
                                     double forwardDistance,
                                     double upOffset,
                                     double damage,
                                     double damageRadius) {
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
        strike(caster, center, orientation, damage, damageRadius);
    }

    private static void applyCollisionDamage(Player caster,
                                             Location center,
                                             double radius,
                                             double damage,
                                             Set<java.util.UUID> hitTargets) {
        if (caster == null || center == null || radius <= 0.0 || damage <= 0.0) {
            return;
        }
        double effectiveRadius = Math.max(radius, 1.6);
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
        double effectiveRadius = Math.max(radius, 1.6);
        BoundingBox probeBox = probe.getBoundingBox().expand(Math.max(0.90, effectiveRadius));
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
