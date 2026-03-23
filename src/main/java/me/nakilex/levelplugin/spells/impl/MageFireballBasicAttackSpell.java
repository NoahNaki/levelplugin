package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellEffectUtil;
import me.nakilex.levelplugin.spells.SpellHandler;
import me.nakilex.levelplugin.spells.SpellTargetingUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.ModelEngineUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MageFireballBasicAttackSpell implements SpellHandler {
    private static final List<String> MODEL_CANDIDATES = List.of("fireball", "fireball.bbmodel", "fireball_bbmodel");
    public static final double DEFAULT_FORWARD_OFFSET = 0.55;
    public static final double DEFAULT_VERTICAL_OFFSET = 0.0;

    private static final double DEFAULT_MAX_RANGE = 26.0;
    private static final double DEFAULT_HIT_RADIUS = 0.35;
    private static final double PROJECTILE_SPEED_PER_TICK = 1.05;
    private static final double PROJECTILE_HOMING_STRENGTH = 0.22;
    private static final double PROJECTILE_CURVE_STRENGTH = 0.22;
    private static final double PROJECTILE_CURVE_FREQUENCY = 0.38;
    private static final double PROJECTILE_TARGET_LOCK_RANGE = 14.0;
    private static final int PROJECTILE_MAX_LIFETIME_TICKS = 36;
    private static final double TECHNIQUE_SCALE = 0.001;

    private static final Set<UUID> DEBUG_PLAYERS = ConcurrentHashMap.newKeySet();

    private final Main plugin;
    private final int projectileCount;
    private final double coneDegrees;
    private final double baseDamage;
    private final double intelligenceScale;
    private final double splashRadius;
    private final double splashDamageFactor;
    private final int burnTicks;

    public MageFireballBasicAttackSpell(Main plugin) {
        this(plugin, 1, 0.0, 3.0, 0.45, 0.0, 0.0, 0);
    }

    public MageFireballBasicAttackSpell(Main plugin,
                                        int projectileCount,
                                        double coneDegrees,
                                        double baseDamage,
                                        double intelligenceScale,
                                        double splashRadius,
                                        double splashDamageFactor,
                                        int burnTicks) {
        this.plugin = plugin;
        this.projectileCount = Math.max(1, Math.min(3, projectileCount));
        this.coneDegrees = Math.max(0.0, coneDegrees);
        this.baseDamage = Math.max(0.0, baseDamage);
        this.intelligenceScale = Math.max(0.0, intelligenceScale);
        this.splashRadius = Math.max(0.0, splashRadius);
        this.splashDamageFactor = Math.max(0.0, splashDamageFactor);
        this.burnTicks = Math.max(0, burnTicks);
    }

    public record FireballSpawnResult(ArmorStand anchor,
                                      ModelEngineUtil.ModelApplyResult modelResult,
                                      Location spawnLocation,
                                      Vector direction) {
    }

    public static Location resolveSpawnLocation(Location eyeLocation, Vector rawDirection) {
        if (eyeLocation == null || rawDirection == null || rawDirection.lengthSquared() <= 0.000001) {
            return null;
        }
        Vector direction = rawDirection.clone().normalize();
        Location spawn = eyeLocation.clone().add(direction.clone().multiply(DEFAULT_FORWARD_OFFSET));
        spawn.add(0.0, DEFAULT_VERTICAL_OFFSET, 0.0);
        return spawn;
    }

    public static FireballSpawnResult spawnProjectileAnchor(Main plugin,
                                                            Location eyeLocation,
                                                            Vector rawDirection) {
        if (plugin == null || eyeLocation == null || rawDirection == null || rawDirection.lengthSquared() <= 0.000001) {
            return null;
        }
        Vector direction = rawDirection.clone().normalize();
        Location spawn = resolveSpawnLocation(eyeLocation, direction);
        if (spawn == null || spawn.getWorld() == null) {
            return null;
        }
        ArmorStand projectile = spawn.getWorld().spawn(spawn, ArmorStand.class, stand -> {
            stand.setInvisible(true);
            stand.setMarker(false);
            stand.setSmall(true);
            stand.setGravity(false);
            stand.setSilent(true);
            stand.setCollidable(false);
            stand.setInvulnerable(true);
        });
        ModelEngineUtil.ModelApplyResult modelResult =
                ModelEngineUtil.applyFirstAvailableModel(projectile, MODEL_CANDIDATES, plugin);
        ModelEngineUtil.orientEntityToVector(projectile, direction);
        return new FireballSpawnResult(projectile, modelResult, spawn, direction);
    }

    public static List<String> modelCandidates() {
        return MODEL_CANDIDATES;
    }

    public static void setDebugEnabled(UUID playerId, boolean enabled) {
        if (playerId == null) {
            return;
        }
        if (enabled) {
            DEBUG_PLAYERS.add(playerId);
        } else {
            DEBUG_PLAYERS.remove(playerId);
        }
    }

    public static boolean isDebugEnabled(UUID playerId) {
        return playerId != null && DEBUG_PLAYERS.contains(playerId);
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        boolean debug = isDebugEnabled(caster.getUniqueId());
        Location eye = caster.getEyeLocation().clone();
        Vector baseDirection = eye.getDirection().clone().normalize();
        LivingEntity lockTarget = SpellTargetingUtil.resolveTargetLivingEntity(
                caster,
                DEFAULT_MAX_RANGE,
                DEFAULT_HIT_RADIUS,
                living -> isValidSpellTarget(living, caster, null));

        caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_FIRECHARGE_USE, 0.7f, 1.2f);
        for (int i = 0; i < projectileCount; i++) {
            double yawOffset = computeYawOffset(i);
            Vector direction = rotateAroundY(baseDirection.clone(), yawOffset);
            if (debug) {
                ChatMessageUtil.send(caster, ChatMessageUtil.MessageType.INFO,
                        "[FireballDebug] Fired bolt yawOffset=" + String.format("%.2f", yawOffset));
            }
            launchHomingBolt(caster, eye, direction, lockTarget, debug, i);
        }
    }

    private double computeYawOffset(int index) {
        if (projectileCount <= 1 || coneDegrees <= 0.0) {
            return 0.0;
        }
        double step = coneDegrees / Math.max(1, projectileCount - 1);
        return (-coneDegrees / 2.0) + (step * index);
    }

    private Vector rotateAroundY(Vector vector, double degrees) {
        return vector.rotateAroundY(Math.toRadians(degrees));
    }

    private void launchHomingBolt(Player caster,
                                  Location eye,
                                  Vector direction,
                                  LivingEntity preferredTarget,
                                  boolean debug,
                                  int projectileIndex) {
        FireballSpawnResult spawnResult = spawnProjectileAnchor(plugin, eye, direction);
        if (spawnResult == null || spawnResult.anchor() == null || !spawnResult.anchor().isValid()) {
            return;
        }
        ArmorStand projectile = spawnResult.anchor();
        Vector initialDirection = spawnResult.direction().clone().normalize();
        double phaseOffset = projectileIndex * (Math.PI / 2.0);

        new BukkitRunnable() {
            private Vector travelDirection = initialDirection.clone();
            private double traveledDistance = 0.0;
            private int ticksLived = 0;

            @Override
            public void run() {
                if (!caster.isOnline() || !projectile.isValid()) {
                    cleanup();
                    return;
                }
                Location current = projectile.getLocation();
                World world = current.getWorld();
                if (world == null) {
                    cleanup();
                    return;
                }

                LivingEntity homingTarget = resolveHomingTarget(caster, current, preferredTarget);
                Vector desiredDirection = resolveDesiredDirection(current, travelDirection, homingTarget);
                travelDirection = blendDirection(travelDirection, desiredDirection, PROJECTILE_HOMING_STRENGTH);

                Vector lateral = travelDirection.clone().crossProduct(new Vector(0.0, 1.0, 0.0));
                if (lateral.lengthSquared() <= 0.000001) {
                    lateral = new Vector(1.0, 0.0, 0.0);
                }
                lateral.normalize().multiply(Math.sin((ticksLived * PROJECTILE_CURVE_FREQUENCY) + phaseOffset) * PROJECTILE_CURVE_STRENGTH);

                Vector step = travelDirection.clone().multiply(PROJECTILE_SPEED_PER_TICK).add(lateral);
                Location next = current.clone().add(step);

                LivingEntity hitEntity = SpellTargetingUtil.rayTraceLivingEntity(current, step, DEFAULT_HIT_RADIUS,
                        living -> isValidSpellTarget(living, caster, projectile));
                if (hitEntity != null) {
                    Location impact = hitEntity.getLocation().clone().add(0.0, Math.min(1.1, hitEntity.getHeight() * 0.5), 0.0);
                    onImpact(caster, impact, hitEntity, debug);
                    cleanup();
                    return;
                }

                RayTraceResult blockHit = world.rayTraceBlocks(current, step.clone().normalize(), step.length());
                if (blockHit != null && blockHit.getHitPosition() != null) {
                    Location impact = blockHit.getHitPosition().toLocation(world);
                    onImpact(caster, impact, null, debug);
                    cleanup();
                    return;
                }

                projectile.teleport(next);
                ModelEngineUtil.orientEntityToVector(projectile, travelDirection);
                SpellEffectUtil.spawnFireProjectileTrail(next);
                world.spawnParticle(Particle.END_ROD, next, 1, 0.03, 0.03, 0.03, 0.002);

                traveledDistance += step.length();
                ticksLived++;
                if (traveledDistance >= DEFAULT_MAX_RANGE || ticksLived >= PROJECTILE_MAX_LIFETIME_TICKS) {
                    onImpact(caster, next, null, debug);
                    cleanup();
                }
            }

            private void cleanup() {
                if (projectile.isValid()) {
                    projectile.remove();
                }
                cancel();
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private LivingEntity resolveHomingTarget(Player caster, Location projectileLocation, LivingEntity preferredTarget) {
        if (preferredTarget != null
                && preferredTarget.isValid()
                && !preferredTarget.isDead()
                && preferredTarget.getWorld().equals(projectileLocation.getWorld())
                && preferredTarget.getLocation().distanceSquared(projectileLocation)
                <= PROJECTILE_TARGET_LOCK_RANGE * PROJECTILE_TARGET_LOCK_RANGE
                && isValidSpellTarget(preferredTarget, caster, null)) {
            return preferredTarget;
        }
        LivingEntity fallback = SpellTargetingUtil.resolveTargetLivingEntity(
                caster,
                PROJECTILE_TARGET_LOCK_RANGE,
                DEFAULT_HIT_RADIUS,
                living -> isValidSpellTarget(living, caster, null));
        if (fallback == null || !fallback.getWorld().equals(projectileLocation.getWorld())) {
            return null;
        }
        return fallback;
    }

    private Vector resolveDesiredDirection(Location current, Vector fallbackDirection, LivingEntity homingTarget) {
        if (homingTarget == null || !homingTarget.isValid() || homingTarget.isDead()) {
            return fallbackDirection.clone().normalize();
        }
        Location targetPoint = homingTarget.getLocation().clone().add(0.0, Math.min(1.1, homingTarget.getHeight() * 0.5), 0.0);
        Vector toTarget = targetPoint.toVector().subtract(current.toVector());
        if (toTarget.lengthSquared() <= 0.000001) {
            return fallbackDirection.clone().normalize();
        }
        return toTarget.normalize();
    }

    private Vector blendDirection(Vector current, Vector desired, double homingStrength) {
        Vector blended = current.clone().normalize().multiply(Math.max(0.0, 1.0 - homingStrength))
                .add(desired.clone().normalize().multiply(Math.max(0.0, homingStrength)));
        if (blended.lengthSquared() <= 0.000001) {
            return desired.clone().normalize();
        }
        return blended.normalize();
    }

    private boolean isValidSpellTarget(Entity entity, Player caster, ArmorStand projectile) {
        if (!(entity instanceof LivingEntity living)) {
            return false;
        }
        return !living.isDead() && !living.equals(caster) && !living.equals(projectile) && !(living instanceof ArmorStand);
    }

    private void onImpact(Player caster, Location impact, LivingEntity target, boolean debug) {
        World world = impact.getWorld();
        if (world == null) {
            return;
        }
        SpellEffectUtil.spawnFireImpactEffect(impact);
        world.playSound(impact, Sound.BLOCK_FIRE_EXTINGUISH, 0.85f, 0.75f);

        double damage = SpellEffectUtil.computeIntTecScaledDamage(caster, baseDamage, intelligenceScale, TECHNIQUE_SCALE);
        if (target != null) {
            SpellEffectUtil.applyDirectSpellDamage(plugin, caster, target, damage, true);
            if (burnTicks > 0) {
                target.setFireTicks(Math.max(target.getFireTicks(), burnTicks));
            }
        }

        if (splashRadius > 0.0 && splashDamageFactor > 0.0) {
            double splashDamage = damage * splashDamageFactor;
            world.spawnParticle(Particle.EXPLOSION, impact, 1, 0.0, 0.0, 0.0, 0.0);
            world.spawnParticle(Particle.FLAME, impact, 20, splashRadius * 0.35, 0.2, splashRadius * 0.35, 0.01);
            SpellEffectUtil.applyAreaDamage(caster, impact, splashRadius, splashDamage);
        }

        if (debug) {
            ChatMessageUtil.send(caster, ChatMessageUtil.MessageType.INFO,
                    "[FireballDebug] Applied damage=" + String.format("%.2f", damage)
                            + " splashRadius=" + String.format("%.2f", splashRadius));
        }
    }

}
