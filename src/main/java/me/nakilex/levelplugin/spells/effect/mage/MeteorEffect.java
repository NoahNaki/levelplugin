package me.nakilex.levelplugin.spells.effect.mage;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.context.SpellCastContextCompat;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.function.Predicate;

/**
 * Custom Meteor effect that spawns a magma-block meteorite using an armor stand.
 * Handles ceilings gracefully, performs AOE damage, and ignites hit targets.
 */
public class MeteorEffect implements SpellEffect {

    private static final String METEOR_META = "Meteor";

    @Override
    public void apply(SpellCastContext ctx) {
        Player caster = ctx.getPlayer();
        World world = caster.getWorld();
        Main plugin = Main.getInstance();

        double baseDamage = ctx.getFinalDamage();
        String spellName = ctx.getBaseSpell().getDisplayName();
        double impactRadius = getDouble(ctx, "impactRadius", 4.0);
        double maxRange = getDouble(ctx, "range", 25.0);
        double spawnHeight = getDouble(ctx, "spawnHeight", 15.0);

        Location eye = caster.getEyeLocation();
        Vector direction = eye.getDirection().normalize();

        Location target = findImpactLocation(caster, direction, maxRange);
        Location spawn = findSpawnLocationAbove(target, spawnHeight);

        ArmorStand stand = world.spawn(spawn, ArmorStand.class, as -> {
            as.setVisible(false);
            as.setMarker(false);
            as.setGravity(true);
            as.setSmall(true);
            as.setInvulnerable(true);
            as.setCollidable(false);
            as.getEquipment().setHelmet(new ItemStack(Material.MAGMA_BLOCK));
            as.setMetadata(METEOR_META, new FixedMetadataValue(plugin, true));
        });

        Vector velocity = direction.clone().multiply(0.6).setY(-0.9);
        stand.setVelocity(velocity);

        new BukkitRunnable() {
            private int ticksLived = 0;

            @Override
            public void run() {
                if (stand.isDead() || !stand.isValid()) {
                    cancel();
                    return;
                }

                if (ticksLived++ > 200) {
                    stand.remove();
                    cancel();
                    return;
                }

                Location current = stand.getLocation();
                world.spawnParticle(Particle.SMOKE_NORMAL, current, 4, 0.2, 0.2, 0.2, 0.01);
                world.spawnParticle(Particle.FLAME, current, 4, 0.2, 0.2, 0.2, 0.01);

                if (hasCollided(current) || current.getY() <= target.getY()) {
                    explode(caster, baseDamage, impactRadius, current, spellName);
                    stand.remove();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);

        SpellCastContextCompat.markSuccess(ctx, true);
    }

    private Location findImpactLocation(Player caster, Vector direction, double maxRange) {
        RayTraceResult result = caster.rayTraceBlocks(maxRange);
        if (result != null && result.getHitPosition() != null) {
            return result.getHitPosition().toLocation(caster.getWorld());
        }

        Location fallback = caster.getEyeLocation().add(direction.multiply(maxRange));
        int groundY = caster.getWorld().getHighestBlockYAt(fallback);
        fallback.setY(groundY + 1.0);
        return fallback;
    }

    private Location findSpawnLocationAbove(Location target, double preferredHeight) {
        World world = target.getWorld();
        double maxY = Math.min(world.getMaxHeight() - 2, target.getY() + preferredHeight);
        for (double y = maxY; y >= target.getY() + 2; y -= 1.0) {
            Location candidate = new Location(world, target.getX(), y, target.getZ());
            if (isPassableColumn(candidate)) {
                return candidate;
            }
        }
        return target.clone().add(0, 2, 0);
    }

    private boolean isPassableColumn(Location loc) {
        World world = loc.getWorld();
        return world.getBlockAt(loc).isPassable() && world.getBlockAt(loc.clone().add(0, 1, 0)).isPassable();
    }

    private boolean hasCollided(Location location) {
        World world = location.getWorld();
        return !world.getBlockAt(location).isPassable() || !world.getBlockAt(location.clone().add(0, 1, 0)).isPassable();
    }

    private void explode(Player caster, double baseDamage, double radius, Location impact, String spellName) {
        World world = impact.getWorld();
        world.playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.6f);
        world.spawnParticle(Particle.EXPLOSION_LARGE, impact, 2, 0.4, 0.4, 0.4, 0.01);
        world.spawnParticle(Particle.LAVA, impact, 20, 0.4, 0.4, 0.4, 0.05);

        Predicate<LivingEntity> validTarget = entity -> {
            if (entity.equals(caster)) return false;
            if (entity instanceof Player other) {
                return DuelManager.getInstance().areInDuel(caster.getUniqueId(), other.getUniqueId());
            }
            return true;
        };

        for (Entity entity : world.getNearbyEntities(impact, radius, radius, radius)) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (!validTarget.test(living)) continue;

            SpellUtils.dealWithChat(caster, living, baseDamage, spellName);
            living.setFireTicks(Math.max(living.getFireTicks(), 80));
        }
    }

    private double getDouble(SpellCastContext ctx, String key, double fallback) {
        Object value = ctx.getExtraParam(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String str) {
            try {
                return Double.parseDouble(str);
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        return fallback;
    }
}
