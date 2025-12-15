package me.nakilex.levelplugin.spells.effect.mage;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.context.SpellCastContextCompat;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import me.nakilex.levelplugin.spells.utils.animation.SpellAnimation;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.PlayerStats;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.Optional;
import org.bukkit.inventory.ItemStack;
import io.lumine.mythic.bukkit.MythicBukkit;

import java.util.HashSet;
import java.util.Set;

/**
 * Custom in‑house meteor implementation that drops a magma block “core” using an armor
 * stand and fragments the path with falling magma blocks. Handles collision manually so
 * low ceilings do not instantly explode the meteor and filters damage to valid targets.
 */
public class MagmaMeteorEffect implements SpellEffect {

    private static final double DEFAULT_RANGE = 28.0;
    private static final double DESIRED_SPAWN_HEIGHT = 15.0;
    private static final double MIN_TRAVEL_DISTANCE = 4.0;
    private static final double STEP_SPEED = 1.2;
    private static final double IMPACT_RADIUS = 4.0;
    private static final int IGNITE_TICKS = 80;

    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null || !player.isOnline()) return;

        Location impact = findImpactLocation(player, DEFAULT_RANGE);
        if (impact == null) return;

        Location spawn = findSpawnLocation(impact, DESIRED_SPAWN_HEIGHT, MIN_TRAVEL_DISTANCE);
        Vector velocity = impact.clone().subtract(spawn).toVector().normalize().multiply(STEP_SPEED);

        ArmorStand meteor = spawnMeteorStand(spawn);
        BlockData magmaData = Material.MAGMA_BLOCK.createBlockData();

        new SpellAnimation(1, 80) {
            private Location current = spawn.clone();
            private boolean exploded = false;

            @Override
            protected void onTick(int tick) {
                if (exploded || !meteor.isValid()) {
                    cancel();
                    return;
                }

                // Advance meteor
                current.add(velocity);
                meteor.teleport(current);
                spawnTrail(current, magmaData);

                if (hasCollided(current)) {
                    explode(current, player, ctx);
                    exploded = true;
                    meteor.remove();
                    cancel();
                    return;
                }

                LivingEntity directHit = findDirectHit(player, current, 1.5);
                if (directHit != null) {
                    explode(current, player, ctx);
                    exploded = true;
                    meteor.remove();
                    cancel();
                }
            }

            @Override
            protected void onEnd() {
                meteor.remove();
            }
        };
    }

    private Location findImpactLocation(Player player, double maxRange) {
        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        Location cursor = eye.clone().add(direction.multiply(maxRange));

        Block targetBlock = player.getTargetBlockExact((int) maxRange);
        if (targetBlock != null) {
            cursor = targetBlock.getLocation().add(0.5, 1.0, 0.5);
        }

        // Ensure we always impact at ground level even if aiming mid‑air
        Location ground = cursor.clone();
        while (ground.getY() > world.getMinHeight() && ground.getBlock().isPassable()) {
            ground.subtract(0, 1, 0);
        }
        ground.add(0, 1, 0);
        return ground;
    }

    private Location findSpawnLocation(Location impact, double desiredHeight, double minTravel) {
        World world = impact.getWorld();
        int columnX = impact.getBlockX();
        int columnZ = impact.getBlockZ();
        int maxY = Math.min(world.getMaxHeight() - 2, (int) Math.floor(impact.getY() + desiredHeight));
        int impactY = (int) Math.floor(impact.getY());

        int airRun = 0;
        int chosenY = -1;
        int highestPassable = -1;
        for (int y = maxY; y > impactY; y--) {
            if (world.getBlockAt(columnX, y, columnZ).isPassable()) {
                highestPassable = Math.max(highestPassable, y);
                airRun++;
                if (airRun >= minTravel) {
                    chosenY = y;
                    break;
                }
            } else {
                airRun = 0;
            }
        }

        if (chosenY == -1) {
            chosenY = highestPassable > 0 ? highestPassable : impactY + 1;
        }
        return new Location(world, impact.getX(), chosenY + 0.1, impact.getZ());
    }

    private ArmorStand spawnMeteorStand(Location spawn) {
        World world = spawn.getWorld();
        ItemStack model = resolveMeteorModel();

        ArmorStand meteor = world.spawn(spawn, ArmorStand.class, stand -> {
            stand.setInvisible(true);
            stand.setMarker(true);
            stand.setGravity(false);
            stand.setInvulnerable(true);
            stand.setSmall(false);
            stand.setSilent(true);
            stand.getEquipment().setHelmet(model);
            stand.setMetadata("Meteor", new FixedMetadataValue(Main.getInstance(), true));
        });
        return meteor;
    }

    private ItemStack resolveMeteorModel() {
        try {
            Optional<ItemStack> mythicItem = MythicBukkit.inst()
                .getItemManager()
                .getItemStack("meteor");
            if (mythicItem.isPresent()) {
                return mythicItem.get();
            }
        } catch (Exception ignored) {
            // fall through to magma block fallback
        }
        return new ItemStack(Material.MAGMA_BLOCK);
    }

    private void spawnTrail(Location location, BlockData magmaData) {
        World world = location.getWorld();
        world.spawnParticle(Particle.SMOKE_LARGE, location, 6, 0.35, 0.35, 0.35, 0.01);
        world.spawnParticle(Particle.FLAME, location, 10, 0.3, 0.3, 0.3, 0.02);
        world.spawnParticle(Particle.LAVA, location, 8, 0.25, 0.25, 0.25, 0.02);
        world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, location, 5, 0.35, 0.2, 0.35, 0.015);

        FallingBlock fragment = world.spawnFallingBlock(location, magmaData);
        fragment.setDropItem(false);
        fragment.setVelocity(new Vector(
            (Math.random() - 0.5) * 0.4,
            -0.6,
            (Math.random() - 0.5) * 0.4
        ));
        fragment.setMetadata("Meteor", new FixedMetadataValue(Main.getInstance(), true));
        fragment.setHurtEntities(false);
        Main.getInstance().getServer().getScheduler().runTaskLater(
            Main.getInstance(), fragment::remove, 40L
        );
    }

    private boolean hasCollided(Location location) {
        Block block = location.getBlock();
        if (!block.isPassable()) return true;
        Block below = block.getRelative(BlockFace.DOWN);
        return below.getType().isSolid() && location.getY() <= below.getY() + 1.1;
    }

    private LivingEntity findDirectHit(Player caster, Location location, double radius) {
        World world = location.getWorld();
        BoundingBox box = BoundingBox.of(location, radius, radius, radius);
        for (LivingEntity entity : world.getLivingEntities()) {
            if (entity.equals(caster)) continue;
            if (entity instanceof ArmorStand) continue;
            if (!box.overlaps(entity.getBoundingBox())) continue;
            if (entity instanceof Player other &&
                !DuelManager.getInstance().areInDuel(caster.getUniqueId(), other.getUniqueId())) {
                continue;
            }
            return entity;
        }
        return null;
    }

    private void explode(Location center, Player caster, SpellCastContext ctx) {
        World world = center.getWorld();
        world.spawnParticle(Particle.EXPLOSION_LARGE, center, 4, 0.5, 0.5, 0.5, 0.05);
        world.spawnParticle(Particle.LAVA, center, 25, 0.7, 0.4, 0.7, 0.05);
        world.playSound(center, org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
        world.playSound(center, org.bukkit.Sound.BLOCK_LAVA_EXTINGUISH, 0.8f, 0.8f);

        SpellCastContextCompat.markSuccess(ctx, true);

        DamageResult damage = computeMageDamage(caster, ctx);
        Set<LivingEntity> alreadyHit = new HashSet<>();
        for (LivingEntity target : world.getNearbyLivingEntities(center, IMPACT_RADIUS)) {
            if (target.equals(caster)) continue;
            if (target instanceof ArmorStand) continue;
            if (!alreadyHit.add(target)) continue;
            if (target instanceof Player other &&
                !DuelManager.getInstance().areInDuel(caster.getUniqueId(), other.getUniqueId())) {
                continue;
            }

            SpellUtils.dealWithChat(
                caster,
                target,
                damage.amount(),
                ctx.getBaseSpell().getDisplayName(),
                true,
                damage.isCrit()
            );
            target.setFireTicks(Math.max(target.getFireTicks(), IGNITE_TICKS));
        }
    }

    private DamageResult computeMageDamage(Player caster, SpellCastContext ctx) {
        PlayerStats stats = StatsManager.getInstance().getPlayerStats(caster.getUniqueId());
        double damage = ctx.getFinalDamage();

        int totalInt = stats.baseIntelligence + stats.bonusIntelligence;
        int totalTec = stats.baseTechnique + stats.bonusTechnique;
        int totalDex = stats.baseDexterity + stats.bonusDexterity;

        damage += totalInt * 0.5;
        damage *= (1.0 + totalTec * 0.003);

        double critChance = (double) totalDex / (totalDex + 100.0);
        boolean isCrit = Math.random() < critChance;
        if (isCrit) {
            damage *= 2;
        }

        damage *= me.nakilex.levelplugin.player.attributes.listeners.StatsEffectListener.SPELL_DAMAGE_MULTIPLIER;
        return new DamageResult(damage, isCrit);
    }

    private record DamageResult(double amount, boolean isCrit) {}
}
