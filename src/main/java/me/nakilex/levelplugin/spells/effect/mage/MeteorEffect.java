package me.nakilex.levelplugin.spells.effect.mage;

import de.slikey.effectlib.effect.HelixEffect;
import de.slikey.effectlib.effect.SphereEffect;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
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
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * Base MeteorEffect: spawns magma meteors, handles damage and AOE.
 * No frost or material overrides.
 */
public class MeteorEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        Main plugin = Main.getInstance();
        World world = player.getWorld();

        // 1) Compute raw damage based on intelligence + weapon
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        int playerInt = ps.baseIntelligence + ps.bonusIntelligence;
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        CustomItem cItem = ItemManager.getInstance().getCustomItemFromItemStack(mainHand);
        int weaponInt = (cItem != null) ? cItem.getIntel() : 0;
        double rawDamage = 6.0 + (playerInt + weaponInt);

        // 2) Rune-driven damage multiplier
        double damageMultiplier = ctx.getFinalDamage() / ctx.getBaseSpell().getBaseDamage();
        double finalDamage = rawDamage * damageMultiplier;

        // 3) Extra projectiles
        int extraProj = 0;
        Object extra = ctx.getExtraParam("extraProjectiles");
        if (extra instanceof Number) {
            extraProj = ((Number) extra).intValue();
        }

        // 4) Impact location
        Location impact = getImpactLocation(player);

        // 5) Spawn position
        Vector dir = player.getEyeLocation().getDirection().normalize();
        Vector up = new Vector(0, 1, 0);
        Vector right = up.clone().crossProduct(dir).normalize();
        Location spawn = impact.clone().add(up.multiply(30)).add(right.multiply(-18));

        // 6) Launch sound
        world.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1f);

        // 7) Sphere offsets
        List<Vector> offsets = getSphereOffsets(0.5, 8);

        // 8) Spawn meteors
        // 8) Spawn armor stands as meteors
        List<ArmorStand> stands = new ArrayList<>();
        for (int round = 0; round < 1 + extraProj; round++) {
            for (Vector off : offsets) {
                ArmorStand as = world.spawn(spawn.clone().add(off), ArmorStand.class, stand -> {
                    stand.setInvisible(true);
                    stand.setMarker(true);
                    stand.setGravity(false);
                    // ← read projectileMaterial (defaults to MAGMA_BLOCK)
                    Material helmMat = Material.MAGMA_BLOCK;
                    Object matParam = ctx.getExtraParam("projectileMaterial");
                    if (matParam instanceof String) {
                        try {
                            helmMat = Material.valueOf((String) matParam);
                        } catch (IllegalArgumentException ignored) {}
                    }
                    stand.getEquipment().setHelmet(new ItemStack(helmMat));
                    stand.setMetadata("Meteor", new FixedMetadataValue(plugin, true));
                });
                stands.add(as);
            }
        }


        // 9) Animate & impact
        new BukkitRunnable() {
            final Vector step = impact.toVector().subtract(spawn.toVector()).normalize().multiply(2.2);
            Location loc = spawn.clone();
            int ticks = 0;

            @Override
            public void run() {
                ticks++;
                loc.add(step);

                // Flame trail
                world.spawnParticle(Particle.FLAME, loc, 8, 0.2, 0.2, 0.2, 0.01);

                // Rotate meteors
                double spin = ticks * 0.1;
                Vector axis = step.clone().normalize();
                for (int i = 0; i < stands.size(); i++) {
                    ArmorStand as = stands.get(i);
                    Vector base = offsets.get(i % offsets.size());
                    Vector rotated = rotateAroundAxis(base, axis, spin);
                    as.teleport(loc.clone().add(rotated));
                    as.setHeadPose(new EulerAngle(spin, spin, 0));
                }

                // Helix effects
                for (int sign : new int[]{1, -1}) {
                    HelixEffect helix = new HelixEffect(plugin.getEffectManager());
                    helix.setLocation(loc);
                    helix.particle = Particle.FLAME;
                    helix.strands = 1;
                    helix.particles = 1;
                    helix.radius = 0.1f;
                    helix.curve = 1.0f;
                    helix.rotation = sign * spin * 0.3;
                    helix.iterations = 1;
                    helix.period = 1;
                    helix.start();
                }

                world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 0.4f, 1f);

                // Collision check (skip our own armor stands)
                for (Entity e : world.getNearbyEntities(loc, 1.2, 1.2, 1.2)) {
                    if (e instanceof ArmorStand) continue;
                    if (!(e instanceof LivingEntity le) || le == player) continue;
                    if (le instanceof Player p
                        && !DuelManager.getInstance().areInDuel(player.getUniqueId(), p.getUniqueId())) continue;
                    impactNow(player, loc, stands, finalDamage);
                    cancel();
                    return;
                }

                // Ground impact
                if (loc.distanceSquared(impact) < 1.0) {
                    impactNow(player, impact, stands, finalDamage);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void impactNow(Player player, Location center, List<ArmorStand> stands, double damage) {
        // Remove stands
        stands.forEach(ArmorStand::remove);
        stands.clear();

        // Shockwave
        SphereEffect shock = new SphereEffect(Main.getInstance().getEffectManager());
        shock.setLocation(center);
        shock.particle = Particle.EXPLOSION;
        shock.particles = 20;
        float radius = 3.0f;
        shock.radius = radius;
        shock.iterations = 5;
        shock.period = 1;
        shock.yOffset = 0.0;
        shock.start();

        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);

        // Deal damage
        for (Entity e : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (!(e instanceof LivingEntity le)) continue;
            if (le instanceof Player p
                && !DuelManager.getInstance().areInDuel(player.getUniqueId(), p.getUniqueId())) continue;
            SpellUtils.dealWithChat(player, le, damage, "Meteor");
        }
    }

    private Location getImpactLocation(Player player) {
        World world = player.getWorld();

        // 1) figure out where they clicked / looked
        Block targetBlock = player.getTargetBlockExact(20);
        double x, z;
        if (targetBlock != null) {
            x = targetBlock.getX() + 0.5;
            z = targetBlock.getZ() + 0.5;
        } else {
            Location eye = player.getEyeLocation();
            Vector dir = eye.getDirection().normalize().multiply(20);
            x = eye.getX() + dir.getX();
            z = eye.getZ() + dir.getZ();
        }

        // 2) snap Y to the top of the terrain at that X,Z
        int groundY = world.getHighestBlockYAt(
            // getHighestBlockYAt takes block‐coordinates
            fastFloor(x),
            fastFloor(z)
        );
        // +0.5 so the explosion is centered in the airspace just above the block
        return new Location(world, x, groundY + 0.5, z);
    }

    // helper to convert world‐coords to block‐coords without rounding up prematurely
    private int fastFloor(double val) {
        return (int)Math.floor(val);
    }


    private Vector rotateAroundAxis(Vector v, Vector axis, double theta) {
        axis = axis.clone().normalize();
        double cos = Math.cos(theta);
        double sin = Math.sin(theta);
        double dot = v.dot(axis);
        Vector term1 = v.clone().multiply(cos);
        Vector term2 = axis.clone().multiply(dot * (1 - cos));
        Vector term3 = axis.clone().crossProduct(v).multiply(sin);
        return term1.add(term2).add(term3);
    }

    private List<Vector> getSphereOffsets(double radius, int max) {
        List<Vector> list = new ArrayList<>();
        for (int i = 0; i < max; i++) {
            double theta = Math.acos(2 * Math.random() - 1);
            double phi = 2 * Math.PI * Math.random();
            double x = radius * Math.sin(theta) * Math.cos(phi);
            double y = radius * Math.sin(theta) * Math.sin(phi);
            double z = radius * Math.cos(theta);
            list.add(new Vector(x, y, z));
        }
        return list;
    }
}
