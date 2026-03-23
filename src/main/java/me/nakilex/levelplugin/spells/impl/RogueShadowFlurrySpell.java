package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellEffectUtil;
import me.nakilex.levelplugin.spells.SpellHandler;
import me.nakilex.levelplugin.spells.SpellTargetingUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class RogueShadowFlurrySpell implements SpellHandler {
    private static final int BLADE_COUNT = 6;
    private static final double BASE_DAMAGE = 5.1;

    private final Main plugin;

    public RogueShadowFlurrySpell(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        Vector forward = caster.getLocation().getDirection().setY(0.0);
        if (forward.lengthSquared() <= 0.0001) {
            ChatMessageUtil.send(caster, ChatMessageUtil.MessageType.WARNING,
                    "No forward direction for Shadow Flurry.");
            return;
        }

        Vector baseDirection = forward.normalize();
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 0.65f, 1.45f);

        for (int bladeIndex = 0; bladeIndex < BLADE_COUNT; bladeIndex++) {
            int launchDelay = bladeIndex;
            int finalBladeIndex = bladeIndex;
            plugin.getServer().getScheduler().runTaskLater(plugin,
                    () -> launchBlade(caster, baseDirection, finalBladeIndex), launchDelay);
        }
    }

    private void launchBlade(Player caster, Vector baseDirection, int bladeIndex) {
        if (!caster.isOnline()) {
            return;
        }

        double angle = (bladeIndex - (BLADE_COUNT - 1) / 2.0) * 6.0;
        Vector direction = rotateAroundY(baseDirection, angle).normalize();
        Location point = caster.getEyeLocation().clone().add(direction.clone().multiply(0.6));
        Set<java.util.UUID> hitTargets = new HashSet<>();

        new BukkitRunnable() {
            private int step;

            @Override
            public void run() {
                if (!caster.isOnline() || step >= 16) {
                    cancel();
                    return;
                }

                Location previous = point.clone();
                point.add(direction.clone().multiply(0.9));
                point.getWorld().spawnParticle(Particle.SMOKE, point, 3, 0.08, 0.08, 0.08, 0.001);
                point.getWorld().spawnParticle(Particle.DUST, point, 2, 0.05, 0.05, 0.05,
                        new Particle.DustOptions(Color.fromRGB(32, 32, 32), 1.0f));

                LivingEntity target = SpellTargetingUtil.rayTraceLivingEntity(previous,
                        point.toVector().subtract(previous.toVector()),
                        0.4,
                        living -> !living.equals(caster) && hitTargets.add(living.getUniqueId()));
                if (target != null) {
                    double damage = BASE_DAMAGE + (bladeIndex * 0.35);
                    SpellEffectUtil.applyDirectSpellDamage(plugin, caster, target, damage, true);
                    point.getWorld().spawnParticle(Particle.CRIT, target.getLocation().clone().add(0.0, 1.0, 0.0),
                            8, 0.24, 0.18, 0.24, 0.04);
                    point.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.75f, 1.18f);
                    cancel();
                    return;
                }

                if (point.getBlock().isSolid()) {
                    point.getWorld().spawnParticle(Particle.SMOKE, point, 8, 0.18, 0.18, 0.18, 0.01);
                    cancel();
                    return;
                }
                step++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private Vector rotateAroundY(Vector vector, double angleDegrees) {
        double radians = Math.toRadians(angleDegrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vector(
                vector.getX() * cos - vector.getZ() * sin,
                vector.getY(),
                vector.getX() * sin + vector.getZ() * cos
        );
    }
}
