package me.nakilex.levelplugin.spells;

import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.listener.ClickComboListener;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MageSpell {

    private final Map<UUID, Long> mageBasicCooldown = new HashMap<>();
    public void castMageSpell(Player player, String effectKey) {
        switch (effectKey.toUpperCase()) {
            case "METEOR":
                castMeteor(player);
                break;
            case "BLACKHOLE":
                castBlackhole(player);
                break;
            case "HEAL":
                healPlayer(player, 10);
                break;
            case "TELEPORT":
                teleportPlayer(player, 15, 150);
                break;
            case "MAGE_BASIC":
                mageBasicSkill(player);
                break;
            default:
                player.sendMessage("§eUnknown Mage Spell: " + effectKey);
                break;
        }
    }

    /** MAGE BASIC SKILL */
    private void mageBasicSkill(Player player) {
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player);
        String className = ps.playerClass.name().toLowerCase();

        if (!className.equals("mage")) {
            return;
        }

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand == null || (mainHand.getType() != Material.STICK && mainHand.getType() != Material.BLAZE_ROD)) {
            return;
        }

        UUID playerUUID = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        if (mageBasicCooldown.containsKey(playerUUID)) {
            long lastCastTime = mageBasicCooldown.get(playerUUID);
            if (currentTime - lastCastTime < 500) { // 500ms cooldown
                return;
            }
        }

        mageBasicCooldown.put(playerUUID, currentTime);
        String activeCombo = ClickComboListener.getActiveCombo(player);
        if (!activeCombo.isEmpty() && activeCombo.length() < 3) {
            return;
        }

        Location start = player.getEyeLocation();
        Vector direction = start.getDirection().normalize();
        double beamLength = 20.0;
        double damage = 6.0;

        for (double i = 0; i < beamLength; i += 0.5) {
            Location point = start.clone().add(direction.clone().multiply(i));
            player.getWorld().spawnParticle(Particle.END_ROD, point, 1, 0.1, 0.1, 0.1, 0.1);

            for (Entity entity : player.getWorld().getNearbyEntities(point, 0.5, 0.5, 0.5)) {
                if (entity instanceof LivingEntity && entity != player) {
                    LivingEntity target = (LivingEntity) entity;
                    target.damage(damage, player);
                    player.getWorld().spawnParticle(Particle.WITCH, target.getLocation(), 10, 0.2, 0.2, 0.2);
                    player.getWorld().playSound(target.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 1f, 1f);
                    return;
                }
            }

            if (!point.getBlock().isPassable()) {
                player.getWorld().playSound(point, Sound.BLOCK_GLASS_BREAK, 1f, 1f);
                break;
            }
        }

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITCH_THROW, 1f, 1f);
    }

    /** METEOR */
    private void castMeteor(Player player) {
        Location target = player.getTargetBlockExact(20) != null
            ? player.getTargetBlockExact(20).getLocation().add(0.5, 1, 0.5)
            : player.getLocation().add(player.getLocation().getDirection().normalize().multiply(5));
        double radius = 4.0; // Explosion radius
        double damage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 2.5; // 250% weapon damage

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1f);

        Location spawnLocation = target.clone().add(0, 15, 0); // Spawn 15 blocks above the target
        Fireball fireball = player.getWorld().spawn(spawnLocation, Fireball.class);
        fireball.setShooter(player); // Ensure the fireball is associated with the player
        fireball.setVelocity(new Vector(0, -1.5, 0)); // Apply downward velocity
        fireball.setIsIncendiary(false); // Prevent fire spread

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!fireball.isValid()) {
                    cancel();
                    return;
                }

                fireball.getWorld().spawnParticle(Particle.FLAME, fireball.getLocation(), 10, 0.3, 0.3, 0.3, 0.02);
                fireball.getWorld().spawnParticle(Particle.LARGE_SMOKE, fireball.getLocation(), 5, 0.2, 0.2, 0.2, 0.02);

                fireball.getWorld().playSound(fireball.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.5f, 1f);
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 0L, 2L);

        fireball.setMetadata("Meteor", new FixedMetadataValue(Bukkit.getPluginManager().getPlugin("LevelPlugin"), true));
        fireball.setYield((float) radius); // Set explosion radius
        fireball.setGravity(true);

        fireball.setMetadata("ExplosionLogic", new FixedMetadataValue(Bukkit.getPluginManager().getPlugin("LevelPlugin"), true));

        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onProjectileHit(org.bukkit.event.entity.ProjectileHitEvent event) {
                if (!(event.getEntity() instanceof Fireball) || !event.getEntity().hasMetadata("Meteor")) return;
                Fireball fireball = (Fireball) event.getEntity();

                fireball.getWorld().spawnParticle(Particle.EXPLOSION, fireball.getLocation(), 1);
                fireball.getWorld().playSound(fireball.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);

                for (Entity entity : fireball.getWorld().getNearbyEntities(fireball.getLocation(), radius, radius, radius)) {
                    if (entity instanceof LivingEntity && entity != player) {
                        LivingEntity targetEntity = (LivingEntity) entity;
                        targetEntity.damage(damage, player); // Apply damage
                        targetEntity.setFireTicks(100); // Ignite for 5 seconds
                    }
                }
                fireball.remove();
            }
        }, Bukkit.getPluginManager().getPlugin("LevelPlugin"));
    }

    /** BLACKHOLE */
    private void castBlackhole(Player player) {
        Location target = player.getEyeLocation().add(player.getLocation().getDirection().multiply(10));
        double pullRadius = 5.0;
        double damageRadius = 1.0;
        double damage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 0.1;
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 1f, 1f);

        createBlackholeEffect(target, pullRadius);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks++ >= 50) {
                    cancel();
                    return;
                }
                for (Entity entity : player.getWorld().getNearbyEntities(target, pullRadius, pullRadius, pullRadius)) {
                    if (entity instanceof LivingEntity && entity != player) {
                        LivingEntity targetEntity = (LivingEntity) entity;
                        Vector pullVector = target.toVector()
                            .subtract(targetEntity.getLocation().toVector())
                            .normalize().multiply(0.2);
                        targetEntity.setVelocity(pullVector);
                        if (targetEntity.getLocation().distance(target) <= damageRadius) {
                            targetEntity.damage(damage, player);
                            targetEntity.getWorld().spawnParticle(Particle.CRIT,
                                targetEntity.getLocation(), 10, 0.2, 0.2, 0.2);
                        }
                    }
                }
                player.getWorld().spawnParticle(Particle.WITCH, target, 10, 0.5, 0.5, 0.5, 0.1);
                player.getWorld().playSound(target, Sound.BLOCK_BEACON_AMBIENT, 0.5f, 1.2f);
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 0L, 2L);
    }

    private void createBlackholeEffect(Location center, double radius) {
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks++ >= 50) {
                    cancel();
                    return;
                }
                for (double angle = 0; angle < 360; angle += 10) {
                    double radians = Math.toRadians(angle);
                    double x = radius * Math.cos(radians);
                    double z = radius * Math.sin(radians);
                    Location particleLocation = center.clone().add(x, 0, z);

                    // Make the ring move up/down slightly
                    particleLocation.add(0, Math.sin(ticks / 10.0) * 0.5, 0);

                    center.getWorld().spawnParticle(Particle.PORTAL, particleLocation, 1, 0, 0, 0, 0);
                    center.getWorld().spawnParticle(Particle.SMOKE, particleLocation, 1, 0, 0, 0, 0);
                }
                center.getWorld().spawnParticle(Particle.DRAGON_BREATH, center, 5, 0.2, 0.2, 0.2, 0.02);
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 0L, 2L);
    }

    /** HEAL */
    private void healPlayer(Player player, int amount) {
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        double newHealth = Math.min(player.getHealth() + amount, maxHealth);
        player.setHealth(newHealth);
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation(), 30, 1, 1, 1);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 1f, 1f);
    }

    /** TELEPORT */
    private void teleportPlayer(Player player, int distance, int particles) {
        Location target = player.getLocation().add(player.getLocation().getDirection().multiply(distance));
        Location safeLocation = findSafeLocation(target, player);

        if (safeLocation != null) {
            player.getWorld().spawnParticle(Particle.DRAGON_BREATH, player.getLocation(), particles, 0.5, 1, 0.5);
            player.teleport(safeLocation);
            player.getWorld().spawnParticle(Particle.DRAGON_BREATH, safeLocation, particles, 0.5, 1, 0.5);
            player.getWorld().playSound(safeLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        } else {
            player.sendMessage("§cTeleportation failed! Destination is unsafe.");
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1f, 1f);
        }
    }

    private Location findSafeLocation(Location target, Player player) {
        for (int i = -1; i <= 1; i++) {
            Location tempLocation = target.clone().add(0, i, 0);
            if (ClickComboListener.isLocTpSafe(tempLocation)) {
                return tempLocation;
            }
        }
        return null;
    }

}
