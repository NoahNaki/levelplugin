package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellEffectUtil;
import me.nakilex.levelplugin.spells.SpellHandler;
import me.nakilex.levelplugin.spells.SpellTargetingUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.PotionEffectUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RogueShadowFlurrySpell implements SpellHandler {
    private static final double MAX_CAST_DISTANCE = 5.0;
    private static final double CONTACT_DISTANCE = 1.9;
    private static final Map<UUID, SlamWindow> ACTIVE_SLAM_WINDOWS = new ConcurrentHashMap<>();

    private final Main plugin;
    private final int barrageHits;
    private final double baseDamage;
    private final double damagePerHit;
    private final int slowFallingTicks;
    private final double slamRadius;
    private final double slamDamage;

    public RogueShadowFlurrySpell(Main plugin,
                                  int barrageHits,
                                  double baseDamage,
                                  double damagePerHit,
                                  int slowFallingTicks,
                                  double slamRadius,
                                  double slamDamage) {
        this.plugin = plugin;
        this.barrageHits = Math.max(1, barrageHits);
        this.baseDamage = baseDamage;
        this.damagePerHit = damagePerHit;
        this.slowFallingTicks = Math.max(20, slowFallingTicks);
        this.slamRadius = Math.max(1.0, slamRadius);
        this.slamDamage = Math.max(0.1, slamDamage);
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        LivingEntity target = SpellTargetingUtil.resolveTargetLivingEntity(caster, 16.0, 0.45,
                living -> !living.equals(caster));
        if (target == null) {
            ChatMessageUtil.send(caster, ChatMessageUtil.MessageType.WARNING,
                    "Look at a target mob for Shadow Flurry.");
            return;
        }
        if (caster.getLocation().distanceSquared(target.getLocation()) > MAX_CAST_DISTANCE * MAX_CAST_DISTANCE) {
            ChatMessageUtil.send(caster, ChatMessageUtil.MessageType.WARNING,
                    "Move within 5 blocks to use Shadow Flurry.");
            return;
        }

        PotionEffectUtil.applyHiddenEffect(caster, PotionEffectType.SLOW_FALLING, slowFallingTicks, 0);
        ACTIVE_SLAM_WINDOWS.put(caster.getUniqueId(), new SlamWindow(System.currentTimeMillis() + (slowFallingTicks * 50L), slamRadius, slamDamage));
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.55f, 1.45f);

        new BukkitRunnable() {
            private int hitIndex;

            @Override
            public void run() {
                if (!caster.isOnline() || !target.isValid() || target.isDead() || hitIndex >= barrageHits) {
                    cancel();
                    return;
                }

                Location casterPoint = caster.getLocation().clone().add(0.0, 1.0, 0.0);
                Location targetPoint = target.getLocation().clone().add(0.0, 1.0, 0.0);
                Vector toTarget = targetPoint.toVector().subtract(casterPoint.toVector());
                if (toTarget.lengthSquared() <= 0.0001) {
                    toTarget = caster.getLocation().getDirection().setY(0.0);
                }
                toTarget.normalize();

                caster.setVelocity(toTarget.clone().multiply(1.18).add(new Vector(0.0, 0.06, 0.0)));
                caster.getWorld().spawnParticle(Particle.CRIT, casterPoint, 8, 0.18, 0.10, 0.18, 0.02);
                caster.getWorld().playSound(casterPoint, Sound.ENTITY_ENDER_DRAGON_FLAP,
                        0.48f, 1.24f + (hitIndex * 0.05f));

                int iteration = hitIndex;
                Vector dashDirection = toTarget.clone();
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> tryImpact(caster, target, dashDirection, iteration, 0), 2L);
                hitIndex++;
            }
        }.runTaskTimer(plugin, 0L, 8L);
    }

    private void tryImpact(Player caster,
                           LivingEntity target,
                           Vector dashDirection,
                           int hitIndex,
                           int attempt) {
        if (caster == null || target == null || !caster.isOnline() || !target.isValid() || target.isDead()) {
            return;
        }

        double distance = caster.getLocation().distance(target.getLocation());
        if (distance > CONTACT_DISTANCE && attempt < 2) {
            Vector correction = target.getLocation().toVector().subtract(caster.getLocation().toVector());
            if (correction.lengthSquared() > 0.0001) {
                caster.setVelocity(correction.normalize().multiply(0.78).setY(0.04));
            }
            plugin.getServer().getScheduler().runTaskLater(plugin,
                    () -> tryImpact(caster, target, dashDirection, hitIndex, attempt + 1),
                    1L);
            return;
        }
        if (distance > CONTACT_DISTANCE + 0.8) {
            return;
        }

        Location targetPoint = target.getLocation().clone().add(0.0, 1.0, 0.0);
        double damage = baseDamage + (hitIndex * damagePerHit);
        SpellEffectUtil.applyDirectSpellDamage(plugin, caster, target, damage, true);
        target.setVelocity(target.getVelocity().multiply(0.84).add(new Vector(0.0, 0.15 + (hitIndex * 0.02), 0.0)));

        Vector rebound = dashDirection.clone().multiply(-0.68).setY(0.36 + (hitIndex * 0.03));
        caster.setVelocity(rebound);

        target.getWorld().spawnParticle(Particle.CRIT, targetPoint, 12 + (hitIndex * 2), 0.28, 0.18, 0.28, 0.03);
        target.getWorld().playSound(targetPoint, Sound.ENTITY_PLAYER_ATTACK_SWEEP,
                0.9f, 1.1f + (hitIndex * 0.07f));
    }

    public static boolean tryTriggerAirSlam(Main plugin, Player player) {
        if (plugin == null || player == null || !player.isOnline()) {
            return false;
        }

        SlamWindow window = ACTIVE_SLAM_WINDOWS.get(player.getUniqueId());
        if (window == null || System.currentTimeMillis() > window.expiresAt()) {
            ACTIVE_SLAM_WINDOWS.remove(player.getUniqueId());
            return false;
        }
        if (player.isOnGround()) {
            return false;
        }

        ACTIVE_SLAM_WINDOWS.remove(player.getUniqueId());
        PotionEffectUtil.removeEffect(player, PotionEffectType.SLOW_FALLING);
        player.setVelocity(new Vector(0.0, -1.9, 0.0));

        new BukkitRunnable() {
            private int ticks;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                player.setFallDistance(0.0f);
                if (player.isOnGround() || ticks >= 22) {
                    Location impact = player.getLocation().clone().add(0.0, 0.1, 0.0);
                    SpellEffectUtil.applyAreaDamage(player, impact, window.radius(), window.damage());
                    impact.getWorld().spawnParticle(Particle.EXPLOSION, impact, 1, 0.0, 0.0, 0.0, 0.0);
                    impact.getWorld().spawnParticle(Particle.CRIT, impact, 22, 0.45, 0.08, 0.45, 0.05);
                    impact.getWorld().spawnParticle(Particle.CLOUD, impact, 18, 0.35, 0.05, 0.35, 0.01);
                    impact.getWorld().playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.2f);
                    cancel();
                    return;
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
        return true;
    }

    private record SlamWindow(long expiresAt, double radius, double damage) {
    }
}
