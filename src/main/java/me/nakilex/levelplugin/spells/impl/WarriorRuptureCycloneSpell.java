package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellHandler;
import me.nakilex.levelplugin.spells.WarriorCombatUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;

public class WarriorRuptureCycloneSpell implements SpellHandler {
    private final Main plugin;
    private final int pulseCount;
    private final long pulseIntervalTicks;
    private final double baseRadius;
    private final double radiusStep;
    private final double baseDamage;
    private final double damageStep;
    private final double knockback;

    public WarriorRuptureCycloneSpell(Main plugin,
                                      int pulseCount,
                                      long pulseIntervalTicks,
                                      double baseRadius,
                                      double radiusStep,
                                      double baseDamage,
                                      double damageStep,
                                      double knockback) {
        this.plugin = plugin;
        this.pulseCount = Math.max(1, pulseCount);
        this.pulseIntervalTicks = Math.max(1L, pulseIntervalTicks);
        this.baseRadius = Math.max(1.2, baseRadius);
        this.radiusStep = Math.max(0.0, radiusStep);
        this.baseDamage = Math.max(0.1, baseDamage);
        this.damageStep = Math.max(0.0, damageStep);
        this.knockback = Math.max(0.05, knockback);
    }

    @Override
    public void cast(SpellContext context) {
        Location center = context.player().getLocation().clone().add(0.0, 1.0, 0.0);
        center.getWorld().spawnParticle(Particle.SWEEP_ATTACK, center, 3, 0.25, 0.25, 0.25, 0.0);
        center.getWorld().spawnParticle(Particle.CRIT, center, 20, 0.35, 0.45, 0.35, 0.03);
        center.getWorld().playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 0.85f);
        center.getWorld().playSound(center, Sound.ITEM_TRIDENT_RIPTIDE_1, 0.55f, 1.45f);
        WarriorCombatUtil.runRadialPulse(plugin, context.player(), center,
                pulseCount, pulseIntervalTicks, baseRadius, radiusStep, baseDamage, damageStep);
    }
}
