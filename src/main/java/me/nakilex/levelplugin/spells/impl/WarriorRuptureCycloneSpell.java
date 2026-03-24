package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellHandler;
import me.nakilex.levelplugin.spells.WarriorCombatUtil;
import org.bukkit.Location;

public class WarriorRuptureCycloneSpell implements SpellHandler {
    private final Main plugin;
    private final int pulseCount;
    private final long pulseIntervalTicks;
    private final double baseRadius;
    private final double radiusStep;
    private final double baseDamage;
    private final double damageStep;

    public WarriorRuptureCycloneSpell(Main plugin,
                                      int pulseCount,
                                      long pulseIntervalTicks,
                                      double baseRadius,
                                      double radiusStep,
                                      double baseDamage,
                                      double damageStep) {
        this.plugin = plugin;
        this.pulseCount = Math.max(1, pulseCount);
        this.pulseIntervalTicks = Math.max(1L, pulseIntervalTicks);
        this.baseRadius = Math.max(1.2, baseRadius);
        this.radiusStep = Math.max(0.0, radiusStep);
        this.baseDamage = Math.max(0.1, baseDamage);
        this.damageStep = Math.max(0.0, damageStep);
    }

    @Override
    public void cast(SpellContext context) {
        Location center = context.player().getLocation().clone();
        WarriorCombatUtil.runRadialPulse(plugin, context.player(), center,
                pulseCount, pulseIntervalTicks, baseRadius, radiusStep, baseDamage, damageStep);
    }
}
