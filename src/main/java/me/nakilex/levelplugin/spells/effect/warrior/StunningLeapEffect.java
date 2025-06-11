package me.nakilex.levelplugin.spells.effect.warrior;

import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.Location;

/**
 * Heroic Leap that also stuns targets on landing.
 */
public class StunningLeapEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        double damage = ctx.getFinalDamage();
        double radius = 5.0;
        int stunTicks = 40;

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 1f);
        Vector leap = player.getLocation().getDirection().normalize().multiply(1.2);
        leap.setY(0.6);
        player.setVelocity(leap);

        new BukkitRunnable() {
            boolean landed = false;
            @Override
            public void run() {
                if (!landed && player.isOnGround()) {
                    landed = true;
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
                    player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 20, 0.5,1,0.5);
                    for (Entity e : player.getWorld().getNearbyEntities(player.getLocation(), radius, radius, radius)) {
                        if (!(e instanceof LivingEntity le) || e.equals(player)) continue;
                        if (le instanceof Player p && !DuelManager.getInstance().areInDuel(player.getUniqueId(), p.getUniqueId()))
                            continue;
                        SpellUtils.dealWithChat(player, le, damage, "Stunning Leap");
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, stunTicks, 2));
                    }
                    cancel();
                }
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 4L, 1L);
    }
}
