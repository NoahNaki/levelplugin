package me.nakilex.levelplugin.spells.effect.warrior;

import java.util.function.BiConsumer;

import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.context.SpellCastContextCompat;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.utils.animation.SpellAnimation;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Pulls nearby entities toward the warrior while concentric ground rings collapse inward.
 */
public class VortexPullEffect implements SpellEffect {
    private static final double AOE_RADIUS = 5.0;
    private static final double AOE_HEIGHT = 3.0;
    private static final double MIN_GROUND_RADIUS = 1.1;
    private static final int    ANIMATION_STEPS = 28;
    private static final int    INTERVAL_TICKS = 2;

    private static final Particle.DustOptions OUTER_RING_DUST =
        new Particle.DustOptions(Color.fromRGB(78, 17, 17), 1.35f);
    private static final Particle.DustOptions INNER_RING_DUST =
        new Particle.DustOptions(Color.fromRGB(158, 46, 46), 0.95f);

    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null || !player.isOnline()) {
            return;
        }

        SpellCastContextCompat.markSuccess(ctx, true);

        World world = player.getWorld();
        Location castLocation = player.getLocation();
        world.playSound(castLocation, Sound.ITEM_TRIDENT_RETURN, 0.6f, 1.25f);
        world.playSound(castLocation, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.65f, 1.1f);

        new SpellAnimation(INTERVAL_TICKS, ANIMATION_STEPS) {
            private double swirl = 0.0;

            @Override
            protected void onTick(int tick) {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                Location currentFeet = player.getLocation();
                Location groundCenter = currentFeet.clone().add(0, -0.12, 0);
                Location vortexCenter = currentFeet.clone().add(0, 0.55, 0);

                double progress = ANIMATION_STEPS <= 1 ? 1.0 : tick / (double) (ANIMATION_STEPS - 1);
                double outerRadius = Math.max(MIN_GROUND_RADIUS,
                    AOE_RADIUS - (AOE_RADIUS - MIN_GROUND_RADIUS) * progress);

                spawnGroundCircles(groundCenter, outerRadius, progress);

                if (tick % 5 == 0) {
                    world.playSound(groundCenter, Sound.BLOCK_BEACON_AMBIENT, 0.22f, 1.65f);
                }

                pullEntities(vortexCenter, progress);

                swirl += Math.PI / 16.0;
            }

            @Override
            protected void onEnd() {
                if (!player.isOnline()) {
                    return;
                }

                Location finish = player.getLocation().add(0, 0.65, 0);
                world.playSound(finish, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.5f, 1.5f);
                world.spawnParticle(Particle.SPELL_WITCH, finish, 10, 0.25, 0.25, 0.25, 0.08);
            }

            private void spawnGroundCircles(Location groundCenter, double outerRadius, double progress) {
                World runeWorld = groundCenter.getWorld();
                double middleRadius = Math.max(0.75, outerRadius * 0.7);
                double innerRadius = Math.max(0.45, outerRadius * 0.45);

                drawRing(groundCenter, outerRadius, 70, swirl, (point, index) -> {
                    runeWorld.spawnParticle(Particle.REDSTONE, point, 1, 0.025, 0.0, 0.025, 0, OUTER_RING_DUST);
                    if (index % 7 == 0) {
                        runeWorld.spawnParticle(Particle.CRIT_MAGIC, point.clone().add(0, 0.05, 0), 1, 0.0, 0.0, 0.0, 0.0);
                    }
                });

                drawRing(groundCenter.clone().add(0, 0.02, 0), middleRadius, 58, -swirl * 0.6, (point, index) -> {
                    runeWorld.spawnParticle(Particle.REDSTONE, point, 1, 0.02, 0.0, 0.02, 0, INNER_RING_DUST);
                    if (index % 9 == 0) {
                        runeWorld.spawnParticle(Particle.ENCHANTMENT_TABLE, point.clone().add(0, 0.1, 0), 1, 0.0, 0.0, 0.0, 0.0);
                    }
                });

                drawRing(groundCenter.clone().add(0, 0.04, 0), innerRadius, 46, swirl * 1.1, (point, index) -> {
                    runeWorld.spawnParticle(Particle.REDSTONE, point, 1, 0.015, 0.0, 0.015, 0, INNER_RING_DUST);
                    if (index % 2 == 0) {
                        runeWorld.spawnParticle(Particle.SPELL_WITCH, point.clone().add(0, 0.05, 0), 1, 0.0, 0.0, 0.0, 0.0);
                    }
                });

                int rays = 6;
                double collapseRadius = Math.max(innerRadius * 0.6,
                    outerRadius * (0.25 + 0.2 * (1.0 - progress)));
                for (int ray = 0; ray < rays; ray++) {
                    double angle = swirl * 0.5 + (Math.PI * 2.0 / rays) * ray;
                    double x = Math.cos(angle) * collapseRadius;
                    double z = Math.sin(angle) * collapseRadius;
                    Location point = groundCenter.clone().add(x, 0.03, z);
                    runeWorld.spawnParticle(Particle.CRIT, point, 1, 0.02, 0.0, 0.02, 0.0);
                }
            }

            private void drawRing(Location center, double radius, int samples, double angleOffset,
                                   BiConsumer<Location, Integer> pointConsumer) {
                Location base = center.clone();
                double increment = (Math.PI * 2) / samples;
                for (int i = 0; i < samples; i++) {
                    double angle = angleOffset + increment * i;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    Location point = base.clone().add(x, 0, z);
                    pointConsumer.accept(point, i);
                }
            }

            private void pullEntities(Location center, double progress) {
                World pullWorld = center.getWorld();
                for (Entity entity : pullWorld.getNearbyEntities(center, AOE_RADIUS, AOE_HEIGHT, AOE_RADIUS)) {
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

                    double closeness = 1.0 - Math.min(1.0, distance / AOE_RADIUS);
                    double pullStrength = 0.26 + progress * 0.12 + closeness * 0.5;
                    Vector velocity = delta.normalize().multiply(pullStrength);
                    double verticalBoost = 0.12 + closeness * 0.25 + progress * 0.1;
                    velocity.setY(Math.min(0.55, Math.max(velocity.getY(), verticalBoost)));
                    living.setVelocity(living.getVelocity().multiply(0.25).add(velocity));
                }
            }
        };
    }
}
