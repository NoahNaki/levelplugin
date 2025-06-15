package me.nakilex.levelplugin.spells.effect.rogue;

import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import me.nakilex.levelplugin.spells.utils.magic.MagicEffects;
import me.nakilex.levelplugin.spells.utils.animation.SpellAnimation;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

/**
 * Unleash a wave of shadows that expands outward damaging enemies.
 */
public class ShadowNovaEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        double damage = ctx.getFinalDamage();
        double maxRadius = 6.0;
        Location center = player.getLocation().add(0, 1, 0);

        Set<LivingEntity> hit = new HashSet<>();
        new SpellAnimation(2, 20) {
            double radius = 0;
            @Override
            protected void onTick(int tick) {
                radius += maxRadius / 10.0;
                MagicEffects.circle(player.getWorld(), center, Particle.SMOKE_NORMAL, radius, 24);
                for (LivingEntity le : center.getWorld().getNearbyLivingEntities(center, radius, 1, radius)) {
                    if (le.equals(player) || hit.contains(le)) continue;
                    if (le instanceof Player p && !DuelManager.getInstance().areInDuel(player.getUniqueId(), p.getUniqueId()))
                        continue;
                    hit.add(le);
                    SpellUtils.dealWithChat(player, le, damage, "Shadow Nova");
                    Vector knock = le.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(0.4);
                    knock.setY(0.2);
                    le.setVelocity(le.getVelocity().add(knock));
                }
            }
        };
    }
}
