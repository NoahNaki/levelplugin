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
    private static final double DEFAULT_TRAIL_STEP = 0.6;
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
    private final int chainBounces;

    public MageFireballBasicAttackSpell(Main plugin) {
        this(plugin, 3, 0.0, 3.0, 0.45, 0.0, 0.0, 0);
    }

    public MageFireballBasicAttackSpell(Main plugin,
                                        int projectileCount,
                                        double coneDegrees,
                                        double baseDamage,
                                        double intelligenceScale,
                                        double splashRadius,
                                        double splashDamageFactor,
                                        int burnTicks) {
        this(plugin, projectileCount, coneDegrees, baseDamage, intelligenceScale, splashRadius, splashDamageFactor, burnTicks, 0);
    }

    public MageFireballBasicAttackSpell(Main plugin,
                                        int projectileCount,
                                        double coneDegrees,
                                        double baseDamage,
                                        double intelligenceScale,
                                        double splashRadius,
                                        double splashDamageFactor,
                                        int burnTicks,
                                        int chainBounces) {
        this.plugin = plugin;
        this.projectileCount = Math.max(1, Math.min(3, projectileCount));
        this.coneDegrees = Math.max(0.0, coneDegrees);
        this.baseDamage = Math.max(0.0, baseDamage);
        this.intelligenceScale = Math.max(0.0, intelligenceScale);
        this.splashRadius = Math.max(0.0, splashRadius);
        this.splashDamageFactor = Math.max(0.0, splashDamageFactor);
        this.burnTicks = Math.max(0, burnTicks);
        this.chainBounces = Math.max(0, chainBounces);
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
        Vector baseDirection = resolveCastDirection(context, caster, eye);
        List<Vector> targetDirections = resolveProjectileDirections(caster, eye, baseDirection);
        if (targetDirections.isEmpty()) {
            return;
        }

        caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_FIRECHARGE_USE, 0.7f, 1.2f);
        for (int i = 0; i < targetDirections.size(); i++) {
            Vector direction = targetDirections.get(i);
            if (debug) {
                ChatMessageUtil.send(caster, ChatMessageUtil.MessageType.INFO,
                        "[FireballDebug] Fired bolt index=" + i);
            }
            fireInstantBolt(caster, eye, direction, debug);
        }
    }

    private List<Vector> resolveProjectileDirections(Player caster, Location eye, Vector baseDirection) {
        List<Vector> directions = new java.util.ArrayList<>();
        if (baseDirection == null || baseDirection.lengthSquared() <= 0.000001) {
            return directions;
        }
        for (int i = 0; i < projectileCount; i++) {
            double yawOffset = computeYawOffset(i);
            directions.add(rotateAroundY(baseDirection.clone(), yawOffset).normalize());
        }
        return directions;
    }

    private Vector resolveCastDirection(SpellContext context, Player caster, Location eye) {
        return eye.getDirection().clone().normalize();
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

    private void fireInstantBolt(Player caster, Location eye, Vector direction, boolean debug) {
        Vector normalized = direction.clone().normalize();
        Location start = eye.clone().add(normalized.clone().multiply(0.35));
        Vector segment = normalized.clone().multiply(DEFAULT_MAX_RANGE);
        LivingEntity target = SpellTargetingUtil.rayTraceLivingEntity(start, segment, DEFAULT_HIT_RADIUS,
                living -> isValidSpellTarget(living, caster, null));

        double travelDistance = DEFAULT_MAX_RANGE;
        Location impact = start.clone().add(segment);
        if (target != null) {
            impact = target.getLocation().clone().add(0.0, Math.min(1.1, target.getHeight() * 0.5), 0.0);
            travelDistance = Math.max(0.2, impact.distance(start));
        }

        spawnBoltTrail(start, normalized, travelDistance);
        onImpact(caster, impact, target, debug);
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
            if (chainBounces > 0) {
                applyChainLightning(caster, target, damage);
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

    private void applyChainLightning(Player caster, LivingEntity firstTarget, double baseDamage) {
        LivingEntity current = firstTarget;
        java.util.Set<UUID> hit = new java.util.HashSet<>();
        hit.add(firstTarget.getUniqueId());
        for (int bounce = 0; bounce < chainBounces; bounce++) {
            LivingEntity anchor = current;
            LivingEntity next = SpellEffectUtil.getLivingTargets(anchor.getLocation(), 8.0,
                            living -> !living.equals(caster) && !hit.contains(living.getUniqueId()))
                    .stream()
                    .min(java.util.Comparator.comparingDouble(living -> living.getLocation().distanceSquared(anchor.getLocation())))
                    .orElse(null);
            if (next == null) {
                return;
            }
            World world = current.getWorld();
            if (world != null) {
                world.spawnParticle(Particle.ELECTRIC_SPARK, current.getEyeLocation(), 10, 0.15, 0.15, 0.15, 0.01);
                world.spawnParticle(Particle.ELECTRIC_SPARK, next.getEyeLocation(), 10, 0.15, 0.15, 0.15, 0.01);
                world.playSound(next.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.35f, 1.6f);
            }
            double bouncedDamage = baseDamage * Math.max(0.35, 1.0 - ((bounce + 1) * 0.15));
            SpellEffectUtil.applyDirectSpellDamage(plugin, caster, next, bouncedDamage, true);
            hit.add(next.getUniqueId());
            current = next;
        }
    }

    private void spawnBoltTrail(Location start, Vector direction, double distance) {
        if (start == null || start.getWorld() == null || direction == null || distance <= 0.0) {
            return;
        }
        World world = start.getWorld();
        double clampedDistance = Math.min(DEFAULT_MAX_RANGE, Math.max(0.2, distance));
        Vector step = direction.clone().normalize().multiply(DEFAULT_TRAIL_STEP);
        int points = Math.max(1, (int) Math.ceil(clampedDistance / DEFAULT_TRAIL_STEP));
        Location point = start.clone();
        for (int i = 0; i < points; i++) {
            world.spawnParticle(Particle.FLAME, point, 1, 0.01, 0.01, 0.01, 0.002);
            point.add(step);
        }
        world.spawnParticle(Particle.FLAME, point, 2, 0.02, 0.02, 0.02, 0.006);
    }

}
