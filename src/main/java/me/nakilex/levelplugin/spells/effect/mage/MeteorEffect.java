package me.nakilex.levelplugin.spells.effect.mage;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.player.attributes.listeners.StatsEffectListener;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Custom Meteor effect that spawns a magma-block meteorite using an armor stand.
 * Handles ceilings gracefully, performs AOE damage, and ignites hit targets.
 */
public class MeteorEffect implements SpellEffect {

    private static final String METEOR_META = "Meteor";
    private static final Vector[] METEOR_OFFSETS = new Vector[] {
        new Vector(0, 0, 0),
        new Vector(0.35, 0.15, 0),
        new Vector(-0.35, 0.15, 0),
        new Vector(0, 0.15, 0.35),
        new Vector(0, 0.15, -0.35),
        new Vector(0.35, -0.05, 0.35),
        new Vector(-0.35, -0.05, -0.35)
    };

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
        double travelSpeed = getDouble(ctx, "meteorSpeed", 0.6);

        Location eye = caster.getEyeLocation();
        Vector direction = eye.getDirection().normalize();

        Location target = findImpactLocation(caster, direction, maxRange);
        Location spawn = findSpawnLocationAbove(target, spawnHeight);

        List<ArmorStand> pieces = spawnMeteorPieces(plugin, spawn);
        ArmorStand anchor = pieces.get(0);

        Vector travelVector = target.clone().subtract(spawn).toVector();
        double totalDistance = travelVector.length();
        if (totalDistance == 0) totalDistance = 0.001; // prevent div-by-zero
        Vector step = travelVector.normalize().multiply(travelSpeed);

        new BukkitRunnable() {
            private int ticksLived = 0;
            private double traveled = 0;

            @Override
            public void run() {
                if (anchor.isDead() || !anchor.isValid()) {
                    cancel();
                    return;
                }

                if (ticksLived++ > 200) {
                    removePieces(pieces);
                    cancel();
                    return;
                }

                Location current = anchor.getLocation().add(step);
                anchor.teleport(current);
                teleportPieces(pieces, current);

                traveled += step.length();

                world.spawnParticle(Particle.SMOKE_NORMAL, current, 8, 0.25, 0.25, 0.25, 0.0);
                world.spawnParticle(Particle.FLAME, current, 8, 0.25, 0.25, 0.25, 0.0);

                boolean reachedTarget = current.distanceSquared(target) <= 1.0;
                boolean collision = hasCollided(current) || traveled >= totalDistance + 1.0;

                if (reachedTarget || collision) {
                    explode(caster, baseDamage, impactRadius, current, spellName);
                    removePieces(pieces);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);

        SpellCastContextCompat.markSuccess(ctx, true);
    }

    private List<ArmorStand> spawnMeteorPieces(Main plugin, Location spawn) {
        World world = spawn.getWorld();
        List<ArmorStand> stands = new ArrayList<>();

        for (Vector offset : METEOR_OFFSETS) {
            Location loc = spawn.clone().add(offset);
            ArmorStand stand = world.spawn(loc, ArmorStand.class, as -> {
                as.setVisible(false);
                as.setMarker(true);
                as.setGravity(false);
                as.setSmall(false);
                as.setInvulnerable(true);
                as.setCollidable(false);
                as.getEquipment().setHelmet(new ItemStack(Material.MAGMA_BLOCK));
                as.setMetadata(METEOR_META, new FixedMetadataValue(plugin, true));
            });
            stands.add(stand);
        }

        return stands;
    }

    private void teleportPieces(List<ArmorStand> stands, Location anchor) {
        for (int i = 0; i < stands.size(); i++) {
            ArmorStand stand = stands.get(i);
            if (stand == null || stand.isDead()) continue;

            stand.teleport(anchor.clone().add(METEOR_OFFSETS[Math.min(i, METEOR_OFFSETS.length - 1)]));
        }
    }

    private void removePieces(List<ArmorStand> stands) {
        for (ArmorStand stand : stands) {
            if (stand != null && stand.isValid()) {
                stand.remove();
            }
        }
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

        StatsManager.PlayerStats stats = StatsManager.getInstance().getPlayerStats(caster.getUniqueId());
        int totalDexterity = stats.baseDexterity + stats.bonusDexterity;
        double critChance = (double) totalDexterity / (totalDexterity + 100.0);

        for (Entity entity : world.getNearbyEntities(impact, radius, radius, radius)) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (!validTarget.test(living)) continue;

            boolean isCrit = Math.random() < Math.max(0.0, Math.min(1.0, critChance));

            double scaledDamage = SpellUtils.scaleMageSpellDamage(caster, baseDamage, isCrit);
            StatsEffectListener.recordCrit(caster, isCrit);

            SpellUtils.dealWithChat(caster, living, scaledDamage, spellName, isCrit, true);
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
