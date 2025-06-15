package me.nakilex.levelplugin.spells.effect.rogue;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Places small traps around the caster that slow and damage enemies.
 */
public class TricksterTrapEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        World world = player.getWorld();
        double damage = ctx.getFinalDamage() / 2.0;
        int count = 3;
        boolean explode = Boolean.TRUE.equals(ctx.getExtraParam("explode"));
        world.playSound(player.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1f, 1f);

        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI / count) * i;
            Vector off = new Vector(Math.cos(angle), 0, Math.sin(angle)).multiply(1.5);
            Location spawn = player.getLocation().add(off).add(0,0.1,0);
            ArmorStand trap = world.spawn(spawn, ArmorStand.class);
            trap.setInvisible(true);
            trap.setMarker(true);
            trap.setGravity(false);
            trap.getEquipment().setHelmet(new ItemStack(Material.TRIPWIRE_HOOK));

            new BukkitRunnable() {
                int life = 0;
                @Override
                public void run() {
                    if (life++ >= 200 || !trap.isValid()) { trap.remove(); cancel(); return; }
                    world.spawnParticle(Particle.CRIT, trap.getLocation(), 1, 0.1,0.1,0.1);
                    for (Entity e : trap.getNearbyEntities(0.5,0.5,0.5)) {
                        if (!(e instanceof LivingEntity le) || le.equals(player)) continue;
                        if (le instanceof Player p && !DuelManager.getInstance().areInDuel(player.getUniqueId(), p.getUniqueId()))
                            continue;
                        SpellUtils.dealWithChat(player, le, damage, "Trickster Trap");
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 1, false, false));
                        if (explode) {
                            world.spawnParticle(Particle.EXPLOSION_NORMAL, trap.getLocation(), 10, 0.2,0.2,0.2);
                            world.createExplosion(trap.getLocation(), 0f);
                        }
                        trap.remove();
                        cancel();
                        return;
                    }
                }
            }.runTaskTimer(Main.getInstance(), 0L, 5L);
        }
    }
}
