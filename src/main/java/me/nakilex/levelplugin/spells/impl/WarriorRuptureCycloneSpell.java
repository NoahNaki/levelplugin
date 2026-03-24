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
        Location center = context.player().getLocation().clone().add(0.0, 0.1, 0.0);
        center.getWorld().spawnParticle(Particle.EXPLOSION, center, 1);
        center.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.55f, 1.65f);
        WarriorCombatUtil.runShockwaveRipple(plugin, context.player(), center,
                pulseCount, pulseIntervalTicks, baseRadius, radiusStep, baseDamage, damageStep, knockback);
    }
}
