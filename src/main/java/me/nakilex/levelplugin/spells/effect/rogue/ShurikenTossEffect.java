package me.nakilex.levelplugin.spells.effect.rogue;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * Launches a spinning shuriken made from invisible armor stands.
 * Supports "teleport" and "applyPoison" transform params and
 * "extraProjectiles" for additional shurikens.
 */
public class ShurikenTossEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        World world = player.getWorld();
        double damage = ctx.getFinalDamage();

        int extra = parseInt(ctx.getExtraParam("extraProjectiles"), 0);
        boolean teleport = parseBoolean(ctx.getExtraParam("teleport"), false);
        boolean poison = parseBoolean(ctx.getExtraParam("applyPoison"), false);

        int total = 1 + extra;
        for (int i = 0; i < total; i++) {
            float yawOffset = (360f / total) * i;
            launchShuriken(player, world, damage, teleport, poison, yawOffset);
        }

        world.playSound(player.getLocation(), Sound.ENTITY_ENDER_PEARL_THROW, 1f, 1f);
    }

    private void launchShuriken(Player player, World world, double damage,
                                boolean teleport, boolean poison, float yawOffset) {
        Location center = player.getLocation().clone();
        center.setYaw(center.getYaw() + yawOffset);
        Vector forwardVelocity = center.getDirection().multiply(1.3);

        List<ArmorStand> stands = new ArrayList<>();
        double radius = 0.2;
        Vector[] offsets = {
                new Vector(radius, 0, 0), new Vector(-radius, 0, 0),
                new Vector(0, 0, radius), new Vector(0, 0, -radius)
        };

        for (Vector off : offsets) {
            Location loc = center.clone().add(off);
            ArmorStand stand = world.spawn(loc, ArmorStand.class);
            stand.setInvisible(true);
            stand.setMarker(true);
            stand.setBasePlate(false);
            stand.setGravity(false);
            stand.setArms(true);
            stand.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
            stand.setRightArmPose(new EulerAngle(Math.toRadians(270), 0, Math.toRadians(90)));
            stand.setLeftArmPose(new EulerAngle(Math.toRadians(270), 0, Math.toRadians(-90)));
            stands.add(stand);
        }

        new BukkitRunnable() {
            int ticks = 0;
            double angle = 0;
            @Override
            public void run() {
                if (ticks++ >= 60) {
                    removeAll();
                    cancel();
                    return;
                }

                angle += Math.toRadians(45);
                center.add(forwardVelocity);

                for (int i = 0; i < stands.size(); i++) {
                    ArmorStand stand = stands.get(i);
                    Vector off = offsets[i].clone();
                    double cos = Math.cos(angle), sin = Math.sin(angle);
                    double x = off.getX() * cos - off.getZ() * sin;
                    double z = off.getX() * sin + off.getZ() * cos;
                    Location loc = center.clone().add(x, 0, z);

                    // block collision
                    Block block = loc.getBlock();
                    if (block.getType() != Material.AIR && !block.isPassable()) {
                        hit(loc, null);
                        return;
                    }

                    // entity collision
                    for (Entity e : world.getNearbyEntities(loc, 0.3, 0.3, 0.3)) {
                        if (e.equals(player) || stands.contains(e)) continue;
                        if (e instanceof LivingEntity le) {
                            hit(loc, le);
                            return;
                        }
                    }

                    loc.setYaw((float) Math.toDegrees(Math.atan2(-x, z)));
                    stand.teleport(loc);
                }
            }

            private void hit(Location loc, LivingEntity victim) {
                if (victim != null) {
                    SpellUtils.dealWithChat(player, victim, damage, "Shuriken Toss");
                    if (poison) {
                        victim.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 40, 1));
                    }
                }
                world.spawnParticle(Particle.CRIT, loc, 10, 0.3, 0.3, 0.3);
                world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
                if (teleport) player.teleport(loc);
                removeAll();
                cancel();
            }

            private void removeAll() {
                stands.forEach(s -> { if (s.isValid()) s.remove(); });
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }

    private int parseInt(Object param, int def) {
        if (param instanceof Number n) return n.intValue();
        if (param instanceof String s) { try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {} }
        if (param instanceof java.util.List<?> list) {
            int sum = 0; for (Object o : list) if (o instanceof Number n) sum += n.intValue();
            return sum;
        }
        return def;
    }

    private boolean parseBoolean(Object param, boolean def) {
        if (param instanceof Boolean b) return b;
        if (param instanceof String s) return Boolean.parseBoolean(s);
        return def;
    }
}
