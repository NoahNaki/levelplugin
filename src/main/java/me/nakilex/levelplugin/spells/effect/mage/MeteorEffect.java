package me.nakilex.levelplugin.spells.effect.mage;

import de.slikey.effectlib.effect.HelixEffect;
import de.slikey.effectlib.effect.SphereEffect;
import me.nakilex.levelplugin.epicspells.utils.DirectionalParticleCollection;
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
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * Base MeteorEffect: spawns meteors of a configurable material, handles damage and AOE.
 * Defaults to MAGMA_BLOCK if no projectileMaterial extraParam is set.
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
        } else if (extra instanceof String) {
            try {
                extraProj = Integer.parseInt((String) extra);
            } catch (NumberFormatException ignored) {}
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

        // 7) Sphere offsets using projectileSize
        double projectileSize = 0.5;
        Object sizeParam = ctx.getExtraParam("projectileSize");
        if (sizeParam instanceof Number) {
            projectileSize = ((Number) sizeParam).doubleValue();
        } else if (sizeParam instanceof String) {
            try {
                projectileSize = Double.parseDouble((String) sizeParam);
            } catch (NumberFormatException ignored) {}
        }

        // 7) Generate hollow sphere offsets (thin shell) for better performance
        List<Vector> offsets = new ArrayList<>();
        double spacing = 0.5;
        double radius  = projectileSize;
        double r2      = radius * radius;
        double innerR  = radius - spacing;      // one “layer” inside

        for (double x = -radius; x <= radius; x += spacing) {
            for (double y = -radius; y <= radius; y += spacing) {
                for (double z = -radius; z <= radius; z += spacing) {
                    double d2 = x*x + y*y + z*z;
                    if (d2 <= r2 && d2 >= innerR*innerR) {
                        offsets.add(new Vector(x, y, z));
                    }
                }
            }
        }

        // 8) Spawn meteors with configured material, count each “round” separately
        List<ArmorStand> stands = new ArrayList<>();
        for (int round = 0; round < 1 + extraProj; round++) {
            List<ArmorStand> roundStands = new ArrayList<>();
            for (Vector off : offsets) {
                ArmorStand as = world.spawn(spawn.clone().add(off), ArmorStand.class, stand -> {
                    stand.setInvisible(true);
                    stand.setMarker(true);
                    stand.setGravity(false);

                    // Read projectileMaterial from context, default to MAGMA_BLOCK
                    Object matParam = ctx.getExtraParam("projectileMaterial");
                    Material helmMat = Material.MAGMA_BLOCK;
                    if (matParam instanceof String) {
                        try {
                            helmMat = Material.valueOf((String) matParam);
                        } catch (IllegalArgumentException ignored) {
                            // fallback remains MAGMA_BLOCK
                        }
                    }
                    stand.getEquipment().setHelmet(new ItemStack(helmMat));
                    stand.setMetadata("Meteor", new FixedMetadataValue(plugin, true));
                });
                roundStands.add(as);
            }
            // Track spawned armor stands for cleanup
            stands.addAll(roundStands);
        }

        new BukkitRunnable() {
            final Vector step = impact.toVector()
                .subtract(spawn.toVector())
                .normalize()
                .multiply(2.2);
            Location loc = spawn.clone();
            int ticks = 0;

            @Override
            public void run() {
                ticks++;
                // 1) Move the meteor forward
                loc.add(step);

                // 2) Flame‐trail, spinning blocks, helix, sound… exactly as before
                world.spawnParticle(Particle.FLAME, loc, 8, 0.2, 0.2, 0.2, 0.01);

                // Fireball-style particles from EpicSpells
                List<DirectionalParticleCollection> trail = new ArrayList<>();
                trail.add(new DirectionalParticleCollection(world, Particle.SMALL_FLAME, loc, step, 20, 0.1));
                trail.add(new DirectionalParticleCollection(world, Particle.LARGE_SMOKE, loc, step, 15, 0.1));
                trail.add(new DirectionalParticleCollection(world, Particle.SMOKE, loc, step, 16, 0.1));
                for (DirectionalParticleCollection dpc : trail) {
                    dpc.randomizeLocations(1);
                    dpc.adjustVelocities();
                    dpc.spawn();
                }
                double spin = ticks * 0.1;
                Vector axis = step.clone().normalize();
                for (int i = 0; i < stands.size(); i++) {
                    ArmorStand as = stands.get(i);
                    Vector base = offsets.get(i % offsets.size());
                    Vector rotated = rotateAroundAxis(base, axis, spin);
                    as.teleport(loc.clone().add(rotated));
                    as.setHeadPose(new EulerAngle(spin, spin, 0));
                }
                for (int sign : new int[]{1, -1}) {
                    HelixEffect helix = new HelixEffect(plugin.getEffectManager());
                    helix.setLocation(loc);
                    helix.particle   = Particle.FLAME;
                    helix.strands    = 1;
                    helix.particles  = 1;
                    helix.radius     = 0.1f;
                    helix.curve      = 1.0f;
                    helix.rotation   = sign * spin * 0.3;
                    helix.iterations = 1;
                    helix.run();
                }
                world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 0.4f, 1f);

                // 3) Entity collision—unchanged
                for (Entity e : world.getNearbyEntities(loc, 1.2, 1.2, 1.2)) {
                    if (e instanceof ArmorStand) continue;
                    if (!(e instanceof LivingEntity le) || le == player) continue;
                    if (le instanceof Player p
                        && !DuelManager.getInstance()
                        .areInDuel(player.getUniqueId(), p.getUniqueId())) continue;
                    impactNow(player, loc, stands, finalDamage);
                    cancel();
                    return;
                }

                // 4) Ground collision only at the pre-computed impact point
                if (loc.distanceSquared(impact) < 1.0) {
                    impactNow(player, impact, stands, finalDamage);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }


    private void impactNow(Player player, Location center, List<ArmorStand> stands, double damage) {
        stands.forEach(ArmorStand::remove);
        stands.clear();

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

        for (Entity e : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (!(e instanceof LivingEntity le)) continue;
            if (le instanceof Player p
                && !DuelManager.getInstance().areInDuel(player.getUniqueId(), p.getUniqueId())) continue;
            SpellUtils.dealWithChat(player, le, damage, "Meteor");
        }
    }

    private Location getImpactLocation(Player player) {
        Block target = player.getTargetBlockExact(20);
        if (target != null) {
            return target.getLocation().add(0.5, 1, 0.5);
        } else {
            return player.getEyeLocation()
                .add(player.getEyeLocation().getDirection().multiply(20));
        }
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

}
