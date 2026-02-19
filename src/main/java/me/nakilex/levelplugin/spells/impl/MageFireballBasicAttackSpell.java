package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellEffectUtil;
import me.nakilex.levelplugin.spells.SpellHandler;
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
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.List;

public class MageFireballBasicAttackSpell implements SpellHandler {
    private static final List<String> MODEL_CANDIDATES = List.of("fireball", "fireball.bbmodel", "fireball_bbmodel");
    public static final double DEFAULT_FORWARD_OFFSET = 0.65;
    public static final double DEFAULT_VERTICAL_OFFSET = -0.15;

    private static final double SPEED_PER_TICK = 1.15;
    private static final double MAX_RANGE = 30.0;
    private static final double HIT_RADIUS = 0.45;
    private static final double BASE_DAMAGE = 3.0;
    private static final double INTELLIGENCE_SCALE = 0.45;
    private static final double TECHNIQUE_SCALE = 0.001;

    private final Main plugin;

    public MageFireballBasicAttackSpell(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        Location eye = caster.getEyeLocation().clone();
        Vector direction = eye.getDirection().clone();
        if (direction.lengthSquared() <= 0.000001) {
            return;
        }
        direction.normalize();

        Location spawn = eye.add(direction.clone().multiply(DEFAULT_FORWARD_OFFSET));
        spawn.add(0.0, DEFAULT_VERTICAL_OFFSET, 0.0);

        World world = spawn.getWorld();
        if (world == null) {
            return;
        }

        ArmorStand projectile = world.spawn(spawn, ArmorStand.class, stand -> {
            stand.setInvisible(true);
            stand.setMarker(false);
            stand.setSmall(true);
            stand.setGravity(false);
            stand.setSilent(true);
            stand.setCollidable(false);
            stand.setInvulnerable(true);
        });
        ModelEngineUtil.ModelApplyResult modelResult = ModelEngineUtil.applyFirstAvailableModel(projectile, MODEL_CANDIDATES, plugin);
        if (modelResult.applied().isEmpty()) {
            ChatMessageUtil.send(caster, ChatMessageUtil.MessageType.WARNING,
                    "Fireball model was not found in ModelEngine. Showing particles only.");
        }
        ModelEngineUtil.orientEntityToVector(projectile, direction);

        launchProjectile(caster, projectile, direction);
    }

    private void launchProjectile(Player caster, ArmorStand projectile, Vector direction) {
        Vector step = direction.clone().normalize().multiply(SPEED_PER_TICK);
        double maxDistanceSq = MAX_RANGE * MAX_RANGE;
        Location origin = projectile.getLocation().clone();
        new BukkitRunnable() {
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

                World world = current.getWorld();
                if (world == null) {
                    removeProjectile();
                    cancel();
                    return;
                }

                RayTraceResult hit = world.rayTrace(current, step.clone().normalize(), step.length() + HIT_RADIUS,
                        org.bukkit.FluidCollisionMode.NEVER,
                        true,
                        HIT_RADIUS,
                        entity -> entity instanceof LivingEntity living
                                && !living.equals(caster)
                                && !living.isDead());
                if (hit != null) {
                    Location impact = hit.getHitPosition().toLocation(world);
                    LivingEntity target = hit.getHitEntity() instanceof LivingEntity living ? living : null;
                    onImpact(caster, impact, target);
                    removeProjectile();
                    cancel();
                    return;
                }

                Location next = current.clone().add(step);
                projectile.teleport(next);
                ModelEngineUtil.orientEntityToVector(projectile, step);
                world.spawnParticle(Particle.FLAME, next, 2, 0.05, 0.05, 0.05, 0.01);
            }

            private void removeProjectile() {
                if (projectile.isValid()) {
                    projectile.remove();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void onImpact(Player caster, Location impact, LivingEntity target) {
        World world = impact.getWorld();
        if (world == null) {
            return;
        }
        world.spawnParticle(Particle.FLAME, impact, 24, 0.35, 0.2, 0.35, 0.04);
        world.spawnParticle(Particle.SMOKE, impact, 10, 0.2, 0.1, 0.2, 0.01);
        world.playSound(impact, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 1.25f);

        if (target != null) {
            double damage = SpellEffectUtil.computeIntTecScaledDamage(caster, BASE_DAMAGE, INTELLIGENCE_SCALE, TECHNIQUE_SCALE);
            SpellEffectUtil.applyDirectSpellDamage(plugin, caster, target, damage);
        }
    }
}
