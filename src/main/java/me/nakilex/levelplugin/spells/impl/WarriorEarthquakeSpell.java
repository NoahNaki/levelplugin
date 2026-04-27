package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellEffectUtil;
import me.nakilex.levelplugin.spells.SpellHandler;
import me.nakilex.levelplugin.spells.WarriorCombatUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class WarriorEarthquakeSpell implements SpellHandler {
    private final Main plugin;
    private final double radius;
    private final double damage;
    private final double knockback;

    public WarriorEarthquakeSpell(Main plugin, double radius, double damage, double knockback) {
        this.plugin = plugin;
        this.radius = Math.max(1.0, radius);
        this.damage = Math.max(0.0, damage);
        this.knockback = Math.max(0.0, knockback);
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        Location center = caster.getLocation().clone();
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        SpellEffectUtil.spawnRingParticles(center, radius, Particle.CRIT, 48, 0.02);
        world.spawnParticle(Particle.CLOUD, center, 26, radius * 0.25, 0.15, radius * 0.25, 0.02);
        WarriorCombatUtil.spawnGroundRipple(plugin, world, center, radius);
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.65f, 0.75f);
        for (LivingEntity target : SpellEffectUtil.getLivingTargets(center, radius, living -> !living.equals(caster))) {
            SpellEffectUtil.applyDirectSpellDamage(plugin, caster, target, damage, true);
            Vector away = target.getLocation().toVector().subtract(center.toVector());
            away.setY(0.0);
            if (away.lengthSquared() > 0.0001 && knockback > 0.0) {
                target.setVelocity(target.getVelocity().multiply(0.6).add(away.normalize().multiply(knockback)).setY(0.24));
            }
        }
    }
}
