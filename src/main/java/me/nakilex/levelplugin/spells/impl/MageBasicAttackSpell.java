package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.attributes.listeners.StatsEffectListener;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellDamageUtil;
import me.nakilex.levelplugin.spells.SpellHandler;
import me.nakilex.levelplugin.utils.ModelEngineUtil;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Locale;

public class MageBasicAttackSpell implements SpellHandler {
    private static final String MODEL_ID = "fireball";
    private static final double SPEED_PER_TICK = 1.0;
    private static final double MAX_RANGE = 26.0;
    private static final double BASE_DAMAGE = 3.0;
    private static final double INTELLIGENCE_SCALING = 0.35;
    private static final double TECHNIQUE_SCALING = 0.001;
    private static final double MODEL_HEIGHT_OFFSET = -1.55;
    private static final float MODEL_YAW_OFFSET = 0.0f;
    private static final double PARTICLE_HEIGHT_OFFSET = 1.0;
    private static final double GROUND_PENETRATION_BLOCKS = 2.0;

    private final Main plugin;

    public MageBasicAttackSpell(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        CastTransform transform = CastTransform.fromCaster(caster, MODEL_YAW_OFFSET, MODEL_HEIGHT_OFFSET);
        if (transform == null || transform.spawn().getWorld() == null) {
            return;
        }

        Snowball projectile = spawnProjectile(caster, transform);
        if (projectile == null) {
            return;
        }

        applyModel(projectile);
        syncProjectileTransform(projectile, transform, transform.spawn());
        logCastFacingDebug(caster, projectile, transform);
        launchProjectile(caster, projectile, transform);
    }

    private Snowball spawnProjectile(Player caster, CastTransform transform) {
        World world = transform.spawn().getWorld();
        if (world == null) {
            return null;
        }
        return world.spawn(transform.spawn(), Snowball.class, snowball -> {
            snowball.setShooter(caster);
            snowball.setGravity(false);
            snowball.setSilent(true);
            snowball.setInvulnerable(true);
            snowball.setVelocity(new Vector(0, 0, 0));
            syncProjectileTransform(snowball, transform, transform.spawn());
        });
    }

    private void applyModel(Entity projectile) {
        ModelEngineUtil.ModelApplyResult result = ModelEngineUtil.applyModels(projectile, List.of(MODEL_ID), plugin);
        if (!result.failed().isEmpty()) {
            plugin.getLogger().warning("Mage basic attack failed to apply model: " + String.join(", ", result.failed()));
        }
    }

    private void launchProjectile(Player caster, Snowball projectile, CastTransform transform) {
        new BukkitRunnable() {
            private double travelled;
            private double remainingGroundPenetration = GROUND_PENETRATION_BLOCKS;

            @Override
            public void run() {
                if (!caster.isOnline() || !projectile.isValid()) {
                    cleanup();
                    return;
                }
                if (travelled >= MAX_RANGE) {
                    despawnAt(projectile.getLocation());
                    cleanup();
                    return;
                }

                Location current = projectile.getLocation();
                RayTraceResult blockHit = current.getWorld().rayTraceBlocks(
                        current,
                        transform.travelDirection(),
                        SPEED_PER_TICK,
                        FluidCollisionMode.NEVER,
                        true);

                RayTraceResult entityHit = current.getWorld().rayTraceEntities(
                        current,
                        transform.travelDirection(),
                        SPEED_PER_TICK,
                        0.5,
                        entity -> isValidTarget(entity, caster));

                if (didHitEntityFirst(current, blockHit, entityHit)) {
                    LivingEntity target = (LivingEntity) entityHit.getHitEntity();
                    Location impact = entityHit.getHitPosition().toLocation(current.getWorld());
                    hitEntity(caster, target, impact);
                    cleanup();
                    return;
                }

                if (blockHit != null && remainingGroundPenetration <= 0.0) {
                    Location impact = blockHit.getHitPosition().toLocation(current.getWorld());
                    despawnAt(impact);
                    cleanup();
                    return;
                }

                double step = SPEED_PER_TICK;
                if (blockHit != null) {
                    step = Math.min(SPEED_PER_TICK, Math.max(0.0, remainingGroundPenetration));
                    remainingGroundPenetration -= step;
                } else {
                    remainingGroundPenetration = GROUND_PENETRATION_BLOCKS;
                }

                Location moved = current.clone().add(transform.travelDirection().clone().multiply(step));
                syncProjectileTransform(projectile, transform, moved);
                renderTravel(moved);
                travelled += step;
            }

            private void cleanup() {
                if (projectile.isValid()) {
                    projectile.remove();
                }
                cancel();
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private boolean isValidTarget(Entity entity, Player caster) {
        if (!(entity instanceof LivingEntity living)) {
            return false;
        }
        return !living.isDead() && !living.equals(caster) && !(living instanceof ArmorStand);
    }

    private boolean didHitEntityFirst(Location origin, RayTraceResult blockHit, RayTraceResult entityHit) {
        if (entityHit == null || entityHit.getHitEntity() == null || entityHit.getHitPosition() == null) {
            return false;
        }
        if (blockHit == null || blockHit.getHitPosition() == null) {
            return true;
        }
        double entityDistance = origin.toVector().distance(entityHit.getHitPosition());
        double blockDistance = origin.toVector().distance(blockHit.getHitPosition());
        return entityDistance <= blockDistance;
    }

    private void syncProjectileTransform(Snowball projectile, CastTransform transform, Location target) {
        if (projectile == null || !projectile.isValid() || transform == null || target == null) {
            return;
        }
        Location transformed = transform.oriented(target);
        projectile.teleport(transformed);
        projectile.setVelocity(new Vector(0, 0, 0));
    }

    private void logCastFacingDebug(Player caster, Entity projectile, CastTransform transform) {
        if (plugin == null || caster == null || projectile == null || transform == null) {
            return;
        }
        Location projectileLoc = projectile.getLocation();
        plugin.getLogger().info("[MageFireballDebugSpawn] player=" + caster.getName()
                + " playerYaw=" + fmt(caster.getEyeLocation().getYaw())
                + " playerPitch=" + fmt(caster.getEyeLocation().getPitch())
                + " facingYaw=" + fmt(transform.facingYaw())
                + " facingPitch=" + fmt(transform.facingPitch())
                + " modelYaw=" + fmt(projectileLoc.getYaw())
                + " modelPitch=" + fmt(projectileLoc.getPitch()));
    }

    private String fmt(float value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private void hitEntity(Player caster, LivingEntity target, Location impact) {
        double damage = SpellDamageUtil.computeScaledDamage(
                caster,
                BASE_DAMAGE,
                StatsManager.StatType.INT,
                INTELLIGENCE_SCALING,
                TECHNIQUE_SCALING);

        StatsEffectListener.markSkipNextScaling(caster);
        target.damage(damage, caster);

        World world = impact.getWorld();
        if (world == null) {
            return;
        }
        world.spawnParticle(Particle.FLAME, impact, 20, 0.25, 0.25, 0.25, 0.02);
        world.spawnParticle(Particle.SMALL_FLAME, impact, 8, 0.2, 0.2, 0.2, 0.01);
        world.playSound(impact, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 1.2f);
    }

    private void renderTravel(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        Location trail = location.clone().add(0.0, PARTICLE_HEIGHT_OFFSET, 0.0);
        world.spawnParticle(Particle.SMALL_FLAME, trail, 2, 0.05, 0.05, 0.05, 0.0);
    }

    private void despawnAt(Location impact) {
        World world = impact.getWorld();
        if (world == null) {
            return;
        }
        world.spawnParticle(Particle.SMOKE, impact, 4, 0.15, 0.15, 0.15, 0.01);
    }

    private record CastTransform(Location spawn,
                                 Vector travelDirection,
                                 float facingYaw,
                                 float facingPitch) {
        static CastTransform fromCaster(Player caster, float yawOffset, double heightOffset) {
            if (caster == null) {
                return null;
            }
            Location eye = caster.getEyeLocation();
            Vector direction = eye.getDirection().normalize();
            Location spawn = eye.clone().add(direction.clone().multiply(0.6)).add(0.0, heightOffset, 0.0);
            return new CastTransform(spawn,
                    direction,
                    eye.getYaw() + yawOffset,
                    eye.getPitch());
        }

        Location oriented(Location location) {
            if (location == null) {
                return null;
            }
            Location oriented = location.clone();
            oriented.setDirection(travelDirection);
            oriented.setYaw(facingYaw);
            oriented.setPitch(facingPitch);
            return oriented;
        }
    }
}
