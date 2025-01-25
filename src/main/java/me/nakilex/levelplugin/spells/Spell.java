package me.nakilex.levelplugin.spells;

import me.nakilex.levelplugin.effects.utils.ArmorStandEffectUtil;
import me.nakilex.levelplugin.effects.utils.ParticleEffectUtil;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.listener.ClickComboListener;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.util.*;

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
    private final Map<UUID, Long> mageBasicCooldown = new HashMap<>();


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

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCombo() {
        return combo;
    }

    public int getManaCost() {
        return manaCost;
    }

    public int getCooldown() {
        return cooldown;
    }

    public int getLevelReq() {
        return levelReq;
    }

    public List<Material> getAllowedWeapons() {
        return allowedWeapons;
    }

    public String getEffectKey() {
        return effectKey;
    }

    public double getDamageMultiplier() {
        return damageMultiplier;
    }

    /**
     * Actually apply the effect logic to the player (and possibly nearby enemies).
     * In a real scenario, you'd add more custom logic for AoE, damage, etc.
     */
    public void castEffect(Player player) {
        // For demonstration, we do simple logic based on effectKey
        switch (effectKey.toUpperCase()) {
            // Warrior
            case "IRON_FORTRESS": {
                castIronFortress(player);
                break;
            }
            case "HEROIC_LEAP": {
                castHeroicLeap(player);
                break;
            }
            case "UPPERCUT": {
                castUppercut(player);
                break;
            }
            case "GROUND_SLAM": {
                castGroundSlam(player);
                break;
            }


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

            // Rogue
            case "SHADOW_STEP": {
                castShadowStep(player);
                break;
            }
            case "BLADE_FURY": {
                castBladeFury(player);
                break;
            }
            case "DAGGER_THROW": {
                castDaggerThrow(player);
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
            if (currentTime - lastCastTime < 500) { // Cooldown of 500ms
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
                    target.damage(damage, player); // Apply damage to the entity
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


    private void castBlackhole(Player player) {
        Location target = player.getEyeLocation().add(player.getLocation().getDirection().multiply(10));
        double pullRadius = 5.0;
        double damageRadius = 1.0;
        double damage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 0.1; // 50% weapon damage per tick
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

                        Vector pullVector = target.toVector().subtract(targetEntity.getLocation().toVector()).normalize().multiply(0.2);
                        targetEntity.setVelocity(pullVector);

                        if (targetEntity.getLocation().distance(target) <= damageRadius) {
                            targetEntity.damage(damage, player);
                            targetEntity.getWorld().spawnParticle(Particle.CRIT, targetEntity.getLocation(), 10, 0.2, 0.2, 0.2);
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

                    particleLocation.add(0, Math.sin(ticks / 10.0) * 0.5, 0);

                    center.getWorld().spawnParticle(Particle.PORTAL, particleLocation, 1, 0, 0, 0, 0);
                    center.getWorld().spawnParticle(Particle.SMOKE, particleLocation, 1, 0, 0, 0, 0);
                }

                center.getWorld().spawnParticle(Particle.DRAGON_BREATH, center, 5, 0.2, 0.2, 0.2, 0.02);
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 0L, 2L);
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
            if (ClickComboListener.isLocTpSafe(tempLocation)) {
                return tempLocation;
            }
        }
        return null;
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
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 0L, 5L); // Fires one arrow every 5 ticks (0.25 seconds)
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

                LivingEntity target = player.getWorld().getNearbyEntities(player.getLocation(), 15, 15, 15).stream()
                    .filter(e -> e instanceof LivingEntity && e != player)
                    .map(e -> (LivingEntity) e)
                    .min(Comparator.comparingDouble(e -> e.getLocation().distance(player.getLocation())))
                    .orElse(null);

                if (target != null) {
                    Location behindTarget = target.getLocation().clone().add(target.getLocation().getDirection().multiply(-1).normalize());
                    behindTarget.setYaw(target.getLocation().getYaw());
                    behindTarget.setPitch(target.getLocation().getPitch());
                    player.teleport(behindTarget);

                    double damage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 1.5; // Increased damage for effect
                    target.damage(damage, player);

                    target.getWorld().spawnParticle(Particle.CRIT, target.getLocation(), 30, 0.5, 1, 0.5); // Critical hit particles
                    target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation(), 10, 0.5, 0.5, 0.5); // Sweep attack visuals
                    target.getWorld().spawnParticle(Particle.PORTAL, target.getLocation(), 20, 0.5, 1, 0.5); // Portal effect for teleportation

                    player.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1f);
                    player.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.5f, 1.2f); // Wither-like whoosh sound
                    player.getWorld().playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.5f); // Distorted teleport sound

                    if (target instanceof Player) {
                        ((Player) target).addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 0)); // 1 second of blindness
                    }

                    target.getWorld().spawnParticle(Particle.EXPLOSION, target.getLocation(), 10, 0.3, 0.3, 0.3);

                    player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20, 0)); // 1 second of glowing for cool effect
                } else {
                }

                player.getWorld().spawnParticle(Particle.ASH, player.getLocation(), 10, 0.3, 0.3, 0.3);

                casts++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 0L, 10L); // Delay of 10 ticks (0.5 seconds) between casts
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

    private void castIronFortress(Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 1f);
        ParticleEffectUtil.createShieldEffect(player.getLocation(), 2, Particle.BLOCK_CRUMBLE, Material.IRON_BLOCK);

        // Create and track 4 armor stands holding shields
        List<ArmorStand> shields = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            ArmorStand armorStand = (ArmorStand) player.getWorld().spawnEntity(player.getLocation(), EntityType.ARMOR_STAND);
            armorStand.setInvisible(true);
            armorStand.setMarker(true);
            armorStand.setSmall(true);
            armorStand.setGravity(false);
            armorStand.setSmall(true);

            ItemStack shieldItem = new ItemStack(Material.SHIELD);
            armorStand.getEquipment().setItemInMainHand(shieldItem);

            armorStand.setArms(true);
            armorStand.setRightArmPose(new EulerAngle(Math.toRadians(-90), Math.toRadians(0), Math.toRadians(0)));

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

                    float yaw = (float) Math.toDegrees(Math.atan2(player.getLocation().getZ() - shieldLocation.getZ(),
                        player.getLocation().getX() - shieldLocation.getX()));
                    shield.setRotation(yaw, 0);
                }
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 0L, 1L);

        // Schedule a task to remove shields after 5 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                shields.forEach(Entity::remove);
                shields.clear();
            }
        }.runTaskLater(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 100L); // 100 ticks = 5 seconds

        // Cancel player damage and handle shield breaking
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onEntityDamage(EntityDamageEvent event) {
                if (!(event.getEntity() instanceof Player) || !event.getEntity().equals(player)) {
                    return;
                }

                if (!shields.isEmpty()) {
                    event.setCancelled(true); // Cancel the damage

                    // Remove one shield
                    ArmorStand shield = shields.remove(0);
                    shield.getWorld().playSound(shield.getLocation(), Sound.ITEM_SHIELD_BREAK, 1f, 1f);
                    shield.remove();

                    if (shields.isEmpty()) {
                        HandlerList.unregisterAll(this); // Unregister the event listener
                    }
                }
            }
        }, Bukkit.getPluginManager().getPlugin("LevelPlugin"));
    }


    private void castHeroicLeap(Player player) {
        double leapDistance = 15.0;
        double damageRadius = 5.0;
        double damage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 1.8; // 180% weapon damage

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 1f);

        Location start = player.getLocation();
        Vector direction = start.getDirection().normalize().multiply(leapDistance);
        Location target = start.clone().add(direction);

        Vector leapVector = target.toVector().subtract(start.toVector()).normalize().multiply(1.5);
        leapVector.setY(1.2); // Add vertical velocity
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
                            target.damage(damage, player);
                            Vector knockback = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(1.5);
                            knockback.setY(0.5);
                            target.setVelocity(knockback);
                        }
                    }

                    cancel();
                }
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 2L, 1L); // Delay by 2 ticks before starting the ground check
    }

    private void castUppercut(Player player) {
        double damage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 1.3; // 130% weapon damage
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

                Vector toTarget = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
                double angle = direction.angle(toTarget);
                if (Math.toDegrees(angle) > 45) {
                    continue;
                }

                target.damage(damage, player);

                Vector knockup = new Vector(0, knockupStrength, 0).add(direction.clone().multiply(knockbackStrength));
                target.setVelocity(knockup);

                target.getWorld().spawnParticle(Particle.CRIT, target.getLocation(), 10, 0.2, 0.2, 0.2, 0.1);
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1f);
            }
        }
    }

    private void castGroundSlam(Player player) {
        double maxRadius = 10.0; // Maximum radius of the ripple
        double damage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 1.5; // 150% weapon damage
        int duration = 20; // Duration in ticks between each ripple expansion
        int steps = 10; // Number of ripple steps (determines granularity of the effect)

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
                    double radians = Math.toRadians(angle);
                    double x = Math.cos(radians) * currentRadius;
                    double z = Math.sin(radians) * currentRadius;
                    Location rippleLocation = player.getLocation().clone().add(x, 0, z);

                    rippleLocation.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, rippleLocation, 10, 0.2, 0.2, 0.2, 0.1, Material.DIRT.createBlockData());
                    rippleLocation.getWorld().spawnParticle(Particle.CRIT, rippleLocation, 5, 0.2, 0.2, 0.2);

                    for (Entity entity : rippleLocation.getWorld().getNearbyEntities(rippleLocation, 1, 1, 1)) {
                        if (entity instanceof LivingEntity && entity != player) {
                            LivingEntity target = (LivingEntity) entity;
                            target.damage(damage, player);

                            Vector knockback = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(0.5);
                            knockback.setY(0.3);
                            target.setVelocity(knockback);
                        }
                    }

                    if (!rippleLocation.getBlock().isPassable()) {
                        break;
                    }
                }

                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 0.5f, 0.8f);
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 0L, duration / steps);
    }
}
