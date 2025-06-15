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

import java.util.HashSet;
import java.util.Set;

/**
 * Conjure spinning daggers that swirl around the rogue, striking nearby foes.
 */
public class UmbralDaggersEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        double damage = ctx.getFinalDamage();
        World world = player.getWorld();

        Location center = player.getLocation().add(0, 1, 0);
        int duration = 20;
        double maxRadius = 3.0;

        world.playSound(center, Sound.ENTITY_ENDER_EYE_LAUNCH, 1f, 0.8f);

        MagicEffects.helix(center.clone(), Particle.CRIT, 1.2, 1.5, 3, duration);

        Set<LivingEntity> hit = new HashSet<>();
        new SpellAnimation(2, duration) {
            @Override
            protected void onTick(int tick) {
                double progress = (double) tick / duration;
                double radius = 1 + (maxRadius - 1) * progress;
                MagicEffects.circle(world, center.clone(), Particle.SWEEP_ATTACK, radius, 16);
                for (LivingEntity le : world.getNearbyLivingEntities(center, radius, 1, radius)) {
                    if (le.equals(player) || hit.contains(le)) continue;
                    if (le instanceof Player p && !DuelManager.getInstance().areInDuel(player.getUniqueId(), p.getUniqueId()))
                        continue;
                    hit.add(le);
                    SpellUtils.dealWithChat(player, le, damage, "Umbral Daggers");
                }
            }
        };
    }
}
