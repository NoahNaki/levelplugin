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

    public BlackholeSpell(Main plugin, double pullRadius, double dotRadius,
                          double pullStrength, double tickDamage,
                          int durationTicks, double collapseDamage) {
        this.plugin = plugin;
        this.pullRadius = pullRadius;
        this.dotRadius = dotRadius;
        this.pullStrength = pullStrength;
        this.tickDamage = tickDamage;
        this.durationTicks = durationTicks;
        this.collapseDamage = collapseDamage;
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        Location center = SpellTargetingUtil.resolveTargetGround(caster, 28);
        if (center == null) {
            ChatMessageUtil.send(caster, ChatMessageUtil.MessageType.WARNING, "No target location for Blackhole.");
            return;
        }
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
}
