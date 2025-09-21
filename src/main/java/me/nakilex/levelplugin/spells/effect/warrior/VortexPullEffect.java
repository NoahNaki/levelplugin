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
 * Pulls nearby entities toward the warrior while a swirling vortex of particles builds around them.
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
        world.playSound(castLocation, Sound.ITEM_TRIDENT_RETURN, 0.7f, 0.8f);
        world.playSound(castLocation, Sound.ENTITY_WITHER_AMBIENT, 0.35f, 2.0f);
        world.spawnParticle(Particle.SWEEP_ATTACK, castLocation.clone().add(0, 1.0, 0), 12, 0.25, 0.15, 0.25, 0.0);

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
                double outerRadius = Math.max(MIN_GROUND_RADIUS, AOE_RADIUS * (1.0 - progress * 0.45));
                double innerRadius = Math.max(0.6, outerRadius * 0.45 + Math.sin(progress * Math.PI * 2.2) * 0.18);

                spawnGroundRunes(groundCenter, outerRadius, innerRadius);
                spawnVortexColumns(vortexCenter.clone().add(0, -0.2, 0), outerRadius, progress);

                world.spawnParticle(Particle.CRIT_MAGIC, vortexCenter, 10, 0.35, 0.45, 0.35, 0.12);
                world.spawnParticle(Particle.PORTAL, vortexCenter, 36, outerRadius * 0.25, 0.35, outerRadius * 0.25, 0.08);
                world.spawnParticle(Particle.SMOKE_NORMAL, groundCenter, 12, outerRadius * 0.2, 0.18, outerRadius * 0.2, 0.01);

                if (tick % 4 == 0) {
                    world.playSound(groundCenter, Sound.BLOCK_BEACON_AMBIENT, 0.3f, 1.8f);
                }

                pullEntities(vortexCenter, progress);

                swirl += Math.PI / 10.0;
            }

            @Override
            protected void onEnd() {
                if (!player.isOnline()) {
                    return;
                }

                Location finish = player.getLocation().add(0, 0.65, 0);
                world.playSound(finish, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 1.4f);
                world.spawnParticle(Particle.EXPLOSION_NORMAL, finish, 22, 0.25, 0.35, 0.25, 0.05);
                world.spawnParticle(Particle.SPELL_WITCH, finish, 16, 0.35, 0.45, 0.35, 0.1);
            }

            private void spawnGroundRunes(Location groundCenter, double outerRadius, double innerRadius) {
                World runeWorld = groundCenter.getWorld();
                drawRing(groundCenter, outerRadius, 72, swirl, (point, index) -> {
                    runeWorld.spawnParticle(Particle.REDSTONE, point, 1, 0.04, 0.01, 0.04, 0, OUTER_RING_DUST);
                    if (index % 3 == 0) {
                        runeWorld.spawnParticle(Particle.SOUL, point.clone().add(0, 0.05, 0), 1, 0.01, 0.02, 0.01, 0.0);
                    }
                    if (index % 6 == 0) {
                        runeWorld.spawnParticle(Particle.SMOKE_NORMAL, point.clone().add(0, 0.12, 0), 1, 0.05, 0.02, 0.05, 0.0);
                    }
                });

                drawRing(groundCenter.clone().add(0, 0.08, 0), innerRadius, 56, -swirl * 1.3, (point, index) -> {
                    runeWorld.spawnParticle(Particle.REDSTONE, point, 1, 0.03, 0.01, 0.03, 0, INNER_RING_DUST);
                    if (index % 4 == 0) {
                        runeWorld.spawnParticle(Particle.ENCHANTMENT_TABLE, point, 1, 0.2, 0.0, 0.2, 0.0);
                    }
                });
            }

            private void spawnVortexColumns(Location center, double baseRadius, double progress) {
                World vortexWorld = center.getWorld();
                double height = 2.6 + (1.0 - progress) * 0.8;
                int arms = 4;
                double armSeparation = (Math.PI * 2) / arms;
                for (int arm = 0; arm < arms; arm++) {
                    double baseAngle = swirl + armSeparation * arm;
                    for (double y = 0; y <= height; y += 0.35) {
                        double taper = 0.55 + progress * 0.25;
                        double radius = baseRadius * Math.max(0.15, 1.0 - (y / height) * taper);
                        double spiralAngle = baseAngle + y * 1.3 + progress * Math.PI * 3.0;
                        double x = Math.cos(spiralAngle) * radius;
                        double z = Math.sin(spiralAngle) * radius;
                        Location point = center.clone().add(x, y, z);
                        vortexWorld.spawnParticle(Particle.END_ROD, point, 1, 0.0, 0.0, 0.0, 0.0);
                        if (((int) Math.round(y * 10)) % 4 == 0) {
                            vortexWorld.spawnParticle(Particle.SPELL_WITCH, point, 1, 0.0, 0.0, 0.0, 0.0);
                        }
                        if (((int) Math.round(y * 10 + arm)) % 6 == 0) {
                            vortexWorld.spawnParticle(Particle.CRIT, point, 1, 0.0, 0.0, 0.0, 0.0);
                        }
                    }
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
