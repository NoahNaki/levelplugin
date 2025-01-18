package me.nakilex.levelplugin.spells;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import me.nakilex.levelplugin.player.listener.ClickComboListener;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Snowball;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.Bukkit;

import java.util.Comparator;
import java.util.List;

/**
 * Represents a single Spell with its combo, name, mana cost, cooldown, effect logic, etc.
 */
public class Spell {

    private final String id;             // e.g. "ground_slam"
    private final String displayName;    // e.g. "Ground Slam"
    private final String combo;          // e.g. "RLR"
    private final int manaCost;
    private final int cooldown;          // in seconds
    private final int levelReq;
    private final List<Material> allowedWeapons; // which weapon materials are valid (e.g. all SHOVELS for warrior)

    // A short descriptor of the effect logic: e.g. "GROUND_SLAM", "SHIELD_WALL", etc.
    private final String effectKey;
    private final double damageMultiplier;  // % weapon damage, e.g. 1.5 = 150%

    public Spell(
        String id,
        String displayName,
        String combo,
        int manaCost,
        int cooldown,
        int levelReq,
        List<Material> allowedWeapons,
        String effectKey,
        double damageMultiplier
    ) {
        this.id = id;
        this.displayName = displayName;
        this.combo = combo;
        this.manaCost = manaCost;
        this.cooldown = cooldown;
        this.levelReq = levelReq;
        this.allowedWeapons = allowedWeapons;
        this.effectKey = effectKey;
        this.damageMultiplier = damageMultiplier;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getCombo() { return combo; }
    public int getManaCost() { return manaCost; }
    public int getCooldown() { return cooldown; }
    public int getLevelReq() { return levelReq; }
    public List<Material> getAllowedWeapons() { return allowedWeapons; }
    public String getEffectKey() { return effectKey; }
    public double getDamageMultiplier() { return damageMultiplier; }

    /**
     * Actually apply the effect logic to the player (and possibly nearby enemies).
     * In a real scenario, you'd add more custom logic for AoE, damage, etc.
     */
    public void castEffect(Player player) {
        // For demonstration, we do simple logic based on effectKey
        switch(effectKey.toUpperCase()) {
            // Warrior
            case "GROUND_SLAM": {
                castGroundSlam(player);
                break;
            }
            case "SHIELD_WALL": {
                castShieldWall(player);
                break;
            }
            case "BATTLE_CRY": {
                castBattleCry(player);
                break;
            }
            case "CHARGE": {
                castCharge(player);
                break;
            }

            case "METEOR":
                castMeteorStrike(player);
                break;
            case "BLACKHOLE":
                createBlackHole(player);
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

            // Rogue
            case "SHADOW_STEP": {
                castShadowStep(player);
                break;
            }
            case "BLADE_FURY": {
                castBladeFury(player);
                break;
            }
            case "SMOKE_BOMB": {
                castSmokeBomb(player);
                break;
            }
            case "VANISH": {
                castVanish(player);
                break;
            }

            // Archer
            case "POWER_SHOT": {
                castPowerShot(player);
                break;
            }
            case "EXPLOSIVE_ARROW": {
                castExplosiveArrow(player);
                break;
            }
            case "GRAPPLE_HOOK": {
                castGrappleHook(player);
                break;
            }
            case "ARROW_STORM": {
                castArrowStorm(player);
                break;
            }

            default: {
                player.sendMessage("§eYou cast " + displayName + " (no effectKey logic)!");
            }
        }
    }

    private void spawnAoEParticles(Location center, int radius, Particle particle) {
        for (int i = 0; i < 50; i++) {
            double x = center.getX() + (Math.random() - 0.5)*2*radius;
            double y = center.getY() + 0.5;
            double z = center.getZ() + (Math.random() - 0.5)*2*radius;
            center.getWorld().spawnParticle(particle, x, y, z, 0, 0,0,0, 1);
        }
    }

    //MAGE

    private void mageBasicSkill(Player player) {
        // Check if the player is in the middle of a combo
        String activeCombo = ClickComboListener.getActiveCombo(player);

        // Allow left-click to be used as part of the combo instead of triggering the basic attack
        if (!activeCombo.isEmpty() && activeCombo.length() < 3) {
            return; // Let the input contribute to the combo instead
        }

        // If no combo is active, proceed with basic attack
        Snowball projectile = player.launchProjectile(Snowball.class);
        projectile.setVelocity(player.getLocation().getDirection().multiply(2));
        projectile.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITCH_THROW, 1f, 1f);
        projectile.getWorld().spawnParticle(Particle.CRIT, projectile.getLocation(), 20, 0.2, 0.2, 0.2);

        projectile.setCustomName("MageBasicSkill");
        projectile.setCustomNameVisible(false);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!projectile.isValid() || projectile.isDead()) {
                    cancel();
                    return;
                }
                projectile.getWorld().spawnParticle(Particle.CRIT, projectile.getLocation(), 5, 0.1, 0.1, 0.1);
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 0L, 1L);
    }


    private void castMeteorStrike(Player player) {
        Location target = player.getTargetBlock(null, 100).getLocation().add(0.5, 1, 0.5);
        player.getWorld().spawnParticle(Particle.FLAME, target, 50, 1, 1, 1);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1f);

        new BukkitRunnable() {
            @Override
            public void run() {
                player.getWorld().createExplosion(target, 3.0f, false, false);
            }
        }.runTaskLater(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 40L);
    }

    private void createBlackHole(Player player) {
        Location target = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(5));
        player.getWorld().spawnParticle(Particle.PORTAL, target, 100, 1, 1, 1);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks++ > 100) {
                    cancel();
                    return;
                }
                for (Entity entity : player.getWorld().getNearbyEntities(target, 5, 5, 5)) {
                    if (entity instanceof LivingEntity && entity != player) {
                        Vector pull = target.toVector().subtract(entity.getLocation().toVector()).normalize().multiply(0.2);
                        entity.setVelocity(pull);
                    }
                }
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 0L, 1L);
    }

    private void healPlayer(Player player, int amount) {
        double newHealth = Math.min(player.getHealth() + amount, player.getAttribute(Attribute.MAX_HEALTH).getValue());
        player.setHealth(newHealth);
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation(), 30, 1, 1, 1);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 1f, 1f);
    }

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
            if (ClickComboListener.isLocTpSafe(tempLocation)) { // Reuse method from ClickComboListener
                return tempLocation;
            }
        }
        return null;
    }

    // Inside the Spell class

    private void castGroundSlam(Player player) {
        double radius = 5.0;
        double damage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 1.5; // 150% weapon damage

        spawnAoEParticles(player.getLocation(), (int) radius, Particle.EXPLOSION);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.8f);

        player.sendMessage("§eYou slam the ground, dealing AoE damage and knocking back enemies!");

        // Damage and knockback nearby entities
        for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), radius, radius, radius)) {
            if (entity instanceof LivingEntity && entity != player) {
                LivingEntity target = (LivingEntity) entity;
                target.damage(damage, player); // Apply damage
                Vector knockback = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(1.5);
                knockback.setY(0.5); // Add upward knockback
                target.setVelocity(knockback);
            }
        }
    }

    private void castShieldWall(Player player) {
        int duration = 200; // 10 seconds (200 ticks)
        double damageReduction = 0.5; // 50% damage reduction

        player.sendMessage("§eYou raise a shield wall, reducing damage for 10 seconds!");

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 1f);
        player.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, player.getLocation(), 50, 1, 1, 1, Material.SHIELD.createBlockData());

        // Apply temporary damage resistance (logic can be extended)
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks++ >= duration) {
                    cancel();
                    player.sendMessage("§cShield Wall has ended.");
                }
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 0L, 20L);
    }

    private void castBattleCry(Player player) {
        double buffMultiplier = 1.2; // 20% damage increase
        int duration = 200; // 10 seconds (200 ticks)

        player.sendMessage("§eYou unleash a Battle Cry, buffing your allies’ damage!");

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 1f);
        player.getWorld().spawnParticle(Particle.SONIC_BOOM, player.getLocation(), 30, 1, 1, 1);

        // Buff the player and nearby allies (logic for actual stat buff to be implemented)
        for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), 10, 10, 10)) {
            if (entity instanceof Player) {
                Player ally = (Player) entity;
                ally.sendMessage("§eYou feel empowered by the Battle Cry!");
                // Placeholder for adding a temporary stat buff
            }
        }
    }

    private void castCharge(Player player) {
        Vector chargeVelocity = player.getLocation().getDirection().normalize().multiply(2);
        double damage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 1.2; // 120% weapon damage

        player.setVelocity(chargeVelocity);
        player.sendMessage("§eYou charge forward, damaging enemies in your path!");

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_HORSE_GALLOP, 1f, 1f);
        player.getWorld().spawnParticle(Particle.LARGE_SMOKE, player.getLocation(), 20, 0.5, 0.5, 0.5);

        new BukkitRunnable() {
            @Override
            public void run() {
                // Check for entities in the charge path
                for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), 1, 1, 1)) {
                    if (entity instanceof LivingEntity && entity != player) {
                        LivingEntity target = (LivingEntity) entity;
                        target.damage(damage, player); // Apply damage
                        target.setVelocity(player.getLocation().getDirection().normalize().multiply(1.5));
                    }
                }
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 0L, 2L);
    }

    private void castPowerShot(Player player) {
        double damage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 2.0; // 200% weapon damage
        double knockbackStrength = 2.0;

        player.sendMessage("§eYou unleash a Power Shot!");
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1f, 0.8f);

        // Launch a projectile
        Snowball projectile = player.launchProjectile(Snowball.class);
        projectile.setVelocity(player.getLocation().getDirection().multiply(2));
        projectile.setCustomName("PowerShot");
        projectile.setCustomNameVisible(false);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!projectile.isValid() || projectile.isDead()) {
                    cancel();
                    return;
                }

                projectile.getWorld().spawnParticle(Particle.CRIT, projectile.getLocation(), 10, 0.1, 0.1, 0.1);

                // Check for collisions
                for (Entity entity : projectile.getNearbyEntities(1, 1, 1)) {
                    if (entity instanceof LivingEntity && entity != player) {
                        LivingEntity target = (LivingEntity) entity;
                        target.damage(damage, player); // Apply damage
                        target.setVelocity(projectile.getVelocity().normalize().multiply(knockbackStrength)); // Knockback
                        projectile.remove();
                        cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 0L, 1L);
    }

    private void castExplosiveArrow(Player player) {
        double radius = 3.0;
        double damage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 1.5; // 150% weapon damage

        player.sendMessage("§eYou shoot an Explosive Arrow!");
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 1f);

        // Launch a projectile
        Snowball projectile = player.launchProjectile(Snowball.class);
        projectile.setVelocity(player.getLocation().getDirection().multiply(2));
        projectile.setCustomName("ExplosiveArrow");
        projectile.setCustomNameVisible(false);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!projectile.isValid() || projectile.isDead()) {
                    cancel();
                    return;
                }

                // Check for collision
                for (Entity entity : projectile.getNearbyEntities(1, 1, 1)) {
                    if (entity instanceof LivingEntity && entity != player) {
                        LivingEntity target = (LivingEntity) entity;
                        // Explode on impact
                        projectile.getWorld().spawnParticle(Particle.EXPLOSION, projectile.getLocation(), 20);
                        projectile.getWorld().playSound(projectile.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);

                        // Damage all entities in the radius
                        for (Entity nearby : projectile.getWorld().getNearbyEntities(projectile.getLocation(), radius, radius, radius)) {
                            if (nearby instanceof LivingEntity && nearby != player) {
                                ((LivingEntity) nearby).damage(damage, player);
                            }
                        }
                        projectile.remove();
                        cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 0L, 1L);
    }


    private void castGrappleHook(Player player) {
        player.sendMessage("§eYou fire a Grapple Hook!");
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

                projectile.getWorld().spawnParticle(Particle.DOLPHIN, projectile.getLocation(), 5, 0.1, 0.1, 0.1);

                // Check for collision
                for (Entity entity : projectile.getNearbyEntities(1, 1, 1)) {
                    if (entity instanceof LivingEntity || projectile.getLocation().getBlock().getType() != Material.AIR) {
                        // Pull player to the location
                        Location targetLocation = projectile.getLocation();
                        player.teleport(targetLocation.add(0, 1, 0)); // Slight offset for safety
                        projectile.remove();
                        cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 0L, 1L);
    }

    private void castArrowStorm(Player player) {
        int arrowCount = 10;
        double spread = 0.2;
        double damage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 0.5; // 50% weapon damage per arrow

        player.sendMessage("§eYou unleash an Arrow Storm!");
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1f, 1f);

        for (int i = 0; i < arrowCount; i++) {
            Snowball projectile = player.launchProjectile(Snowball.class);
            Vector direction = player.getLocation().getDirection().clone();
            direction.add(new Vector((Math.random() - 0.5) * spread, (Math.random() - 0.5) * spread, (Math.random() - 0.5) * spread));
            projectile.setVelocity(direction.multiply(2));
            projectile.setCustomName("ArrowStorm");
            projectile.setCustomNameVisible(false);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!projectile.isValid() || projectile.isDead()) {
                        cancel();
                        return;
                    }

                    projectile.getWorld().spawnParticle(Particle.CRIT, projectile.getLocation(), 5, 0.1, 0.1, 0.1);

                    // Check for collisions
                    for (Entity entity : projectile.getNearbyEntities(1, 1, 1)) {
                        if (entity instanceof LivingEntity && entity != player) {
                            LivingEntity target = (LivingEntity) entity;
                            target.damage(damage, player); // Apply damage
                            projectile.remove();
                            cancel();
                            return;
                        }
                    }
                }
            }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 0L, 1L);
        }
    }

    private void castShadowStep(Player player) {
        player.sendMessage("§eYou vanish and appear behind your enemy with a critical strike!");

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        player.getWorld().spawnParticle(Particle.LARGE_SMOKE, player.getLocation(), 20, 0.5, 1, 0.5);

        // Find the closest enemy
        LivingEntity target = player.getWorld().getNearbyEntities(player.getLocation(), 10, 10, 10).stream()
            .filter(e -> e instanceof LivingEntity && e != player)
            .map(e -> (LivingEntity) e)
            .min(Comparator.comparingDouble(e -> e.getLocation().distance(player.getLocation())))
            .orElse(null);

        if (target != null) {
            // Teleport behind the target
            Location behindTarget = target.getLocation().clone().add(target.getLocation().getDirection().multiply(-1).normalize());
            behindTarget.setYaw(target.getLocation().getYaw());
            behindTarget.setPitch(target.getLocation().getPitch());
            player.teleport(behindTarget);

            // Perform a critical strike
            double damage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 1.3; // 130% weapon damage
            target.damage(damage, player);

            // Visuals
            target.getWorld().spawnParticle(Particle.CRIT, target.getLocation(), 20, 0.5, 1, 0.5);
            player.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1f);
        } else {
            player.sendMessage("§cNo target found nearby!");
        }
    }

    private void castBladeFury(Player player) {
        double radius = 5.0;
        double damage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 1.5; // 150% weapon damage

        player.sendMessage("§eYou spin wildly, hitting all enemies around you!");
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation(), 30, radius, 1, radius);

        // Damage all nearby entities
        for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), radius, radius, radius)) {
            if (entity instanceof LivingEntity && entity != player) {
                LivingEntity target = (LivingEntity) entity;
                target.damage(damage, player); // Apply damage
            }
        }
    }


    private void castSmokeBomb(Player player) {
        double radius = 5.0;

        player.sendMessage("§eYou toss a smoke bomb, blinding enemies!");
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_TNT_PRIMED, 1f, 1f);
        player.getWorld().spawnParticle(Particle.LARGE_SMOKE, player.getLocation(), 50, radius, 1, radius);

        // Blind all nearby entities
        for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), radius, radius, radius)) {
            if (entity instanceof LivingEntity && entity != player) {
                LivingEntity target = (LivingEntity) entity;
                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0)); // 5 seconds of blindness
            }
        }
    }


    private void castVanish(Player player) {
        int duration = 200; // 10 seconds (200 ticks)

        player.sendMessage("§eYou vanish into the shadows, gaining +30% speed!");
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 1f, 1f);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 30, 0.5, 1, 0.5);

        // Apply invisibility and speed effects
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, duration, 0)); // Invisibility for 10 seconds
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 0)); // Speed boost for 10 seconds
    }

}
