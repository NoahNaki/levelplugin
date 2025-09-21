package me.nakilex.levelplugin.spells.effect.warrior;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.context.SpellCastContextCompat;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import me.nakilex.levelplugin.spells.utils.animation.SpellAnimation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.List;

/**
 * Ground slam style effect that can expand outward or implode inward in rings.
 * Radius can be increased via the "aoeRadius" extra param.
 */
public class ShockwaveEffect implements SpellEffect {

    public enum WaveDirection {
        OUTWARD(1),
        INWARD(-1);

        private final int vectorMultiplier;

        WaveDirection(int vectorMultiplier) {
            this.vectorMultiplier = vectorMultiplier;
        }

        public int getVectorMultiplier() {
            return vectorMultiplier;
        }
    }

    private final String damageSourceName;
    private final WaveDirection waveDirection;
    private final double baseRadius;
    private final double entityHorizontalForce;
    private final double entityVerticalBoost;
    private final double fallingBlockHorizontalSpeed;
    private final double fallingBlockVerticalSpeed;

    public ShockwaveEffect() {
        this(
            "Shockwave",
            WaveDirection.OUTWARD,
            10.0,
            0.5,
            0.3,
            0.2,
            0.15
        );
    }

    public ShockwaveEffect(
        String damageSourceName,
        WaveDirection waveDirection,
        double baseRadius,
        double entityHorizontalForce,
        double entityVerticalBoost,
        double fallingBlockHorizontalSpeed,
        double fallingBlockVerticalSpeed
    ) {
        this.damageSourceName = damageSourceName;
        this.waveDirection = waveDirection;
        this.baseRadius = baseRadius;
        this.entityHorizontalForce = entityHorizontalForce;
        this.entityVerticalBoost = entityVerticalBoost;
        this.fallingBlockHorizontalSpeed = fallingBlockHorizontalSpeed;
        this.fallingBlockVerticalSpeed = fallingBlockVerticalSpeed;
    }

    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null) {
            return;
        }

        double damage = ctx.getFinalDamage();

        double maxRadius = baseRadius;
        Object radiusParam = ctx.getExtraParam("aoeRadius");
        if (radiusParam instanceof Number number) {
            maxRadius += number.doubleValue();
        } else if (radiusParam instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Number number) {
                    maxRadius += number.doubleValue();
                }
            }
        }

        int duration = 20;
        int steps = 10;
        double radiusStep = maxRadius / steps;

        World world = player.getWorld();
        Location playerLocation = player.getLocation();
        world.playSound(playerLocation, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
        world.playSound(playerLocation, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1f);
        world.spawnParticle(Particle.EXPLOSION, playerLocation, 10, 0.5, 0.5, 0.5);

        Plugin plugin = Bukkit.getPluginManager().getPlugin("LevelPlugin");
        if (plugin == null) {
            plugin = Main.getInstance();
        }
        final Plugin effectPlugin = plugin;

        new SpellAnimation(duration / steps, duration) {
            double currentRadius = waveDirection == WaveDirection.OUTWARD ? 0.0 : maxRadius + radiusStep;

            @Override
            protected void onTick(int tick) {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                if (waveDirection == WaveDirection.OUTWARD) {
                    if (currentRadius >= maxRadius) {
                        cancel();
                        return;
                    }
                    currentRadius += radiusStep;
                } else {
                    if (currentRadius <= 0.0) {
                        cancel();
                        return;
                    }
                    currentRadius -= radiusStep;
                }

                double effectiveRadius = Math.max(0.1, currentRadius);
                Location center = player.getLocation();
                World ringWorld = center.getWorld();

                for (double angle = 0; angle < 360; angle += 10) {
                    double radians = Math.toRadians(angle);
                    double x = Math.cos(radians) * effectiveRadius;
                    double z = Math.sin(radians) * effectiveRadius;
                    Location loc = center.clone().add(x, 0, z);

                    ringWorld.spawnParticle(Particle.BLOCK_CRUMBLE, loc, 10, 0.2, 0.2, 0.2, 0.1, Material.DIRT.createBlockData());
                    ringWorld.spawnParticle(Particle.CRIT, loc, 5, 0.2, 0.2, 0.2);

                    if (Math.random() < 0.2) {
                        Block ground = ringWorld.getHighestBlockAt(loc);
                        if (ground.getType() != Material.AIR) {
                            Location fallingSpawn = ground.getLocation().add(0.5, 1.0, 0.5);
                            FallingBlock fb = ringWorld.spawnFallingBlock(fallingSpawn, ground.getBlockData());
                            fb.setDropItem(false);
                            double directionMultiplier = waveDirection.getVectorMultiplier();
                            Vector velocity = new Vector(
                                Math.cos(radians) * fallingBlockHorizontalSpeed * directionMultiplier,
                                fallingBlockVerticalSpeed,
                                Math.sin(radians) * fallingBlockHorizontalSpeed * directionMultiplier
                            );
                            fb.setVelocity(velocity);
                            if (effectPlugin != null) {
                                fb.setMetadata("Shockwave", new FixedMetadataValue(effectPlugin, true));
                                Bukkit.getScheduler().runTaskLater(effectPlugin, fb::remove, 80L);
                            }
                        }
                    }

                    for (Entity entity : ringWorld.getNearbyEntities(loc, 1, 1, 1)) {
                        if (!(entity instanceof LivingEntity living) || living.equals(player)) {
                            continue;
                        }

                        if (living instanceof Player other &&
                            !DuelManager.getInstance().areInDuel(player.getUniqueId(), other.getUniqueId())) {
                            continue;
                        }

                        SpellUtils.dealWithChat(player, living, damage, damageSourceName);

                        Vector offset = living.getLocation().toVector().subtract(center.toVector());
                        if (offset.lengthSquared() == 0) {
                            continue;
                        }

                        Vector knockback = offset.normalize().multiply(entityHorizontalForce * waveDirection.getVectorMultiplier());
                        knockback.setY(entityVerticalBoost);
                        living.setVelocity(knockback);
                    }

                    if (!loc.getBlock().isPassable()) {
                        break;
                    }
                }

                world.playSound(center, Sound.ENTITY_IRON_GOLEM_ATTACK, 0.5f, 0.8f);
                world.playSound(center, Sound.BLOCK_STONE_BREAK, 0.7f, 1f);
            }
        };

        SpellCastContextCompat.markSuccess(ctx, true);
    }
}
