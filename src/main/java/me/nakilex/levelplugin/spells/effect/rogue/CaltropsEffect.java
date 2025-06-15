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
 * Throws caltrops that linger on the ground and harm foes who step on them.
 */
public class CaltropsEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        World world = player.getWorld();
        double damage = ctx.getFinalDamage() / 2.0;
        int count = 4;
        world.playSound(player.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1f, 1f);

        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI / count) * i;
            Vector off = new Vector(Math.cos(angle), 0, Math.sin(angle)).multiply(1.5);
            ArmorStand trap = world.spawn(player.getLocation().add(off).add(0,0.1,0), ArmorStand.class);
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
                        SpellUtils.dealWithChat(player, le, damage, "Caltrops");
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 1, false, false));
                        trap.remove();
                        cancel();
                        return;
                    }
                }
            }.runTaskTimer(Main.getInstance(), 0L, 5L);
        }
    }
}
