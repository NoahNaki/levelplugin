package me.nakilex.levelplugin.spells.effect.rogue;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

/**
 * Throws a phantom blade that returns like a boomerang.
 */
public class PhantomBladeEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        double range = 8.0;
        Object r = ctx.getExtraParam("range");
        if (r instanceof Number num) range += num.doubleValue();

        World world = player.getWorld();
        Vector dir = player.getLocation().getDirection().normalize();
        Location start = player.getEyeLocation().add(dir.multiply(0.5));
        ArmorStand stand = world.spawn(start, ArmorStand.class, a -> {
            a.setInvisible(true);
            a.setGravity(false);
            a.setArms(true);
            a.setSmall(true);
            a.setMarker(true);
            a.setInvulnerable(true);
            a.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
            a.setRotation(player.getLocation().getYaw(), 0f);
            a.setRightArmPose(new EulerAngle(Math.toRadians(-90), 0, 0));
        });

        world.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1f, 1.1f);

        new BukkitRunnable() {
            double travelled = 0;
            double spin = 0;
            boolean returning = false;
            @Override
            public void run() {
                if (!returning && travelled >= range) {
                    returning = true;
                }
                Vector currentDir = returning ?
                        player.getEyeLocation().toVector().subtract(stand.getLocation().toVector()).normalize()
                        : dir;
                Location loc = stand.getLocation().add(currentDir.clone().multiply(1.4));
                stand.teleport(loc);
                spin += Math.toRadians(20);
                stand.setRightArmPose(new EulerAngle(Math.toRadians(-90), spin, 0));
                world.spawnParticle(Particle.CRIT, loc, 2, 0, 0, 0, 0.01);
                if (!returning) travelled += 1.4;

                for (LivingEntity hit : loc.getNearbyLivingEntities(0.4)) {
                    if (hit.equals(player) || hit.equals(stand)) continue;
                    if (hit instanceof Player p && !DuelManager.getInstance().areInDuel(player.getUniqueId(), p.getUniqueId()))
                        continue;
                    world.spawnParticle(Particle.CRIT, hit.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.05);
                    SpellUtils.dealWithChat(player, hit, ctx.getFinalDamage(), "Phantom Blade");
                    returning = true;
                }

                if (loc.getBlock().getType().isSolid()) {
                    returning = true;
                }

                if (returning && loc.distanceSquared(player.getEyeLocation()) < 1.0) {
                    stand.remove();
                    cancel();
                }
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }
}
