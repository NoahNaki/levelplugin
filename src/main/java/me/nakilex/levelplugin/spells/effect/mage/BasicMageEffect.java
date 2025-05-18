package me.nakilex.levelplugin.spells.effect.mage;

import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class BasicMageEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getCaster();
        World world = player.getWorld();
        double damage = ctx.getBaseDamage() * ctx.getFinalDamageMultiplier();

        Location start = player.getEyeLocation();
        Vector dir = start.getDirection().normalize();

        world.spawnParticle(Particle.END_ROD, start, 5, 0.1, 0.1, 0.1, 0.02);
        int range = 20;

        for (int i = 0; i < range; i++) {
            Location loc = start.clone().add(dir.clone().multiply(i));
            world.spawnParticle(Particle.CRIT, loc, 1, 0, 0, 0, 0);
            for (Entity e : world.getNearbyEntities(loc, 0.5, 0.5, 0.5)) {
                if (!(e instanceof LivingEntity le) || le == player) continue;
                world.spawnParticle(Particle.DAMAGE_INDICATOR, le.getLocation(), 10, 0.2, 0.2, 0.2, 0.02);
                world.playSound(le.getLocation(), Sound.ENTITY_PLAYER_HURT, 1f, 1.5f);
                SpellUtils.dealWithChat(player, le, damage, "Basic Mage Attack");
                return;
            }
            if (loc.getBlock().getType().isSolid()) {
                world.spawnParticle(Particle.SMOKE, loc, 5, 0.2, 0.2, 0.2, 0.05);
                world.playSound(loc, Sound.BLOCK_STONE_HIT, 1f, 0.8f);
                return;
            }
        }
        Location end = start.clone().add(dir.multiply(range));
        world.spawnParticle(Particle.SMOKE, end, 5, 0.2, 0.2, 0.2, 0.05);
    }
}