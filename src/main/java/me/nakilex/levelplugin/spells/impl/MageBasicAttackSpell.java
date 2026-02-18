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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.List;

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
        Location eye = caster.getEyeLocation();
        World world = eye.getWorld();
        if (world == null) {
            return;
        }

        Vector direction = eye.getDirection().normalize();
        float castYaw = eye.getYaw();
        float castPitch = eye.getPitch();
        Location spawn = eye.clone().add(direction.clone().multiply(0.6)).add(0.0, MODEL_HEIGHT_OFFSET, 0.0);
        Location spawnFacing = resolveFacingLocation(spawn, castYaw, castPitch);

        float finalYaw = castYaw + MODEL_YAW_OFFSET;
        float finalPitch = castPitch;
        ArmorStand projectile = world.spawn(spawnFacing, ArmorStand.class, stand -> {
            stand.setInvisible(true);
            stand.setMarker(true);
            stand.setGravity(false);
            stand.setSilent(true);
            stand.setCollidable(false);
            stand.setInvulnerable(true);
            stand.setRotation(finalYaw, finalPitch);
        });
        syncModelTransform(projectile, spawn, castYaw, castPitch);

        ModelEngineUtil.ModelApplyResult result = ModelEngineUtil.applyModels(projectile, List.of(MODEL_ID), plugin);
        if (!result.failed().isEmpty()) {
            plugin.getLogger().warning("Mage basic attack failed to apply model: " + String.join(", ", result.failed()));
        }
        syncModelTransform(projectile, spawn, castYaw, castPitch);
        logCastFacingDebug(caster, projectile, castYaw, castPitch);
        plugin.getLogger().info("[MageFireballDebugSpawn] y=" + eyeFmt(projectile.getLocation().getYaw())
                + " p=" + eyeFmt(projectile.getLocation().getPitch())
                + " expectedY=" + eyeFmt(castYaw + MODEL_YAW_OFFSET)
                + " expectedP=" + eyeFmt(castPitch));

        launchProjectile(caster, projectile, direction, castYaw, castPitch);
    }

    private void launchProjectile(Player caster, ArmorStand projectile, Vector direction, float castYaw, float castPitch) {
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
                        direction,
                        SPEED_PER_TICK,
                        FluidCollisionMode.NEVER,
                        true);

                RayTraceResult entityHit = current.getWorld().rayTraceEntities(
                        current,
                        direction,
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

                Vector runtimeDirection = direction.clone().normalize();
                if (blockHit != null) {
                    double step = Math.min(SPEED_PER_TICK, Math.max(0.0, remainingGroundPenetration));
                    Location moved = current.clone().add(runtimeDirection.clone().multiply(step));
                    Location next = resolveFacingLocation(moved, castYaw, castPitch);
                    syncModelTransform(projectile, moved, castYaw, castPitch);
                    renderTravel(next);
                    travelled += step;
                    remainingGroundPenetration -= step;
                    if (remainingGroundPenetration <= 0.0) {
                        despawnAt(moved);
                        cleanup();
                    }
                    return;
                }

                Location moved = current.clone().add(runtimeDirection.clone().multiply(SPEED_PER_TICK));
                Location next = resolveFacingLocation(moved, castYaw, castPitch);
                syncModelTransform(projectile, moved, castYaw, castPitch);
                renderTravel(next);
                travelled += SPEED_PER_TICK;
                remainingGroundPenetration = GROUND_PENETRATION_BLOCKS;
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

    private Location resolveFacingLocation(Location location, float baseYaw, float basePitch) {
        if (location == null) {
            return null;
        }
        Location oriented = location.clone();
        oriented.setYaw(baseYaw + MODEL_YAW_OFFSET);
        oriented.setPitch(basePitch);
        return oriented;
    }

    private void syncModelTransform(ArmorStand projectile, Location target, float baseYaw, float basePitch) {
        if (projectile == null || !projectile.isValid() || target == null) {
            return;
        }
        float finalYaw = baseYaw + MODEL_YAW_OFFSET;
        Location transformed = target.clone();
        transformed.setYaw(finalYaw);
        transformed.setPitch(basePitch);
        projectile.teleport(transformed);
        projectile.setRotation(finalYaw, basePitch);
    }

    private void logCastFacingDebug(Player caster, ArmorStand projectile, float castYaw, float castPitch) {
        if (plugin == null || caster == null || projectile == null) {
            return;
        }
        Location projectileLoc = projectile.getLocation();
        plugin.getLogger().info("[MageFireballDebug] player=" + caster.getName()
                + " playerYaw=" + eyeFmt(caster.getEyeLocation().getYaw())
                + " playerPitch=" + eyeFmt(caster.getEyeLocation().getPitch())
                + " castYaw=" + eyeFmt(castYaw)
                + " castPitch=" + eyeFmt(castPitch)
                + " modelYaw=" + eyeFmt(projectileLoc.getYaw())
                + " modelPitch=" + eyeFmt(projectileLoc.getPitch()));
    }

    private String eyeFmt(float value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
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
}
