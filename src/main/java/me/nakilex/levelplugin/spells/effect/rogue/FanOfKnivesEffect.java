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
 * Throws a spread of spinning daggers in front of the rogue.
 */
public class FanOfKnivesEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        double range = 8.0;
        Object r = ctx.getExtraParam("range");
        if (r instanceof Number num) range += num.doubleValue();

        int count = 5;
        Object c = ctx.getExtraParam("extraDaggers");
        if (c instanceof Number num) count += Math.max(0, num.intValue());

        World world = player.getWorld();
        Vector base = player.getLocation().getDirection().normalize();
        double spread = Math.toRadians(60);

        world.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1f, 1.2f);

        for (int i = 0; i < count; i++) {
            double angle = -spread / 2 + (spread / (count - 1)) * i;
            Vector dir = rotate(base.clone(), angle);
            Location start = player.getEyeLocation().add(dir.clone().multiply(0.5));
            ArmorStand stand = world.spawn(start, ArmorStand.class, a -> {
                a.setInvisible(true);
                a.setGravity(false);
                a.setArms(true);
                a.setSmall(true);
                a.setMarker(true);
                a.setInvulnerable(true);
                a.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
                a.setRotation(player.getLocation().getYaw() + (float)Math.toDegrees(angle), 0f);
                a.setRightArmPose(new EulerAngle(Math.toRadians(-90), 0, 0));
            });

            new BukkitRunnable() {
                double travelled = 0;
                double spin = 0;
                @Override
                public void run() {
                    if (travelled >= range) {
                        stand.remove();
                        cancel();
                        return;
                    }

                    Location loc = stand.getLocation().add(dir.clone().multiply(1.4));
                    stand.teleport(loc);
                    spin += Math.toRadians(20);
                    stand.setRightArmPose(new EulerAngle(Math.toRadians(-90), spin, 0));
                    world.spawnParticle(Particle.CRIT, loc, 2, 0, 0, 0, 0.01);
                    travelled += 1.4;

                    for (LivingEntity hit : loc.getNearbyLivingEntities(0.4)) {
                        if (hit.equals(player) || hit.equals(stand)) continue;
                        if (hit instanceof Player p && !DuelManager.getInstance().areInDuel(player.getUniqueId(), p.getUniqueId()))
                            continue;
                        world.spawnParticle(Particle.CRIT, hit.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.05);
                        SpellUtils.dealWithChat(player, hit, ctx.getFinalDamage(), "Fan of Knives");
                        stand.remove();
                        cancel();
                        return;
                    }

                    if (loc.getBlock().getType().isSolid()) {
                        world.spawnParticle(Particle.CRIT, loc, 10, 0.2, 0.2, 0.2, 0.02);
                        stand.remove();
                        cancel();
                    }
                }
            }.runTaskTimer(Main.getInstance(), 0L, 1L);
        }
    }

    private Vector rotate(Vector vec, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double x = vec.getX() * cos - vec.getZ() * sin;
        double z = vec.getX() * sin + vec.getZ() * cos;
        vec.setX(x);
        vec.setZ(z);
        return vec;
    }
}
