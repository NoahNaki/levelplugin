package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellEffectUtil;
import me.nakilex.levelplugin.spells.SpellHandler;
import me.nakilex.levelplugin.utils.PotionEffectUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class ArcherWindGuardSpell implements SpellHandler {
    private final int durationTicks;
    private final double pulseRadius;
    private final int pulseStunTicks;

    public ArcherWindGuardSpell(int durationTicks,
                                double pulseRadius,
                                int pulseStunTicks) {
        this.durationTicks = Math.max(40, durationTicks);
        this.pulseRadius = Math.max(1.0, pulseRadius);
        this.pulseStunTicks = Math.max(1, pulseStunTicks);
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        PotionEffectUtil.applyHiddenEffect(caster, PotionEffectType.RESISTANCE, durationTicks, 0);
        PotionEffectUtil.applyHiddenEffect(caster, PotionEffectType.SPEED, durationTicks, 1);
        Location center = caster.getLocation().clone().add(0.0, 1.0, 0.0);
        center.getWorld().spawnParticle(Particle.CLOUD, center, 34, 0.6, 0.4, 0.6, 0.03);
        center.getWorld().spawnParticle(Particle.CRIT, center, 18, 0.45, 0.3, 0.45, 0.02);
        center.getWorld().playSound(center, Sound.ITEM_TRIDENT_RIPTIDE_1, 0.7f, 1.4f);

        for (var target : SpellEffectUtil.getLivingTargets(center, pulseRadius,
                living -> !living.equals(caster))) {
            SpellEffectUtil.applyStun(target, pulseStunTicks, true);
            target.setVelocity(target.getLocation().toVector().subtract(caster.getLocation().toVector())
                    .normalize().multiply(0.45).setY(0.2));
        }
    }
}
