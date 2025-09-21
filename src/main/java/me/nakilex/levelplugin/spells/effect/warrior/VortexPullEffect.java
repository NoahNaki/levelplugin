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
 * Pulls nearby entities toward the warrior while swirling particles orbit them.
 */
public class VortexPullEffect implements SpellEffect {
    private static final double AOE_RADIUS = 5.0;
    private static final double AOE_HEIGHT = 3.0;
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
        world.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 0.75f, 0.85f);
        world.spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().add(0, 1.1, 0), 6, 0.2, 0.2, 0.2, 0.02);

        Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(92, 22, 22), 1.4f);

        new SpellAnimation(INTERVAL_TICKS, ITERATIONS) {
            private double angleOffset = 0.0;

            @Override
            protected void onTick(int tick) {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                Location center = player.getLocation().clone().add(0, 0.7, 0);
                double progress = tick / (double) ITERATIONS;
                double ringRadius = Math.max(0.6, AOE_RADIUS * (1.0 - 0.55 * progress));
                double verticalSpread = 0.3 + progress * 0.9;

                spawnParticles(center, ringRadius, verticalSpread);
                pullEntities(center);

                angleOffset += Math.PI / 10.0;
                if (tick % 3 == 0) {
                    world.playSound(center, Sound.BLOCK_CONDUIT_AMBIENT_SHORT, 0.45f, 1.55f);
                }
            }

            private void spawnParticles(Location center, double ringRadius, double verticalSpread) {
                for (int i = 0; i < 20; i++) {
                    double angle = angleOffset + (i * (Math.PI * 2 / 20));
                    double x = Math.cos(angle) * ringRadius;
                    double z = Math.sin(angle) * ringRadius;
                    double y = (i % 2 == 0 ? 0.0 : 0.25) + verticalSpread;
                    Location loc = center.clone().add(x, y - 0.4, z);
                    center.getWorld().spawnParticle(Particle.REDSTONE, loc, 1, 0, 0, 0, 0, dust);
                    center.getWorld().spawnParticle(Particle.CRIT, loc, 0, 0, 0, 0);
                }
                center.getWorld().spawnParticle(Particle.PORTAL, center, 24, 0.35, 0.45, 0.35, 0.07);
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
                Location endLoc = player.getLocation().add(0, 0.7, 0);
                world.playSound(endLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.65f, 1.3f);
                world.spawnParticle(Particle.EXPLOSION_NORMAL, endLoc, 18, 0.25, 0.25, 0.25, 0.06);
            }
        };
    }
}
