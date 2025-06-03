package me.nakilex.levelplugin.spells.effect.warrior;

import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.bukkit.Location;

public class UppercutEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        double damage = ctx.getFinalDamage();
        double range = 4.0;
        double knockup = 1.5;
        double knockback = 0.5;

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.2f);
        Location eye = player.getEyeLocation();
        Vector dir = player.getLocation().getDirection().normalize();
        for (double i = 0; i <= range; i += 0.5) {
            Location slash = eye.clone().add(dir.clone().multiply(i));
            player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, slash, 1, 0.1, 0.2, 0.1, 0.01);
        }

        for (org.bukkit.entity.Entity e : player.getWorld().getNearbyEntities(player.getLocation(), range, range, range)) {
            if (e instanceof LivingEntity target && !target.equals(player)) {
                if (target instanceof Player p && !DuelManager.getInstance().areInDuel(player.getUniqueId(), p.getUniqueId()))
                    continue;
                Vector toTarget = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
                if (Math.toDegrees(dir.angle(toTarget)) > 45) continue;

                SpellUtils.dealWithChat(player, target, damage, "Uppercut");
                Vector vel = new Vector(0, knockup, 0).add(dir.clone().multiply(knockback));
                target.setVelocity(vel);
                target.getWorld().spawnParticle(Particle.CRIT, target.getLocation(), 10, 0.2, 0.2, 0.2, 0.1);
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1f);
            }
        }
    }
}
