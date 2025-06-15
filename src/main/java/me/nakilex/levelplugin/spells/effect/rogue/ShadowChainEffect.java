package me.nakilex.levelplugin.spells.effect.rogue;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Launches a shadowy chain forward that pulls struck foes.
 */
public class ShadowChainEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        double range = 10.0;
        Object r = ctx.getExtraParam("range");
        if (r instanceof Number num) range += num.doubleValue();

        Vector dir = player.getLocation().getDirection().normalize();
        World world = player.getWorld();
        Location start = player.getEyeLocation().add(dir.multiply(0.5));

        world.playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1f, 0.8f);

        new BukkitRunnable() {
            Location loc = start.clone();
            double travelled = 0;
            @Override
            public void run() {
                if (travelled >= range) { cancel(); return; }
                loc.add(dir.clone().multiply(0.6));
                world.spawnParticle(Particle.SMOKE_NORMAL, loc, 2, 0, 0, 0, 0.01);
                for (LivingEntity hit : loc.getNearbyLivingEntities(0.5)) {
                    if (hit.equals(player)) continue;
                    if (hit instanceof Player p && !DuelManager.getInstance().areInDuel(player.getUniqueId(), p.getUniqueId()))
                        continue;
                    SpellUtils.dealWithChat(player, hit, ctx.getFinalDamage(), "Shadow Chain");
                    hit.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 1));
                    Vector pull = player.getLocation().toVector().subtract(hit.getLocation().toVector()).normalize().multiply(0.5);
                    hit.setVelocity(hit.getVelocity().add(pull));
                    world.playSound(hit.getLocation(), Sound.CHAIN_BREAK, 1f, 1f);
                    cancel();
                    return;
                }
                if (loc.getBlock().getType().isSolid()) { cancel(); return; }
                travelled += 0.6;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }
}
