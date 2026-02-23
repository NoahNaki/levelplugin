package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellEffectUtil;
import me.nakilex.levelplugin.spells.SpellHandler;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class BlinkSpell implements SpellHandler {
    private final Main plugin;
    private final double range;
    private final boolean trailDamage;
    private final boolean defensiveBuff;

    public BlinkSpell(Main plugin, double range, boolean trailDamage, boolean defensiveBuff) {
        this.plugin = plugin;
        this.range = range;
        this.trailDamage = trailDamage;
        this.defensiveBuff = defensiveBuff;
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        Location from = caster.getLocation().clone();
        Location to = resolveDestination(caster, from);
        caster.getWorld().spawnParticle(Particle.PORTAL, from.add(0, 1.0, 0), 20, 0.4, 0.3, 0.4, 0.25);
        caster.teleport(to);
        caster.getWorld().playSound(to, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.2f);
        caster.getWorld().spawnParticle(Particle.END_ROD, to.clone().add(0, 1.0, 0), 30, 0.35, 0.45, 0.35, 0.02);

        if (trailDamage) {
            Vector step = to.toVector().subtract(from.toVector()).multiply(1.0 / 6.0);
            Location sample = from.clone().add(0, 1.0, 0);
            for (int i = 0; i < 6; i++) {
                SpellEffectUtil.applyAreaDamage(caster, sample, 1.25, 2.5);
                sample.add(step);
            }
        }
        if (defensiveBuff) {
            caster.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 80, 1, true, true, true));
            caster.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 50, 0, true, true, true));
        }
    }

    private Location resolveDestination(Player caster, Location from) {
        RayTraceResult trace = caster.rayTraceBlocks(range);
        if (trace != null && trace.getHitPosition() != null) {
            return trace.getHitPosition().toLocation(caster.getWorld()).add(0, 1.0, 0);
        }
        return from.add(caster.getEyeLocation().getDirection().normalize().multiply(range));
    }
}
