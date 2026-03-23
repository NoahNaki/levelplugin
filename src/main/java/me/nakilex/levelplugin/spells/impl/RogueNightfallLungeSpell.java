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

import java.util.HashSet;
import java.util.Set;

public class RogueNightfallLungeSpell implements SpellHandler {
    private final Main plugin;

    public RogueNightfallLungeSpell(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        LivingEntity firstTarget = SpellTargetingUtil.resolveTargetLivingEntity(caster, 16.0, 0.45,
                living -> !living.equals(caster));
        if (firstTarget == null) {
            ChatMessageUtil.send(caster, ChatMessageUtil.MessageType.WARNING,
                    "Look at a target mob for Nightfall Lunge.");
            return;
        }

        Set<java.util.UUID> struckTargets = new HashSet<>();

        new BukkitRunnable() {
            private LivingEntity currentTarget = firstTarget;
            private int jumps;

            @Override
            public void run() {
                if (!caster.isOnline() || currentTarget == null || !currentTarget.isValid() || currentTarget.isDead() || jumps >= 4) {
                    cancel();
                    return;
                }

                strikeTarget(caster, currentTarget, jumps);
                struckTargets.add(currentTarget.getUniqueId());
                jumps++;

                LivingEntity nextTarget = findNearestUnstruckTarget(currentTarget.getLocation(), caster, struckTargets, 8.0);
                if (nextTarget == null) {
                    cancel();
                    return;
                }
                currentTarget = nextTarget;
            }
        }.runTaskTimer(plugin, 0L, 6L);
    }

    private void strikeTarget(Player caster, LivingEntity target, int jumpIndex) {
        Location behind = computeBehindLocation(caster, target);
        caster.teleport(behind);
        caster.setVelocity(new Vector(0.0, 0.06, 0.0));

        Location hit = target.getLocation().clone().add(0.0, 1.0, 0.0);
        SpellEffectUtil.applyDirectSpellDamage(plugin, caster, target, 7.6 + (jumpIndex * 1.2), true);
        SpellEffectUtil.spawnRingParticles(hit, 0.7, Particle.CRIT, 14, 0.0);
        hit.getWorld().playSound(hit, Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.85f, 1.0f + (jumpIndex * 0.08f));
    }

    private LivingEntity findNearestUnstruckTarget(Location from,
                                                    Player caster,
                                                    Set<java.util.UUID> struckTargets,
                                                    double radius) {
        LivingEntity closest = null;
        double bestDistanceSquared = Double.MAX_VALUE;
        for (LivingEntity candidate : SpellEffectUtil.getLivingTargets(from, radius,
                living -> !living.equals(caster) && !struckTargets.contains(living.getUniqueId()))) {
            double distanceSquared = candidate.getLocation().distanceSquared(from);
            if (distanceSquared < bestDistanceSquared) {
                bestDistanceSquared = distanceSquared;
                closest = candidate;
            }
        }
        return closest;
    }

    private Location computeBehindLocation(Player caster, LivingEntity target) {
        Vector toCaster = caster.getLocation().toVector().subtract(target.getLocation().toVector()).setY(0.0);
        if (toCaster.lengthSquared() <= 0.0001) {
            toCaster = target.getLocation().getDirection().setY(0.0);
        }
        if (toCaster.lengthSquared() <= 0.0001) {
            toCaster = new Vector(0.0, 0.0, 1.0);
        }

        Location destination = target.getLocation().clone().add(toCaster.normalize().multiply(1.35)).add(0.0, 0.05, 0.0);
        destination.setPitch(caster.getLocation().getPitch());
        return destination;
    }
}
