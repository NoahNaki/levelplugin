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

public class RogueShadowFlurrySpell implements SpellHandler {
    private static final int BARRAGE_HITS = 4;
    private static final double MAX_CAST_DISTANCE = 5.0;
    private static final double CONTACT_DISTANCE = 1.9;

    private final Main plugin;

    public RogueShadowFlurrySpell(Main plugin) {
        this.plugin = plugin;
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

        PotionEffectUtil.applyHiddenEffect(caster, PotionEffectType.SLOW_FALLING, 80, 0);
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.55f, 1.45f);

        new BukkitRunnable() {
            private int hitIndex;

            @Override
            public void run() {
                if (!caster.isOnline() || !target.isValid() || target.isDead() || hitIndex >= BARRAGE_HITS) {
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
        double damage = 6.0 + (hitIndex * 0.8);
        SpellEffectUtil.applyDirectSpellDamage(plugin, caster, target, damage, true);
        target.setVelocity(target.getVelocity().multiply(0.84).add(new Vector(0.0, 0.15 + (hitIndex * 0.02), 0.0)));

        Vector rebound = dashDirection.clone().multiply(-0.68).setY(0.36 + (hitIndex * 0.03));
        caster.setVelocity(rebound);

        target.getWorld().spawnParticle(Particle.CRIT, targetPoint, 12 + (hitIndex * 2), 0.28, 0.18, 0.28, 0.03);
        target.getWorld().playSound(targetPoint, Sound.ENTITY_PLAYER_ATTACK_SWEEP,
                0.9f, 1.1f + (hitIndex * 0.07f));
    }
}
