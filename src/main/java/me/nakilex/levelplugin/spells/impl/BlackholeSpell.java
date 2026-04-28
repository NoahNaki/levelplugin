package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellEffectUtil;
import me.nakilex.levelplugin.spells.SpellHandler;
import me.nakilex.levelplugin.spells.SpellTargetingUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class BlackholeSpell implements SpellHandler {
    private final Main plugin;
    private final double pullRadius;
    private final double dotRadius;
    private final double pullStrength;
    private final double tickDamage;
    private final int durationTicks;
    private final double collapseDamage;
    private final int eventHorizonArcs;
    private final double eventHorizonDamage;
    private final double eventHorizonSpinPerTick;

    public BlackholeSpell(Main plugin, double pullRadius, double dotRadius,
                          double pullStrength, double tickDamage,
                          int durationTicks, double collapseDamage) {
        this(plugin, pullRadius, dotRadius, pullStrength, tickDamage, durationTicks, collapseDamage, 0, 0.0, 0.0);
    }

    public BlackholeSpell(Main plugin, double pullRadius, double dotRadius,
                          double pullStrength, double tickDamage,
                          int durationTicks, double collapseDamage,
                          int eventHorizonArcs, double eventHorizonDamage, double eventHorizonSpinPerTick) {
        this.plugin = plugin;
        this.pullRadius = pullRadius;
        this.dotRadius = dotRadius;
        this.pullStrength = pullStrength;
        this.tickDamage = tickDamage;
        this.durationTicks = durationTicks;
        this.collapseDamage = collapseDamage;
        this.eventHorizonArcs = Math.max(0, eventHorizonArcs);
        this.eventHorizonDamage = Math.max(0.0, eventHorizonDamage);
        this.eventHorizonSpinPerTick = Math.max(0.0, eventHorizonSpinPerTick);
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        Location resolved = SpellTargetingUtil.resolveNearestEnemyGround(caster, 28);
        if (resolved == null) {
            resolved = SpellTargetingUtil.resolveTargetGround(caster, 28);
        }
        if (resolved == null) {
            resolved = caster.getLocation().clone().add(caster.getLocation().getDirection().multiply(7.0));
            resolved.setY(caster.getWorld().getHighestBlockYAt(resolved) + 1.0);
        }
        final Location center = resolved;
        new BukkitRunnable() {
            int elapsed;

            @Override
            public void run() {
                if (!caster.isOnline() || center.getWorld() == null) {
                    cancel();
                    return;
                }
                World world = center.getWorld();
                SpellEffectUtil.spawnRingParticles(center, pullRadius, Particle.WITCH, 48, 0.15);
                SpellEffectUtil.spawnRingParticles(center, dotRadius, Particle.ENCHANT, 28, 0.1);
                world.spawnParticle(Particle.PORTAL, center, 36, dotRadius * 0.4, 0.4, dotRadius * 0.4, 0.25);
                applyEventHorizonArcs(caster, center, elapsed);
                if (elapsed % 10 == 0) {
                    world.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.35f, 0.65f);
                }
                for (LivingEntity target : SpellEffectUtil.getLivingTargets(center, pullRadius, living -> !living.equals(caster))) {
                    Vector pull = center.toVector().subtract(target.getLocation().toVector());
                    if (pull.lengthSquared() > 0.0001) {
                        target.setVelocity(target.getVelocity().multiply(0.7).add(pull.normalize().multiply(pullStrength)));
                    }
                    if (target.getLocation().distanceSquared(center) <= dotRadius * dotRadius) {
                        SpellEffectUtil.applyDirectSpellDamage(plugin, caster, target, tickDamage);
                    }
                }
                elapsed += 5;
                if (elapsed >= durationTicks) {
                    if (collapseDamage > 0.0) {
                        world.spawnParticle(Particle.EXPLOSION, center, 1, 0, 0, 0, 0);
                        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.6f);
                        SpellEffectUtil.applyAreaDamage(caster, center, pullRadius + 0.75, collapseDamage);
                    }
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    private void applyEventHorizonArcs(Player caster, Location center, int elapsed) {
        if (eventHorizonArcs <= 0 || eventHorizonDamage <= 0.0 || center.getWorld() == null) {
            return;
        }
        World world = center.getWorld();
        double baseAngle = elapsed * eventHorizonSpinPerTick;
        for (int i = 0; i < eventHorizonArcs; i++) {
            double angle = baseAngle + ((Math.PI * 2.0 * i) / eventHorizonArcs);
            Location ringPoint = center.clone().add(Math.cos(angle) * pullRadius, 0.35, Math.sin(angle) * pullRadius);
            world.spawnParticle(Particle.END_ROD, ringPoint, 1, 0.0, 0.0, 0.0, 0.0);
            world.spawnParticle(Particle.ELECTRIC_SPARK, ringPoint, 2, 0.03, 0.03, 0.03, 0.0);
            for (LivingEntity target : SpellEffectUtil.getLivingTargets(ringPoint, 1.35, living -> !living.equals(caster))) {
                SpellEffectUtil.applyDirectSpellDamage(plugin, caster, target, eventHorizonDamage);
                Vector inward = center.toVector().subtract(target.getLocation().toVector());
                if (inward.lengthSquared() > 0.0001) {
                    target.setVelocity(target.getVelocity().multiply(0.72).add(inward.normalize().multiply(pullStrength * 0.8)));
                }
            }
        }
    }
}
