package me.nakilex.levelplugin.spells.effect.rogue;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Grants temporary invisibility and speed.
 */
public class NightCloakEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        int duration = 120; // 6s
        Object bonus = ctx.getExtraParam("durationBonus");
        if (bonus instanceof Number n) duration += n.intValue();

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITCH_DRINK, 1f, 0.8f);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, duration, 0, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 1, false, false));

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks++ >= duration || !player.isOnline()) { cancel(); return; }
                player.getWorld().spawnParticle(Particle.SQUID_INK, player.getLocation(), 2, 0.2,0.2,0.2,0.01);
            }
        }.runTaskTimer(Main.getInstance(), 0L, 5L);
    }
}
