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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MageFireballBasicAttackSpell implements SpellHandler {
    private static final List<String> MODEL_CANDIDATES = List.of("fireball", "fireball.bbmodel", "fireball_bbmodel");
    public static final double DEFAULT_FORWARD_OFFSET = 0.55;
    public static final double DEFAULT_VERTICAL_OFFSET = 0.0;

    private static final double DEFAULT_SPEED_PER_TICK = 1.15;
    private static final double DEFAULT_MAX_RANGE = 30.0;
    private static final double DEFAULT_HIT_RADIUS = 0.45;
    private static final double TECHNIQUE_SCALE = 0.001;
    private static final int PROJECTILE_LIGHT_LEVEL = 12;

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

        caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_FIRECHARGE_USE, 0.7f, 1.2f);
        for (int i = 0; i < projectileCount; i++) {
            double yawOffset = computeYawOffset(i);
            Vector direction = rotateAroundY(baseDirection.clone(), yawOffset);
            FireballSpawnResult spawnResult = spawnProjectileAnchor(plugin, eye, direction);
            if (spawnResult == null) {
                continue;
            }
            if (spawnResult.modelResult().applied().isEmpty()) {
                ChatMessageUtil.send(caster, ChatMessageUtil.MessageType.WARNING,
                        "Fireball model was not found in ModelEngine. Showing particles only.");
            }
            if (debug) {
                ChatMessageUtil.send(caster, ChatMessageUtil.MessageType.INFO,
                        "[FireballDebug] Spawned projectile anchor id=" + spawnResult.anchor().getEntityId()
                                + " yawOffset=" + String.format("%.2f", yawOffset));
            }
            launchProjectile(caster, spawnResult.anchor(), direction, debug);
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

    private void launchProjectile(Player caster, ArmorStand projectile, Vector direction, boolean debug) {
        Vector step = direction.clone().normalize().multiply(DEFAULT_SPEED_PER_TICK);
        double maxDistanceSq = DEFAULT_MAX_RANGE * DEFAULT_MAX_RANGE;
        Location origin = projectile.getLocation().clone();

        LivingEntity immediateHit = findTargetAlongPath(caster.getEyeLocation(), origin, caster, projectile);
        if (immediateHit == null) {
            immediateHit = findTargetAlongPath(origin, origin.clone().add(step), caster, projectile);
        }
        if (immediateHit != null) {
            onImpact(caster, origin, immediateHit, debug);
            if (projectile.isValid()) {
                projectile.remove();
            }
            return;
        }

        new BukkitRunnable() {
            private int ticks;
            private Location activeLight;

            @Override
            public void run() {
                if (!projectile.isValid() || !caster.isOnline()) {
                    removeProjectile();
                    cancel();
                    return;
                }

                Location current = projectile.getLocation();
                if (current.distanceSquared(origin) >= maxDistanceSq) {
                    removeProjectile();
                    cancel();
                    return;
                }

                Location next = current.clone().add(step);
                projectile.teleport(next);
                ModelEngineUtil.orientEntityToVector(projectile, step);
                activeLight = SpellEffectUtil.moveTemporaryLight(activeLight, next, PROJECTILE_LIGHT_LEVEL);

                LivingEntity target = findTargetAlongPath(current, next, caster, projectile);
                if (target != null) {
                    onImpact(caster, next, target, debug);
                    removeProjectile();
                    cancel();
                    return;
                }

                ticks++;
                if (debug && ticks % 10 == 0) {
                    ChatMessageUtil.send(caster, ChatMessageUtil.MessageType.INFO,
                            "[FireballDebug] Traveling tick=" + ticks + " loc=" + format(next));
                }
            }

            private void removeProjectile() {
                SpellEffectUtil.clearTemporaryLight(activeLight);
                activeLight = null;
                if (projectile.isValid()) {
                    projectile.remove();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private LivingEntity findTargetAt(Location center, Player caster, ArmorStand projectile) {
        if (center == null || center.getWorld() == null) {
            return null;
        }
        for (var entity : center.getWorld().getNearbyEntities(center, DEFAULT_HIT_RADIUS, DEFAULT_HIT_RADIUS, DEFAULT_HIT_RADIUS)) {
            if (isValidSpellTarget(entity, caster, projectile)) {
                return (LivingEntity) entity;
            }
        }
        return null;
    }

    private LivingEntity findTargetAlongPath(Location start, Location end, Player caster, ArmorStand projectile) {
        if (start == null || end == null || start.getWorld() == null || !start.getWorld().equals(end.getWorld())) {
            return null;
        }
        LivingEntity directHit = findTargetAt(start, caster, projectile);
        if (directHit != null) {
            return directHit;
        }
        Vector segment = end.toVector().subtract(start.toVector());
        return SpellTargetingUtil.rayTraceLivingEntity(start, segment, DEFAULT_HIT_RADIUS,
                living -> isValidSpellTarget(living, caster, projectile));
    }

    private boolean isValidSpellTarget(org.bukkit.entity.Entity entity, Player caster, ArmorStand projectile) {
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
        SpellEffectUtil.applyDirectSpellDamage(plugin, caster, target, damage);
        if (burnTicks > 0) {
            target.setFireTicks(Math.max(target.getFireTicks(), burnTicks));
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

    private String format(Location location) {
        return String.format("%.2f %.2f %.2f", location.getX(), location.getY(), location.getZ());
    }
}
