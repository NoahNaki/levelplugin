package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellEffectUtil;
import me.nakilex.levelplugin.spells.SpellHandler;
import me.nakilex.levelplugin.spells.SpellTargetingUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class RogueNightfallLungeSpell implements SpellHandler {
    private final Main plugin;

    public RogueNightfallLungeSpell(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        LivingEntity mainTarget = SpellTargetingUtil.resolveTargetLivingEntity(caster, 15.0, 0.45,
                living -> !living.equals(caster));
        if (mainTarget == null) {
            ChatMessageUtil.send(caster, ChatMessageUtil.MessageType.WARNING,
                    "Look at a target mob for Nightfall Lunge.");
            return;
        }

        teleportBehindTarget(caster, mainTarget);

        Location detonationCenter = mainTarget.getLocation().clone().add(0.0, 1.0, 0.0);
        List<LivingEntity> markedTargets = new ArrayList<>(SpellEffectUtil.getLivingTargets(detonationCenter, 3.8,
                living -> !living.equals(caster) && !(living instanceof Player)));
        if (markedTargets.isEmpty()) {
            markedTargets.add(mainTarget);
        }

        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.55f, 1.65f);
        caster.getWorld().spawnParticle(Particle.LARGE_SMOKE, detonationCenter, 18, 0.45, 0.35, 0.45, 0.01);

        new BukkitRunnable() {
            private int ticks;

            @Override
            public void run() {
                if (!caster.isOnline()) {
                    cancel();
                    return;
                }

                markedTargets.removeIf(target -> !target.isValid() || target.isDead());
                if (markedTargets.isEmpty()) {
                    cancel();
                    return;
                }

                for (LivingEntity target : markedTargets) {
                    Location marker = target.getLocation().clone().add(0.0, target.getHeight() + 0.35, 0.0);
                    marker.getWorld().spawnParticle(Particle.ENCHANTED_HIT, marker, 4, 0.22, 0.06, 0.22, 0.0);
                    marker.getWorld().spawnParticle(Particle.SMOKE, marker, 2, 0.14, 0.05, 0.14, 0.002);
                }

                ticks += 4;
                if (ticks < 20) {
                    return;
                }

                for (LivingEntity target : markedTargets) {
                    Location hit = target.getLocation().clone().add(0.0, 1.0, 0.0);
                    double damage = target.equals(mainTarget) ? 11.8 : 8.6;
                    SpellEffectUtil.applyDirectSpellDamage(plugin, caster, target, damage, true);
                    hit.getWorld().spawnParticle(Particle.EXPLOSION, hit, 1, 0.0, 0.0, 0.0, 0.0);
                    hit.getWorld().spawnParticle(Particle.SMOKE, hit, 14, 0.30, 0.18, 0.30, 0.01);
                    hit.getWorld().playSound(hit, Sound.ENTITY_GENERIC_EXPLODE, 0.58f, 1.45f);
                }
                cancel();
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }

    private void teleportBehindTarget(Player caster, LivingEntity target) {
        Vector awayFromCaster = target.getLocation().toVector().subtract(caster.getLocation().toVector()).setY(0.0);
        if (awayFromCaster.lengthSquared() <= 0.0001) {
            awayFromCaster = target.getLocation().getDirection().setY(0.0);
        }
        if (awayFromCaster.lengthSquared() <= 0.0001) {
            awayFromCaster = new Vector(0.0, 0.0, 1.0);
        }

        Location destination = target.getLocation().clone()
                .subtract(awayFromCaster.normalize().multiply(1.35))
                .add(0.0, 0.1, 0.0);
        destination.setYaw(target.getLocation().getYaw());
        destination.setPitch(caster.getLocation().getPitch());

        caster.teleport(destination);
        caster.setVelocity(new Vector(0.0, 0.08, 0.0));
    }
}
