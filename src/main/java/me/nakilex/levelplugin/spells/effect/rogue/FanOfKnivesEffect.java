package me.nakilex.levelplugin.spells.effect.rogue;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Throws a cone of knives in front of the caster.
 */
public class FanOfKnivesEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        World world = player.getWorld();
        int knives = 5;
        double damage = ctx.getFinalDamage();
        world.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1f, 1.2f);

        for (int i = 0; i < knives; i++) {
            double angle = Math.toRadians(-20 + i * (40.0 / (knives - 1)));
            Vector dir = rotateY(player.getLocation().getDirection().clone(), angle).multiply(1.4);
            Snowball sb = player.launchProjectile(Snowball.class);
            sb.setItem(new ItemStack(Material.IRON_SWORD));
            sb.setVelocity(dir);
            sb.setGravity(true);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!sb.isValid() || sb.isDead()) { cancel(); return; }
                    world.spawnParticle(Particle.CRIT, sb.getLocation(), 2, 0.1,0.1,0.1);
                    for (Entity e : sb.getNearbyEntities(0.5,0.5,0.5)) {
                        if (!(e instanceof LivingEntity le) || le.equals(player)) continue;
                        if (le instanceof Player p && !DuelManager.getInstance().areInDuel(player.getUniqueId(), p.getUniqueId()))
                            continue;
                        SpellUtils.dealWithChat(player, le, damage, "Fan of Knives");
                        sb.remove();
                        cancel();
                        return;
                    }
                }
            }.runTaskTimer(Main.getInstance(), 0L, 1L);
        }
    }

    private Vector rotateY(Vector v, double rad) {
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double x = v.getX() * cos - v.getZ() * sin;
        double z = v.getX() * sin + v.getZ() * cos;
        v.setX(x);
        v.setZ(z);
        return v;
    }
}
