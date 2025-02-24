package me.nakilex.levelplugin.spells;

import me.nakilex.levelplugin.duels.managers.DuelManager;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ArcherSpell {

    public void castArcherSpell(Player player, String effectKey) {
        switch (effectKey.toUpperCase()) {
            case "POWER_SHOT":
                //castPowerShot(player);
                break;
            case "ARROW_STORM":
                castArrowStorm(player);
                break;
            case "EXPLOSIVE_ARROW":
                //castExplosiveArrow(player);
                break;
            case "GRAPPLE_HOOK":
                castGrappleHook(player);
                break;
            default:
                player.sendMessage("§eUnknown Archer Spell: " + effectKey);
                break;
        }
    }

    private void castPowerShot(Player player) {
        double radius = 5.0;
        double damage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 0.8; // 80% weapon damage per arrow
        int duration = 100;
        int arrowCount = 30;

        Location targetLocation = player.getTargetBlockExact(20) != null
            ? player.getTargetBlockExact(20).getLocation().add(0.5, 0.5, 0.5)
            : player.getLocation().add(player.getLocation().getDirection().normalize().multiply(5));

        player.getWorld().playSound(targetLocation, Sound.ENTITY_ARROW_SHOOT, 1f, 1f);

        new BukkitRunnable() {
            int arrowsSpawned = 0;

            @Override
            public void run() {
                if (arrowsSpawned >= arrowCount) {
                    cancel();
                    return;
                }

                double xOffset = (Math.random() - 0.5) * 2 * radius;
                double zOffset = (Math.random() - 0.5) * 2 * radius;
                Location dropLocation = targetLocation.clone().add(xOffset, 15, zOffset); // Increased spawn height

                dropLocation.getWorld().spawnParticle(Particle.CLOUD, dropLocation, 10, 0.3, 0.3, 0.3, 0.02);
                dropLocation.getWorld().playSound(dropLocation, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.5f, 1.5f);

                Arrow arrow = player.getWorld().spawnArrow(dropLocation, new Vector(0, -3, 0), 1.5f, 0.0f); // Increased velocity
                arrow.setCustomName("ArrowRain");
                arrow.setCustomNameVisible(false);
                arrow.setShooter(player);
                arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED); // Prevent pickup

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!arrow.isValid() || arrow.isDead()) {
                            cancel();
                            return;
                        }

                        for (Entity entity : arrow.getNearbyEntities(1, 1, 1)) {
                            if (entity instanceof LivingEntity && entity != player) {
                                // Duel check: if the entity is a Player, ensure they're in a duel with the caster.
                                if (entity instanceof Player) {
                                    if (!DuelManager.getInstance().areInDuel(player.getUniqueId(), ((Player) entity).getUniqueId())) {
                                        continue; // Skip if not in a duel.
                                    }
                                }

                                LivingEntity target = (LivingEntity) entity;
                                target.damage(damage, player);
                                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1)); // Slow for 2 seconds
                            }
                        }
                    }
                }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 0L, 1L);
                arrowsSpawned++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 0L, duration / arrowCount); // Spread out arrow spawns
    }




    private void castExplosiveArrow(Player player) {
        double explosionRadius = 5.0;
        double damage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 2.0; // 200% weapon damage

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1f, 1f);

        Arrow arrow = player.launchProjectile(Arrow.class);
        arrow.setVelocity(player.getLocation().getDirection().multiply(2));
        arrow.setCustomName("ExplosiveArrow");
        arrow.setCustomNameVisible(false);
        arrow.setCritical(true);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!arrow.isValid() || arrow.isDead()) {
                    cancel();
                    return;
                }

                arrow.getWorld().spawnParticle(Particle.CRIT, arrow.getLocation(), 5, 0.1, 0.1, 0.1);

                if (arrow.isOnGround() || !arrow.getLocation().getBlock().isPassable()) {
                    Location explosionLocation = arrow.getLocation();

                    // Create explosion particle effect and sound
                    arrow.getWorld().spawnParticle(Particle.EXPLOSION, explosionLocation, 10, 1, 1, 1);
                    arrow.getWorld().playSound(explosionLocation, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);

                    // Create firework effect
                    Firework firework = (Firework) arrow.getWorld().spawnEntity(explosionLocation, EntityType.FIREWORK_ROCKET);
                    FireworkMeta fireworkMeta = firework.getFireworkMeta();
                    fireworkMeta.addEffect(FireworkEffect.builder()
                        .withColor(Color.ORANGE, Color.RED, Color.YELLOW)
                        .withFade(Color.BLACK)
                        .with(FireworkEffect.Type.BALL_LARGE)
                        .trail(true)
                        .flicker(true)
                        .build());
                    fireworkMeta.setPower(1);
                    firework.setFireworkMeta(fireworkMeta);
                    firework.detonate();

                    // Damage nearby entities
                    for (Entity entity : arrow.getWorld().getNearbyEntities(explosionLocation, explosionRadius, explosionRadius, explosionRadius)) {
                        if (entity instanceof LivingEntity && entity != player) {
                            LivingEntity target = (LivingEntity) entity;
                            target.damage(damage, player); // Apply damage
                            target.setFireTicks(60); // Ignite target for 3 seconds
                        }
                    }
                    arrow.remove();
                    cancel();
                }
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 0L, 1L);
    }

    private final Set<UUID> grappleCooldown = new HashSet<>();

    private void castGrappleHook(Player player) {
        if (grappleCooldown.contains(player.getUniqueId())) {
            return;
        }

        grappleCooldown.add(player.getUniqueId()); // Add player to cooldown

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_PEARL_THROW, 1f, 1f);

        Snowball projectile = player.launchProjectile(Snowball.class);
        projectile.setVelocity(player.getLocation().getDirection().multiply(2));
        projectile.setCustomName("GrappleHook");
        projectile.setCustomNameVisible(false);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!projectile.isValid() || projectile.isDead()) {
                    cancel();
                    return;
                }

                projectile.getWorld().spawnParticle(Particle.WITCH, projectile.getLocation(), 5, 0.1, 0.1, 0.1);

                // Check for collision
                for (Entity entity : projectile.getNearbyEntities(1, 1, 1)) {
                    if (entity instanceof LivingEntity || projectile.getLocation().getBlock().getType() != Material.AIR) {
                        // Pull player toward the target location
                        Location targetLocation = projectile.getLocation();
                        Vector direction = targetLocation.toVector().subtract(player.getLocation().toVector()).normalize().multiply(1.5);

                        player.setVelocity(direction.add(new Vector(0, 0.5, 0))); // Horizontal and upward boost

                        // Apply glide and slam detection
                        handleGlideAndSlam(player);

                        projectile.remove();
                        cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 0L, 1L);
    }

    private void handleGlideAndSlam(Player player) {
        new BukkitRunnable() {
            boolean slamTriggered = false;

            @Override
            public void run() {
                if (player.isOnGround()) {
                    grappleCooldown.remove(player.getUniqueId());
                    if (slamTriggered) {
                        performSlam(player);
                    }
                    cancel();
                    return;
                }

                if (slamTriggered) {
                    Vector downwardVelocity = player.getVelocity();
                    downwardVelocity.setY(Math.max(downwardVelocity.getY() - 0.5, -2.5));
                    player.setVelocity(downwardVelocity);
                } else {
                    Vector glide = player.getVelocity().multiply(0.9);
                    glide.setY(Math.max(player.getVelocity().getY() - 0.05, -0.1));
                    player.setVelocity(glide);

                    if (player.isSneaking()) {
                        slamTriggered = true;
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 1f, 0.8f);
                        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation(), 20, 0.5, 1, 0.5);
                        player.setVelocity(new Vector(0, -2, 0));
                    }
                }
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 10L, 1L);
    }

    private void performSlam(Player player) {
        double radius = 5.0;
        double damage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 2.0; // 200% weapon damage

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
        player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 20);

        for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), radius, radius, radius)) {
            if (entity instanceof LivingEntity && entity != player) {
                LivingEntity target = (LivingEntity) entity;
                target.damage(damage, player);
                Vector knockback = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(1.5);
                knockback.setY(0.5);
                target.setVelocity(knockback);
            }
        }
    }

    private void castArrowStorm(Player player) {
        int arrowCount = 10;
        double spread = 0.1;
        double damage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 0.5; // 50% weapon damage per arrow

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1f, 1f);

        new BukkitRunnable() {
            int arrowsFired = 0;
            @Override
            public void run() {
                if (arrowsFired >= arrowCount) {
                    cancel();
                    return;
                }
                Arrow arrow = player.launchProjectile(Arrow.class);
                arrow.setDamage(0);  // Prevent default damage
                Vector direction = player.getLocation().getDirection().clone();
                direction.add(new Vector((Math.random() - 0.5) * spread, (Math.random() - 0.5) * spread, (Math.random() - 0.5) * spread));
                arrow.setVelocity(direction.multiply(2));
                arrow.setCustomName("ArrowStorm");
                arrow.setCustomNameVisible(false);
                arrow.setCritical(true);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!arrow.isValid() || arrow.isDead()) {
                            cancel();
                            return;
                        }
                        arrow.getWorld().spawnParticle(Particle.CRIT, arrow.getLocation(), 5, 0.1, 0.1, 0.1);
                        for (Entity entity : arrow.getNearbyEntities(1, 1, 1)) {
                            if (entity instanceof LivingEntity && entity != player) {
                                if (entity instanceof Player) {
                                    if (!DuelManager.getInstance().areInDuel(player.getUniqueId(), entity.getUniqueId())) {
                                        continue; // Skip if not in a duel.
                                    }
                                }
                                LivingEntity target = (LivingEntity) entity;
                                target.damage(damage, player);
                                arrow.remove();
                                cancel();
                                return;
                            }
                        }
                    }
                }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 0L, 1L);
                arrowsFired++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 0L, 5L);
    }
}
