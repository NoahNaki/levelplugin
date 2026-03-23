package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellHandler;
import me.nakilex.levelplugin.utils.PotionEffectUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class ArcherVeilStepSpell implements SpellHandler {
    private final int invisibilityTicks;
    private final int speedTicks;
    private final int absorptionTicks;

    public ArcherVeilStepSpell(int invisibilityTicks,
                               int speedTicks,
                               int absorptionTicks) {
        this.invisibilityTicks = Math.max(20, invisibilityTicks);
        this.speedTicks = Math.max(20, speedTicks);
        this.absorptionTicks = Math.max(20, absorptionTicks);
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        Vector backward = caster.getLocation().getDirection().clone().setY(0.0);
        if (backward.lengthSquared() <= 0.000001) {
            backward = new Vector(0.0, 0.0, 1.0);
        }
        backward.normalize().multiply(-0.75).setY(0.18);
        caster.setVelocity(caster.getVelocity().multiply(0.2).add(backward));

        PotionEffectUtil.applyHiddenEffect(caster, PotionEffectType.INVISIBILITY, invisibilityTicks, 0);
        PotionEffectUtil.applyHiddenEffect(caster, PotionEffectType.SPEED, speedTicks, 1);
        PotionEffectUtil.applyHiddenEffect(caster, PotionEffectType.ABSORPTION, absorptionTicks, 1);

        Location center = caster.getLocation().clone().add(0.0, 1.0, 0.0);
        center.getWorld().spawnParticle(Particle.CLOUD, center, 30, 0.45, 0.2, 0.45, 0.02);
        center.getWorld().spawnParticle(Particle.CRIT, center, 14, 0.3, 0.15, 0.3, 0.02);
        center.getWorld().playSound(center, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.7f, 1.35f);
    }
}
