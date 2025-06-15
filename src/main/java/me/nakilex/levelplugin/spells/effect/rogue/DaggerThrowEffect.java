package me.nakilex.levelplugin.spells.effect.rogue;

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
import org.bukkit.util.RayTraceResult;

/**
 * Quick ranged attack that throws a virtual dagger forward.
 */
public class DaggerThrowEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        double range = 12.0;
        Object r = ctx.getExtraParam("range");
        if (r instanceof Number num) range += num.doubleValue();

        World world = player.getWorld();
        RayTraceResult res = world.rayTraceEntities(
                player.getEyeLocation(),
                player.getLocation().getDirection(),
                range,
                0.2,
                entity -> entity instanceof LivingEntity && !entity.equals(player)
        );

        world.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1f, 1.4f);

        if (res != null && res.getHitEntity() instanceof LivingEntity target) {
            if (target instanceof Player p && !DuelManager.getInstance().areInDuel(player.getUniqueId(), p.getUniqueId())) {
                return;
            }
            world.spawnParticle(Particle.CRIT, target.getLocation().add(0,1,0), 15, 0.3, 0.3, 0.3, 0.05);
            SpellUtils.dealWithChat(player, target, ctx.getFinalDamage(), "Dagger Throw");
        } else {
            Location end = player.getEyeLocation().add(player.getLocation().getDirection().multiply(range));
            world.spawnParticle(Particle.CRIT, end, 10, 0.2, 0.2, 0.2, 0.02);
        }
    }
}
