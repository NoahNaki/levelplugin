package me.nakilex.levelplugin.spells;

import me.nakilex.levelplugin.effects.utils.ArmorStandEffectUtil;
import me.nakilex.levelplugin.effects.utils.ParticleEffectUtil;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import me.nakilex.levelplugin.player.listener.ClickComboListener;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
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
            case "IRON_FORTRESS": {
                castIronFortress(player);
                break;
            }
            case "HEROIC_LEAP": {
                castHeroicLeap(player);
                break;
            }
            case "WHIRLWIND": {
                castWhirlwind(player);
                break;
            }
            case "SEISMIC_SHOCKWAVE": {
                castSeismicShockwave(player);
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

    private void spawnAoEParticles(Location center, int radius, Particle particle) {
        for (int i = 0; i < 50; i++) {
            double x = center.getX() + (Math.random() - 0.5)*2*radius;
            double y = center.getY() + 0.5;
            double z = center.getZ() + (Math.random() - 0.5)*2*radius;
            center.getWorld().spawnParticle(particle, x, y, z, 0, 0,0,0, 1);
        }
    }

    private void mageBasicSkill(Player player) {
        // Retrieve player's stats and class
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player);
        String className = ps.playerClass.name().toLowerCase();

        // Check if the player is a Mage
        if (!className.equals("mage")) {
            return; // Do nothing if the player is not a Mage
        }

        // Check if the player is holding a valid Mage weapon (stick or blaze rod)
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand == null || (mainHand.getType() != Material.STICK && mainHand.getType() != Material.BLAZE_ROD)) {
            return; // Do nothing if the player is not holding a valid Mage weapon
        }

        // Check cooldown
        UUID playerUUID = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        if (mageBasicCooldown.containsKey(playerUUID)) {
            long lastCastTime = mageBasicCooldown.get(playerUUID);
            if (currentTime - lastCastTime < 500) { // Cooldown of 500ms
                player.sendMessage("§cSkill is on cooldown! Please wait a moment.");
                return;
            }
        }

        // Record the cast time
        mageBasicCooldown.put(playerUUID, currentTime);

        // Check if the player is in the middle of a combo
        String activeCombo = ClickComboListener.getActiveCombo(player);
        if (!activeCombo.isEmpty() && activeCombo.length() < 3) {
            return; // Let the input contribute to the combo instead
        }

        // Magic Beam Logic
        Location start = player.getEyeLocation();
        Vector direction = start.getDirection().normalize();
        double beamLength = 20.0; // Max length of the beam
        double damage = 6.0; // Damage dealt to entities

        // Beam visuals and logic
        for (double i = 0; i < beamLength; i += 0.5) {
            Location point = start.clone().add(direction.clone().multiply(i));

            // Spawn particles at each point along the beam
            player.getWorld().spawnParticle(Particle.END_ROD, point, 1, 0.1, 0.1, 0.1, 0.1);

            // Check for collision with entities
            for (Entity entity : player.getWorld().getNearbyEntities(point, 0.5, 0.5, 0.5)) {
                if (entity instanceof LivingEntity && entity != player) {
                    LivingEntity target = (LivingEntity) entity;
                    target.damage(damage, player); // Apply damage to the entity
                    player.getWorld().spawnParticle(Particle.WITCH, target.getLocation(), 10, 0.2, 0.2, 0.2);
                    player.getWorld().playSound(target.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 1f, 1f);
                    return; // Stop the beam upon hitting an entity
                }
            }

            // Stop if the beam reaches an obstacle
            if (!point.getBlock().isPassable()) {
                player.getWorld().playSound(point, Sound.BLOCK_GLASS_BREAK, 1f, 1f);
                break;
            }
        }

        // Play casting sound at the player's location
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITCH_THROW, 1f, 1f);
    }




    private void castMeteor(Player player) {
        Location target = player.getTargetBlockExact(100).getLocation().add(0.5, 1, 0.5);
        double radius = 4.0;
        double damage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 2.5; // 250% weapon damage

        player.sendMessage("§eYou summon a Meteor!");
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1f);
        player.getWorld().spawnParticle(Particle.FLAME, target, 50, 1, 1, 1);

        // Delayed meteor strike
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks++ >= 40) { // 2-second delay
                    player.getWorld().spawnParticle(Particle.EXPLOSION, target, 20);
                    player.getWorld().playSound(target, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);

                    // Deal damage and ignite nearby entities
                    for (Entity entity : player.getWorld().getNearbyEntities(target, radius, radius, radius)) {
                        if (entity instanceof LivingEntity && entity != player) {
                            LivingEntity targetEntity = (LivingEntity) entity;
                            targetEntity.damage(damage, player); // Apply damage
                            targetEntity.setFireTicks(100); // Ignite for 5 seconds
                        }
                    }

                    cancel();
                } else {
                    // Visuals during the meteor descent
                    player.getWorld().spawnParticle(Particle.SMOKE, target.add(0, 0.2, 0), 10, 1, 1, 1, 0.1);
                    player.getWorld().playSound(target, Sound.BLOCK_FIRE_AMBIENT, 0.5f, 0.8f);
                }
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 0L, 1L);
    }


    private void castBlackhole(Player player) {
        Location target = player.getEyeLocation().add(player.getLocation().getDirection().multiply(10));
        double pullRadius = 5.0;
        double damageRadius = 3.0;
        double damage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 0.5; // 50% weapon damage per tick

        player.sendMessage("§eYou summon a Black Hole!");
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 1f, 1f);
        player.getWorld().spawnParticle(Particle.PORTAL, target, 100, 1, 1, 1);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks++ >= 100) { // 5 seconds duration
                    cancel();
                    return;
                }

                // Pull nearby entities
                for (Entity entity : player.getWorld().getNearbyEntities(target, pullRadius, pullRadius, pullRadius)) {
                    if (entity instanceof LivingEntity && entity != player) {
                        LivingEntity targetEntity = (LivingEntity) entity;

                        // Pull toward the black hole
                        Vector pullVector = target.toVector().subtract(targetEntity.getLocation().toVector()).normalize().multiply(0.2);
                        targetEntity.setVelocity(pullVector);

                        // Apply damage if within damage radius
                        if (targetEntity.getLocation().distance(target) <= damageRadius) {
                            targetEntity.damage(damage, player);
                            targetEntity.getWorld().spawnParticle(Particle.CRIT, targetEntity.getLocation(), 10, 0.2, 0.2, 0.2);
                        }
                    }
                }

                // Visuals for the black hole
                player.getWorld().spawnParticle(Particle.WITCH, target, 10, 0.5, 0.5, 0.5, 0.1);
                player.getWorld().playSound(target, Sound.BLOCK_BEACON_AMBIENT, 0.5f, 1.2f);
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

    private final Set<UUID> grappleCooldown = new HashSet<>(); // Tracks players who can't use grapple mid-air

    private void castGrappleHook(Player player) {
        if (grappleCooldown.contains(player.getUniqueId())) {
            player.sendMessage("§cYou must touch the ground before using Grapple Hook again!");
            return;
        }

        grappleCooldown.add(player.getUniqueId()); // Add player to cooldown

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
            boolean slamTriggered = false; // Tracks if slam should be triggered upon landing

            @Override
            public void run() {
                if (player.isOnGround()) {
                    grappleCooldown.remove(player.getUniqueId()); // Allow grapple again when grounded
                    if (slamTriggered) {
                        performSlam(player); // Trigger slam upon landing
                    }
                    cancel();
                    return;
                }

                if (slamTriggered) {
                    // Maintain strong downward velocity for the slam
                    Vector downwardVelocity = player.getVelocity();
                    downwardVelocity.setY(Math.max(downwardVelocity.getY() - 0.5, -2.5)); // Consistent downward force
                    player.setVelocity(downwardVelocity);
                } else {
                    // Glide effect
                    Vector glide = player.getVelocity().multiply(0.9); // Slow horizontal speed
                    glide.setY(Math.max(player.getVelocity().getY() - 0.05, -0.1)); // Slow vertical descent
                    player.setVelocity(glide);

                    // Check if the player is crouching (sneaking)
                    if (player.isSneaking()) {
                        slamTriggered = true; // Mark that slam should be triggered upon landing
                        player.sendMessage("§ePreparing to slam into the ground!");
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 1f, 0.8f);
                        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation(), 20, 0.5, 1, 0.5);

                        // Apply initial downward velocity
                        player.setVelocity(new Vector(0, -2, 0));
                    }
                }
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 10L, 1L);
    }

    private void performSlam(Player player) {
        double radius = 5.0;
        double damage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 2.0; // 200% weapon damage

        player.sendMessage("§eYou slam into the ground, damaging nearby enemies!");
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
        player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 20);

        // Damage and knock back nearby entities
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

    private void castVanish(Player player) {
        int duration = 200; // 10 seconds (200 ticks)

        player.sendMessage("§eYou vanish into the shadows, gaining +30% speed!");
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 1f, 1f);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 30, 0.5, 1, 0.5);

        // Apply invisibility and speed effects
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, duration, 0)); // Invisibility for 10 seconds
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 0)); // Speed boost for 10 seconds
    }

    private void castDaggerThrow(Player player) {
        player.sendMessage("§eYou throw 3 daggers in a cone!");

        double distance = 10.0;
        double damage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 1.5;

        // Base location at player's chest level
        Location baseLocation = player.getLocation().clone().add(0, 1.0, 0); // Chest level

        // Get the player's forward direction (horizontal only)
        Vector forward = baseLocation.getDirection();
        forward.setY(0).normalize(); // Only horizontal movement

        // Add a hard-coded offset to the left
        Vector leftOffset = rotateAroundAxisY(forward.clone(), -90).normalize().multiply(0.7); // Adjust multiplier for more/less left
        Location adjustedLocation = baseLocation.clone().add(leftOffset);

        // Spawn locations for the three daggers
        Location centerSpawn = adjustedLocation.clone(); // Center dagger
        Location leftSpawn = adjustedLocation.clone().add(rotateAroundAxisY(forward.clone(), -15).normalize().multiply(0.3)); // Left dagger
        Location rightSpawn = adjustedLocation.clone().add(rotateAroundAxisY(forward.clone(), 15).normalize().multiply(0.3)); // Right dagger

        // Spawn each dagger
        ArmorStandEffectUtil.createLeadingArmorStandInDirection(centerSpawn, Material.IRON_SWORD, 22, forward, distance);
        ArmorStandEffectUtil.createLeadingArmorStandInDirection(leftSpawn, Material.IRON_SWORD, 22, rotateAroundAxisY(forward.clone(), -15).normalize(), distance);
        ArmorStandEffectUtil.createLeadingArmorStandInDirection(rightSpawn, Material.IRON_SWORD, 22, rotateAroundAxisY(forward.clone(), 15).normalize(), distance);

        // Damage logic (unchanged)
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
        int duration = 100; // 5 seconds (100 ticks)
        double reflectPercentage = 0.5; // Reflect 50% damage

        player.sendMessage("§eYou raise an Iron Fortress, becoming immune and reflecting damage!");

        ParticleEffectUtil.createShieldEffect(player.getLocation(), 2, Particle.BLOCK_CRUMBLE, Material.IRON_BLOCK);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 1f);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks++ >= duration) {
                    cancel();
                    player.sendMessage("§cIron Fortress has ended.");
                    return;
                }
                // Reflect damage logic could be implemented in a custom damage listener
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 0L, 1L);
    }

    private void castHeroicLeap(Player player) {
        double leapDistance = 15.0; // Distance of the leap
        double damageRadius = 5.0; // Radius for AoE damage
        double damage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 1.8; // 180% weapon damage

        player.sendMessage("§eYou leap heroically into the air, ready to slam down on your foes!");
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 1f);

        // Calculate the target location based on the player's direction
        Location start = player.getLocation();
        Vector direction = start.getDirection().normalize().multiply(leapDistance);
        Location target = start.clone().add(direction);

        // Launch the player into the air towards the target location
        Vector leapVector = target.toVector().subtract(start.toVector()).normalize().multiply(1.5);
        leapVector.setY(1.2); // Add vertical velocity
        player.setVelocity(leapVector);

        // Play a sound and spawn particles to visualize the leap
        player.getWorld().spawnParticle(Particle.EXPLOSION, start, 10, 0.5, 1, 0.5);

        // Handle the landing effects
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnGround()) {
                    // Damage and knock back nearby entities upon landing
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
                    player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 20, 0.5, 1, 0.5);

                    for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), damageRadius, damageRadius, damageRadius)) {
                        if (entity instanceof LivingEntity && entity != player) {
                            LivingEntity target = (LivingEntity) entity;
                            target.damage(damage, player); // Apply damage
                            Vector knockback = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(1.5);
                            knockback.setY(0.5); // Add upward knockback
                            target.setVelocity(knockback);
                        }
                    }

                    cancel();
                }
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 0L, 1L);
    }



    private void castWhirlwind(Player player) {
        double radius = 4.0;
        double damage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 0.8; // 80% weapon damage per tick
        int duration = 100; // 5 seconds (100 ticks)

        player.sendMessage("§eYou spin in a whirlwind, slicing nearby enemies!");

        // Spawn spinning axes
        ArmorStandEffectUtil.spawnRotatingArmorStands(player.getLocation(), Material.IRON_AXE, 3, duration);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks++ >= duration) {
                    cancel();
                    player.sendMessage("§cWhirlwind has ended.");
                    return;
                }

                // Create particle vortex
                ParticleEffectUtil.createVortexEffect(player.getLocation(), Particle.SWEEP_ATTACK, radius, 10);

                for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), radius, radius, radius)) {
                    if (entity instanceof LivingEntity && entity != player) {
                        LivingEntity target = (LivingEntity) entity;
                        target.damage(damage, player);
                    }
                }
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 0L, 5L);
    }


    private void castSeismicShockwave(Player player) {
        double distance = 10.0;
        double damage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 1.5; // 150% weapon damage
        double slowDuration = 60; // 3 seconds (60 ticks)

        player.sendMessage("§eYou slam the ground, creating a shockwave!");

        // Spawn hammer-leading shockwave
        ArmorStandEffectUtil.createLeadingArmorStand(player.getLocation(), Material.IRON_AXE, 10);

        Vector direction = player.getLocation().getDirection().normalize();
        Location start = player.getLocation().add(0, 1, 0);
        for (double i = 0; i <= distance; i += 0.5) {
            Location point = start.clone().add(direction.clone().multiply(i));
            point.getWorld().spawnParticle(Particle.CRIT, point, 10, 0.2, 0.2, 0.2);

            // Ground cracking effect
            ParticleEffectUtil.createBlockBreakingEffect(point, Material.COBBLESTONE, 5);

            for (Entity entity : player.getWorld().getNearbyEntities(point, 1, 1, 1)) {
                if (entity instanceof LivingEntity && entity != player) {
                    LivingEntity target = (LivingEntity) entity;
                    target.damage(damage, player);
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, (int) slowDuration, 1)); // Slow effect
                }
            }

            if (!point.getBlock().isPassable()) break; // Stop shockwave at obstacles
        }
    }
}
