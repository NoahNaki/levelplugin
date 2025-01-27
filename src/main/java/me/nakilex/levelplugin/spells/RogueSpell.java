package me.nakilex.levelplugin.spells;

import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.effects.utils.ArmorStandEffectUtil;
import me.nakilex.levelplugin.effects.utils.ParticleEffectUtil;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.util.*;

public class RogueSpell {

    public void castRogueSpell(Player player, String effectKey) {
        switch (effectKey.toUpperCase()) {
            case "SHADOW_STEP":
                castShadowStep(player);
                break;
            case "BLADE_FURY":
                castBladeFury(player);
                break;
            case "VANISH":
                castVanish(player);
                break;
            case "DAGGER_THROW":
                castDaggerThrow(player);
                break;
            default:
                player.sendMessage("§eUnknown Rogue Spell: " + effectKey);
                break;
        }
    }

    private void castShadowStep(Player player) {

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        player.getWorld().spawnParticle(Particle.LARGE_SMOKE, player.getLocation(), 30, 0.5, 1, 0.5);

        new BukkitRunnable() {
            int casts = 0;

            @Override
            public void run() {
                if (casts >= 5) {
                    cancel();
                    return;
                }

                // Find the closest living entity within 15 blocks, excluding 'player'
                LivingEntity target = player.getWorld().getNearbyEntities(player.getLocation(), 15, 15, 15).stream()
                    .filter(e -> e instanceof LivingEntity && e != player)
                    .map(e -> (LivingEntity) e)
                    .min(Comparator.comparingDouble(e -> e.getLocation().distance(player.getLocation())))
                    .orElse(null);

                if (target != null) {
                    // --- Only apply to players if they're in a duel with the caster ---
                    if (target instanceof Player) {
                        Player pTarget = (Player) target;
                        if (!DuelManager.getInstance().areInDuel(player.getUniqueId(), pTarget.getUniqueId())) {
                            // Not in a duel, skip this iteration
                            // (No teleport behind them, no damage/effect)
                            casts++;
                            return;
                        }
                    }
                    // If it's a non-player or a Player in a duel, proceed:

                    // Teleport behind target
                    Location behindTarget = target.getLocation().clone()
                        .add(target.getLocation().getDirection().multiply(-1).normalize());
                    behindTarget.setYaw(target.getLocation().getYaw());
                    behindTarget.setPitch(target.getLocation().getPitch());
                    player.teleport(behindTarget);

                    double damage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 1.5; // 150% weapon damage
                    target.damage(damage, player);

                    target.getWorld().spawnParticle(Particle.CRIT, target.getLocation(), 30, 0.5, 1, 0.5);
                    target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation(), 10, 0.5, 0.5, 0.5);
                    target.getWorld().spawnParticle(Particle.PORTAL, target.getLocation(), 20, 0.5, 1, 0.5);

                    player.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1f);
                    player.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.5f, 1.2f);
                    player.getWorld().playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.5f);

                    // If the target is a Player, briefly apply blindness
                    if (target instanceof Player) {
                        ((Player) target).addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 0));
                    }

                    target.getWorld().spawnParticle(Particle.EXPLOSION, target.getLocation(), 10, 0.3, 0.3, 0.3);

                    // Give caster a brief glowing effect
                    player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20, 0));
                }

                // Small particle effect around the caster each cast
                player.getWorld().spawnParticle(Particle.ASH, player.getLocation(), 10, 0.3, 0.3, 0.3);

                casts++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 0L, 10L); // 10 ticks = 0.5 sec intervals
    }


    private void castBladeFury(Player player) {
        double radius = 5.0;
        double damage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 1.5; // 150% weapon damage

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation(), 30, radius, 1, radius);

        for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), radius, radius, radius)) {
            if (entity instanceof LivingEntity && entity != player) {
                LivingEntity target = (LivingEntity) entity;
                target.damage(damage, player);
            }
        }
    }

    private void castVanish(Player player) {
        int duration = 200;

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 1f, 1f);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 30, 0.5, 1, 0.5);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, duration, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 0));
    }

    private void castDaggerThrow(Player player) {
        double distance = 10.0;
        double damage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 1.5;

        Location baseLocation = player.getLocation().clone().add(0, 1.0, 0); // Chest level

        Vector forward = baseLocation.getDirection();
        forward.setY(0).normalize(); // Only horizontal movement

        Vector leftOffset = rotateAroundAxisY(forward.clone(), -90).normalize().multiply(0.7); // Adjust multiplier for more/less left
        Location adjustedLocation = baseLocation.clone().add(leftOffset);

        Location centerSpawn = adjustedLocation.clone(); // Center dagger
        Location leftSpawn = adjustedLocation.clone().add(rotateAroundAxisY(forward.clone(), -15).normalize().multiply(0.3)); // Left dagger
        Location rightSpawn = adjustedLocation.clone().add(rotateAroundAxisY(forward.clone(), 15).normalize().multiply(0.3)); // Right dagger

        ArmorStandEffectUtil.createLeadingArmorStandInDirection(centerSpawn, Material.IRON_SWORD, 22, forward, distance);
        ArmorStandEffectUtil.createLeadingArmorStandInDirection(leftSpawn, Material.IRON_SWORD, 22, rotateAroundAxisY(forward.clone(), -15).normalize(), distance);
        ArmorStandEffectUtil.createLeadingArmorStandInDirection(rightSpawn, Material.IRON_SWORD, 22, rotateAroundAxisY(forward.clone(), 15).normalize(), distance);

        for (Vector dir : new Vector[]{forward, rotateAroundAxisY(forward.clone(), -15), rotateAroundAxisY(forward.clone(), 15)}) {
            for (double i = 0; i <= distance; i += 0.5) {
                Location point = adjustedLocation.clone().add(dir.clone().multiply(i));
                point.getWorld().spawnParticle(Particle.CRIT, point, 5, 0.1, 0.1, 0.1, 0.0);

                for (Entity entity : point.getWorld().getNearbyEntities(point, 1, 1, 1)) {
                    if (entity instanceof LivingEntity && entity != player) {
                        LivingEntity target = (LivingEntity) entity;
                        target.damage(damage, player);
                    }
                }
                if (!point.getBlock().isPassable()) {
                    break;
                }
            }
        }
    }

    private Vector rotateAroundAxisY(Vector vector, double degrees) {
        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        double x = vector.getX() * cos + vector.getZ() * sin;
        double z = vector.getZ() * cos - vector.getX() * sin;
        return new Vector(x, vector.getY(), z);
    }



}
