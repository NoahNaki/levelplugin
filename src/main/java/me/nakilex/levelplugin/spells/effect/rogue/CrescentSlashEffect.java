package me.nakilex.levelplugin.spells.effect.rogue;

import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import me.nakilex.levelplugin.spells.utils.animation.SpellAnimation;
import me.nakilex.levelplugin.spells.utils.magic.MagicEffects;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

/**
 * Sweep a crescent-shaped slash in front of the rogue, damaging enemies.
 */
public class CrescentSlashEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        World world = player.getWorld();
        double damage = ctx.getFinalDamage();

        Location center = player.getLocation().add(0, 1, 0);
        double yaw = Math.toRadians(player.getLocation().getYaw());
        double range = 4.0;

        world.playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.9f);

        new SpellAnimation(1, 12) {
            @Override
            protected void onTick(int tick) {
                double progress = (double) tick / 12;
                Location loc = center.clone().add(player.getLocation().getDirection().multiply(progress * range));
                MagicEffects.crescent(loc, Particle.SWEEP_ATTACK, 1.5, yaw, 15);
            }
        };

        Set<LivingEntity> hit = new HashSet<>();
        for (LivingEntity le : world.getNearbyLivingEntities(center, range, 1.5, range)) {
            if (le.equals(player)) continue;
            if (le instanceof Player p && !DuelManager.getInstance().areInDuel(player.getUniqueId(), p.getUniqueId()))
                continue;
            Vector to = le.getLocation().toVector().subtract(player.getLocation().toVector());
            double angle = Math.toDegrees(Math.atan2(to.getZ(), to.getX())) - Math.toDegrees(yaw);
            angle = Math.abs((angle + 180) % 360 - 180);
            if (angle <= 60 && to.length() <= range) {
                if (hit.add(le)) {
                    SpellUtils.dealWithChat(player, le, damage, "Crescent Slash");
                }
            }
        }
    }
}
