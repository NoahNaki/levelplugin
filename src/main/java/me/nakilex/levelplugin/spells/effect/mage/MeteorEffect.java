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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Exact reincarnation of the original castMeteor logic as a SpellEffect.
 */
public class MeteorEffect implements SpellEffect {

    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getCaster();
        Main plugin = Main.getInstance();
        World world = player.getWorld();

        plugin.getLogger().info("[MeteorEffect] Running apply() for " + player.getName());
        plugin.getLogger().info("[MeteorEffect]   Base spell:     " + ctx.getBaseSpell().getId());
        plugin.getLogger().info("[MeteorEffect]   Damage mult:    " + ctx.getFinalDamageMultiplier());
        plugin.getLogger().info("[MeteorEffect]   Cooldown mult:  " + ctx.getFinalCooldown());
        plugin.getLogger().info("[MeteorEffect]   Effect key:     " + ctx.getEffectKey());
        plugin.getLogger().info("[MeteorEffect]   Extra params:   " + ctx.getExtraParams());

        // 1) Compute raw damage as before
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        int playerInt = ps.baseIntelligence + ps.bonusIntelligence;
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        CustomItem cItem = ItemManager.getInstance().getCustomItemFromItemStack(mainHand);
        int weaponInt = (cItem != null) ? cItem.getIntel() : 0;
        double rawDamage = 6.0 + (playerInt + weaponInt);

        // 2) Apply rune‐driven damage multiplier
        double finalDamage = rawDamage * ctx.getFinalDamageMultiplier();

        // 3) Pull out any transform params (e.g. extraProjectiles)
        int extraProj = (int) ctx.getExtraParams().getOrDefault("extraProjectiles", 0);

        // 4) Determine true impact point
        Block targetBlock = player.getTargetBlockExact(20);
        Location impact = (targetBlock != null)
            ? targetBlock.getLocation().add(0.5, 1, 0.5)
            : player.getLocation().add(player.getLocation().getDirection().multiply(20));

        // 5) Build directional spawn above-left
        Vector look = player.getEyeLocation().getDirection().normalize();
        Vector up    = new Vector(0, 1, 0);
        Vector right = up.clone().crossProduct(look).normalize();
        Vector left  = right.clone().multiply(-1);
        double heightAbove      = 30;
        double horizontalOffset = 18;
        Location spawn = impact.clone()
            .add(up.multiply(heightAbove))
            .add(left.multiply(horizontalOffset));

        // 6) Launch sound
        world.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1f);

        // 7) Precompute the sphere offsets
        List<Vector> offsets = getSphereOffsets(0.5, 8);

        // 8) Spawn multiple sets of armour‐stands based on extraProj
        List<ArmorStand> stands = new ArrayList<>();
        for (int round = 0; round < 1 + extraProj; round++) {
            for (Vector off : offsets) {
                ArmorStand as = (ArmorStand) world.spawn(spawn.clone().add(off), ArmorStand.class, stand -> {
                    stand.setInvisible(true);
                    stand.setMarker(true);
                    stand.setGravity(false);
                    stand.getEquipment().setHelmet(new ItemStack(Material.MAGMA_BLOCK));
                    stand.setMetadata("Meteor", new FixedMetadataValue(plugin, true));
                });
                stands.add(as);
            }
        }

        // 9) Animate & move them all forward
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
                loc.add(step);

                // Flame trail
                Location trailPos = loc.clone().subtract(step.clone().normalize().multiply(1.0));
                world.spawnParticle(Particle.FLAME, trailPos, 8, 0.2, 0.2, 0.2, 0.01);

                // Rotate stands
                double spinAngle = ticks * 0.1;
                Vector axis = step.clone().normalize();
                for (int i = 0; i < stands.size(); i++) {
                    ArmorStand as = stands.get(i);
                    Vector baseOffset = offsets.get(i % offsets.size());
                    Vector rotated = rotateAroundAxis(baseOffset, axis, spinAngle);
                    as.teleport(loc.clone().add(rotated));
                    as.setHeadPose(new EulerAngle(spinAngle, spinAngle, 0));
                }

                // Fiery helices
                for (int sign : new int[]{1, -1}) {
                    HelixEffect helix = new HelixEffect(plugin.getEffectManager());
                    helix.setLocation(loc);
                    helix.particle   = Particle.FLAME;
                    helix.strands    = 1;
                    helix.particles  = 1;
                    helix.radius     = 0.1f;
                    helix.curve      = 1.0f;
                    helix.rotation   = sign * ticks * 0.3;
                    helix.iterations = 1;
                    helix.period     = 1;
                    helix.start();
                }

                world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 0.4f, 1f);

                // Collision check
                for (Entity e : world.getNearbyEntities(loc, 1.2, 1.2, 1.2)) {
                    if (e instanceof ArmorStand) continue;
                    if (!(e instanceof LivingEntity le) || le == player) continue;
                    if (!DuelManager.getInstance().areInDuel(player.getUniqueId(), ((Player) le).getUniqueId())) continue;
                    impactNow(loc);
                    cancel();
                    return;
                }

                // Impact on ground
                if (loc.distanceSquared(impact) < 1.0) {
                    impactNow(impact);
                    cancel();
                }
            }

            private Vector rotateAroundAxis(Vector v, Vector axis, double theta) {
                axis = axis.clone().normalize();
                double cos = Math.cos(theta), sin = Math.sin(theta);
                double dot = v.dot(axis);
                Vector term1 = v.clone().multiply(cos);
                Vector term2 = axis.clone().multiply(dot * (1 - cos));
                Vector term3 = axis.clone().crossProduct(v).multiply(sin);
                return term1.add(term2).add(term3);
            }

            private void impactNow(Location here) {
                // remove all stands
                for (ArmorStand as : stands) {
                    as.remove();
                }
                stands.clear();

                // Shockwave VFX
                SphereEffect shock = new SphereEffect(plugin.getEffectManager());
                shock.setLocation(here);
                shock.particle   = Particle.EXPLOSION;
                shock.particles  = 20;
                shock.radius     = 3.0;
                shock.iterations = 5;
                shock.period     = 1;
                shock.yOffset    = 0.0;
                shock.start();

                // Explosion sound
                here.getWorld().playSound(here, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);

                // Real damage: use finalDamage
                here.getWorld().spawnParticle(Particle.EXPLOSION, here, 1);
                double radius = 4.0;
                for (Entity e : world.getNearbyEntities(here, radius, radius, radius)) {
                    if (!(e instanceof LivingEntity le) || le == player) continue;
                    if (le instanceof Player p
                        && !DuelManager.getInstance().areInDuel(player.getUniqueId(), p.getUniqueId())) continue;
                    SpellUtils.dealWithChat(player, le, finalDamage, "Meteor");
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }


    private List<Vector> getSphereOffsets(double radius, int maxPoints) {
        List<Vector> list = new ArrayList<>();
        for (int i = 0; i < maxPoints; i++) {
            double theta = Math.acos(2 * Math.random() - 1);
            double phi   = 2 * Math.PI * Math.random();
            double x = radius * Math.sin(theta) * Math.cos(phi);
            double y = radius * Math.sin(theta) * Math.sin(phi);
            double z = radius * Math.cos(theta);
            list.add(new Vector(x, y, z));
        }
        return list;
    }
}