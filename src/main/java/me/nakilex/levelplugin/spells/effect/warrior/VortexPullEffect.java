package me.nakilex.levelplugin.spells.effect.warrior;

import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.context.SpellCastContextCompat;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.utils.animation.SpellAnimation;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Pulls nearby entities toward the warrior while a shrinking circle of particles forms on the ground.
 */
public class VortexPullEffect implements SpellEffect {
    private static final double AOE_RADIUS = 5.0;
    private static final double AOE_HEIGHT = 3.0;
    private static final double MIN_RING_RADIUS = 0.8;
    private static final int    ITERATIONS = 18;
    private static final int    INTERVAL_TICKS = 2;

    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null || !player.isOnline()) {
            return;
        }

        SpellCastContextCompat.markSuccess(ctx, true);

        var world = player.getWorld();
        world.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 0.8f, 0.75f);
        world.spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().add(0, 1.0, 0), 8, 0.2, 0.1, 0.2, 0.01);

        final Particle.DustOptions outerDust = new Particle.DustOptions(Color.fromRGB(92, 22, 22), 1.35f);
        final Particle.DustOptions innerDust = new Particle.DustOptions(Color.fromRGB(168, 47, 47), 0.9f);

        new SpellAnimation(INTERVAL_TICKS, ITERATIONS) {
            private double ringRotation = 0.0;

            @Override
            protected void onTick(int tick) {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                Location feet = player.getLocation().clone();
                Location groundCenter = feet.clone();
                groundCenter.setY(feet.getY() - 0.1);
                Location pullCenter = feet.add(0, 0.55, 0);

                double progress = ITERATIONS <= 1 ? 1.0 : (tick / (double) (ITERATIONS - 1));
                double baseRadius = Math.max(MIN_RING_RADIUS, AOE_RADIUS - (AOE_RADIUS - MIN_RING_RADIUS) * progress);
                double ripple = Math.sin(progress * Math.PI * 1.5) * 0.35;
                double ringRadius = Math.max(MIN_RING_RADIUS * 0.65, baseRadius + ripple);

                spawnRing(groundCenter, ringRadius, outerDust);
                if (tick % 2 == 0) {
                    double innerRadius = Math.max(MIN_RING_RADIUS * 0.5, ringRadius * 0.6);
                    spawnRing(groundCenter.clone().add(0, 0.1, 0), innerRadius, innerDust);
                }

                world.spawnParticle(Particle.SMOKE_NORMAL, groundCenter, 10, ringRadius * 0.25, 0.15, ringRadius * 0.25, 0.01);
                world.spawnParticle(Particle.PORTAL, pullCenter, 18, 0.35, 0.4, 0.35, 0.05);

                pullEntities(pullCenter);

                ringRotation += Math.PI / 12.0;
                if (tick % 3 == 0) {
                    world.playSound(groundCenter, Sound.BLOCK_BEACON_AMBIENT, 0.35f, 1.9f);
                }
            }

            private void spawnRing(Location center, double radius, Particle.DustOptions dust) {
                double step = Math.PI / 12.0;
                for (double angle = ringRotation; angle < ringRotation + Math.PI * 2; angle += step) {
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    Location sample = center.clone().add(x, 0, z);
                    center.getWorld().spawnParticle(Particle.REDSTONE, sample, 1, 0.02, 0.02, 0.02, 0, dust);
                    center.getWorld().spawnParticle(Particle.CRIT, sample.clone().add(0, 0.15, 0), 1, 0.0, 0.0, 0.0, 0.0);
                }
            }

            private void pullEntities(Location center) {
                for (Entity entity : center.getWorld().getNearbyEntities(center, AOE_RADIUS, AOE_HEIGHT, AOE_RADIUS)) {
                    if (!(entity instanceof LivingEntity living) || living.equals(player) || entity instanceof ArmorStand) {
                        continue;
                    }
                    if (living instanceof Player other
                        && !DuelManager.getInstance().areInDuel(player.getUniqueId(), other.getUniqueId())) {
                        continue;
                    }

                    Vector delta = center.toVector().subtract(living.getLocation().toVector());
                    double distance = delta.length();
                    if (distance > AOE_RADIUS || distance < 0.1) {
                        continue;
                    }

                    double pullStrength = 0.18 + (1.0 - distance / AOE_RADIUS) * 0.55;
                    Vector velocity = delta.normalize().multiply(pullStrength);
                    double yBoost = 0.12 + (1.0 - distance / AOE_RADIUS) * 0.18;
                    velocity.setY(Math.min(0.45, Math.max(velocity.getY(), yBoost)));
                    living.setVelocity(living.getVelocity().multiply(0.35).add(velocity));
                }
            }

            @Override
            protected void onEnd() {
                if (!player.isOnline()) {
                    return;
                }
                Location endLoc = player.getLocation().add(0, 0.5, 0);
                world.playSound(endLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.65f, 1.3f);
                world.spawnParticle(Particle.EXPLOSION_NORMAL, endLoc, 16, 0.2, 0.2, 0.2, 0.04);
            }
        };
    }
}
