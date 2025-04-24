package me.nakilex.levelplugin.spells;

import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.effects.utils.ParticleEffectUtil;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class WarriorSpell {

    public void castWarriorSpell(Player player, String effectKey) {
        switch (effectKey.toUpperCase()) {
            case "IRON_FORTRESS":
                castIronFortress(player);
                break;
            case "GROUND_SLAM":
                castGroundSlam(player);
                break;
            case "HEROIC_LEAP":
                castHeroicLeap(player);
                break;
            case "UPPERCUT":
                castUppercut(player);
                break;
            default:
                player.sendMessage("§eUnknown Warrior Spell: " + effectKey);
                break;
        }
    }

    private void castIronFortress(Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 1f);
        ParticleEffectUtil.createShieldEffect(player.getLocation(), 2, Particle.BLOCK_CRUMBLE, Material.IRON_BLOCK);

        List<ArmorStand> shields = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            ArmorStand armorStand = (ArmorStand) player.getWorld().spawnEntity(player.getLocation(), EntityType.ARMOR_STAND);
            armorStand.setInvisible(true);
            armorStand.setMarker(true);
            armorStand.setSmall(true);
            armorStand.setGravity(false);
            armorStand.setArms(true);
            armorStand.setRightArmPose(new EulerAngle(Math.toRadians(-90), Math.toRadians(0), Math.toRadians(0)));

            ItemStack shieldItem = new ItemStack(Material.SHIELD);
            armorStand.getEquipment().setItemInMainHand(shieldItem);
            shields.add(armorStand);
        }

        new BukkitRunnable() {
            double angle = 0;

            @Override
            public void run() {
                if (!player.isOnline() || shields.isEmpty()) {
                    shields.forEach(Entity::remove);
                    cancel();
                    return;
                }

                angle += Math.PI / 60;
                for (int i = 0; i < shields.size(); i++) {
                    ArmorStand shield = shields.get(i);
                    double radians = angle + (2 * Math.PI * i / shields.size());
                    double x = Math.cos(radians) * 2;
                    double z = Math.sin(radians) * 2;
                    Location shieldLocation = player.getLocation().clone().add(x, 1, z);
                    shield.teleport(shieldLocation);

                    float yaw = (float) Math.toDegrees(Math.atan2(
                        player.getLocation().getZ() - shieldLocation.getZ(),
                        player.getLocation().getX() - shieldLocation.getX())
                    );
                    shield.setRotation(yaw, 0);
                }
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 0L, 1L);

        new BukkitRunnable() {
            @Override
            public void run() {
                shields.forEach(Entity::remove);
                shields.clear();
            }
        }.runTaskLater(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 100L);

        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onEntityDamage(EntityDamageEvent event) {
                if (!(event.getEntity() instanceof Player) || !event.getEntity().equals(player)) {
                    return;
                }

                if (!shields.isEmpty()) {
                    event.setCancelled(true);
                    ArmorStand shield = shields.remove(0);
                    shield.getWorld().playSound(shield.getLocation(), Sound.ITEM_SHIELD_BREAK, 1f, 1f);
                    shield.remove();

                    if (shields.isEmpty()) {
                        HandlerList.unregisterAll(this);
                    }
                }
            }
        }, Bukkit.getPluginManager().getPlugin("LevelPlugin"));
    }

    private void castHeroicLeap(Player player) {
        double leapDistance = 15.0;
        double damageRadius = 5.0;
        double damage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 1.8;

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 1f);

        Location start = player.getLocation();
        Vector direction = start.getDirection().normalize().multiply(leapDistance);
        Vector leapVector = direction.clone().normalize().multiply(1.5);
        leapVector.setY(1.2);
        player.setVelocity(leapVector);

        player.getWorld().spawnParticle(Particle.FLAME, start, 30, 0.5, 1, 0.5);
        player.getWorld().spawnParticle(Particle.LARGE_SMOKE, start, 15, 0.5, 1, 0.5);

        new BukkitRunnable() {
            boolean hasLanded = false;

            @Override
            public void run() {
                if (!hasLanded && player.isOnGround()) {
                    hasLanded = true;

                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
                    player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 20, 0.5, 1, 0.5);
                    player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 50, 1, 0.5, 1, 0.1);
                    player.getWorld().spawnParticle(Particle.LAVA, player.getLocation(), 20, 0.5, 0.5, 0.5);

                    for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), damageRadius, damageRadius, damageRadius)) {
                        if (entity instanceof LivingEntity && entity != player) {
                            LivingEntity target = (LivingEntity) entity;
                            SpellUtils.dealWithChat(player, target, damage, "Heroic Leap");
                            Vector knockback = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(1.5);
                            knockback.setY(0.5);
                            target.setVelocity(knockback);
                        }
                    }

                    cancel();
                }
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 2L, 1L);
    }

    private void castUppercut(Player player) {
        double damage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 1.3;
        double range = 4.0;
        double knockupStrength = 1.5;
        double knockbackStrength = 0.5;

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.2f);

        Location startLocation = player.getEyeLocation();
        Vector direction = player.getLocation().getDirection().normalize();
        for (double i = 0; i <= range; i += 0.5) {
            Location slashLocation = startLocation.clone().add(direction.clone().multiply(i));
            player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, slashLocation, 1, 0.1, 0.2, 0.1, 0.01);
        }

        for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), range, range, range)) {
            if (entity instanceof LivingEntity && entity != player) {
                LivingEntity target = (LivingEntity) entity;
                if (target instanceof Player) {
                    Player pTarget = (Player) target;
                    if (!DuelManager.getInstance().areInDuel(player.getUniqueId(), pTarget.getUniqueId())) continue;
                }
                Vector toTarget = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
                double angle = direction.angle(toTarget);
                if (Math.toDegrees(angle) > 45) continue;

                SpellUtils.dealWithChat(player, target, damage, "Uppercut");
                Vector knockup = new Vector(0, knockupStrength, 0).add(direction.clone().multiply(knockbackStrength));
                target.setVelocity(knockup);

                target.getWorld().spawnParticle(Particle.CRIT, target.getLocation(), 10, 0.2, 0.2, 0.2, 0.1);
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1f);
            }
        }
    }

    private void castGroundSlam(Player player) {
        double maxRadius = 10.0;
        double damage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 1.5;
        int duration = 20;
        int steps = 10;

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
        player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 10, 0.5, 0.5, 0.5);

        new BukkitRunnable() {
            double currentRadius = 0;

            @Override
            public void run() {
                if (currentRadius >= maxRadius) {
                    cancel();
                    return;
                }

                currentRadius += maxRadius / steps;

                for (double angle = 0; angle < 360; angle += 10) {
                    double rad = Math.toRadians(angle);
                    double x = Math.cos(rad) * currentRadius;
                    double z = Math.sin(rad) * currentRadius;
                    Location rippleLocation = player.getLocation().clone().add(x, 0, z);

                    rippleLocation.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, rippleLocation, 10, 0.2, 0.2, 0.2, 0.1, Material.DIRT.createBlockData());
                    rippleLocation.getWorld().spawnParticle(Particle.CRIT, rippleLocation, 5, 0.2, 0.2, 0.2);

                    for (Entity entity : rippleLocation.getWorld().getNearbyEntities(rippleLocation, 1, 1, 1)) {
                        if (entity instanceof LivingEntity && entity != player) {
                            LivingEntity target = (LivingEntity) entity;
                            if (target instanceof Player) {
                                Player pTarget = (Player) target;
                                if (!DuelManager.getInstance().areInDuel(player.getUniqueId(), pTarget.getUniqueId())) continue;
                            }
                            SpellUtils.dealWithChat(player, target, damage, "Ground Slam");
                            Vector knockback = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(0.5);
                            knockback.setY(0.3);
                            target.setVelocity(knockback);
                        }
                    }

                    if (!rippleLocation.getBlock().isPassable()) break;
                }

                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 0.5f, 0.8f);
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 0L, duration / steps);
    }
}
