package me.nakilex.levelplugin.spells.effect.warrior;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.utils.animation.SpellAnimation;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * A battle cry that buffs nearby allies with strength and speed.
 */
public class WarCryEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        double radius = 6.0;
        int duration = 200; // 10 seconds

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 0.8f);
        player.getWorld().spawnParticle(Particle.SONIC_BOOM, player.getLocation().add(0,1,0), 1);

        new SpellAnimation(2, 20) {
            @Override
            protected void onTick(int tick) {
                player.getWorld().spawnParticle(Particle.NOTE, player.getLocation().add(0,1,0), 4, 0.5,0.5,0.5, 0.2);
                player.getWorld().playSound(player.getLocation(), Sound.UI_TOAST_IN, 0.5f, 1.5f);
            }
        };

        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof Player p) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duration, 1));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 0));
            }
        }
        // buff caster too
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duration, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 0));
    }
}
