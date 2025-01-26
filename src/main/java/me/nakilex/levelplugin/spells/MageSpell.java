package me.nakilex.levelplugin.spells;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.managers.ItemManager;
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

    // Optional if you need the plugin reference for setMetadata, etc.
    // Otherwise, you can do a static reference or however your plugin is structured.
    private final Main plugin = Main.getInstance();

    private final Map<UUID, Long> mageBasicCooldown = new HashMap<>();
    private final Map<UUID, Double> teleportManaCosts = new HashMap<>();
    private final Map<UUID, Long> lastTeleportTimes = new HashMap<>();
    private static final double INITIAL_TELEPORT_MANA_COST = 5.0; // Starting mana cost
    private static final double TELEPORT_MANA_MULTIPLIER = 1.2; // Multiplier for successive casts
    private static final long TELEPORT_RESET_TIME = 3000L; // 3 seconds in milliseconds

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

    /** ------------------------------------------------
    /** ------------------------------------------------
     *  MAGE BASIC ATTACK: Simple ray attack
     *  ------------------------------------------------ */
    public void mageBasicSkill(Player player) {
        // Range of the ray in blocks
        int range = 20;

        // Damage dealt by the ray
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        double damage = 5.0 + ps.baseIntelligence + + ps.bonusIntelligence * 0.5; // Example: base damage + INT scaling

        // Starting point and direction of the ray
        Location start = player.getEyeLocation();
        Vector direction = start.getDirection().normalize();

        World world = player.getWorld();

        // Play initial sound and particle effects
        world.playSound(start, Sound.ENTITY_GHAST_SHOOT, 1f, 1.2f);
        world.spawnParticle(Particle.END_ROD, start, 5, 0.1, 0.1, 0.1, 0.02);

        // Perform the ray trace
        for (int i = 0; i < range; i++) {
            Location current = start.clone().add(direction.clone().multiply(i));

            // Visual effect for the ray path
            world.spawnParticle(Particle.CRIT, current, 1, 0, 0, 0, 0);

            // Check for entities at the current location
            for (Entity entity : world.getNearbyEntities(current, 0.5, 0.5, 0.5)) {
                if (entity instanceof LivingEntity && entity != player) {
                    LivingEntity target = (LivingEntity) entity;

                    // Apply damage and knockback
                    target.damage(damage, player);
                    target.setVelocity(direction.clone().multiply(0.2));

                    // Visual and sound effect on hit
                    world.spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation(), 10, 0.2, 0.2, 0.2, 0.02);
                    world.playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 1f, 1.5f);

                    return; // Stop the ray after hitting the first entity
                }
            }

            // Stop the ray if it hits a solid block
            if (current.getBlock().getType().isSolid()) {
                world.spawnParticle(Particle.SMOKE, current, 5, 0.2, 0.2, 0.2, 0.05);
                world.playSound(current, Sound.BLOCK_STONE_HIT, 1f, 0.8f);
                return;
            }
        }

        // End of ray reached without hitting anything
        world.spawnParticle(Particle.SMOKE, start.clone().add(direction.multiply(range)), 5, 0.2, 0.2, 0.2, 0.05);
    }




    /** ------------------------------------------------
     *  METEOR: AoE damage on impact, purely from INT
     *  ------------------------------------------------ */
    private void castMeteor(Player player) {
        // 1) Gather INT from player + weapon
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        int playerInt = ps.baseIntelligence + ps.bonusIntelligence;

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        CustomItem cItem = ItemManager.getInstance().getCustomItemFromItemStack(mainHand);
        int weaponInt = (cItem != null) ? cItem.getIntel() : 0;

        // Example formula: 10 base + 2.0 * totalINT
        double finalDamage = 10.0 + 2.0 * (playerInt + weaponInt);

        // 2) Decide where meteor lands:
        Location target = player.getTargetBlockExact(20) != null
            ? player.getTargetBlockExact(20).getLocation().add(0.5, 1, 0.5)
            : player.getLocation().add(player.getLocation().getDirection().normalize().multiply(5));

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1f);

        // 3) Spawn Fireball from above
        Location spawnLocation = target.clone().add(0, 15, 0);
        Fireball fireball = player.getWorld().spawn(spawnLocation, Fireball.class);
        fireball.setShooter(player);
        fireball.setVelocity(new Vector(0, -1.5, 0));
        // Disable vanilla explosion damage
        fireball.setIsIncendiary(false);
        fireball.setYield(0f);

        // 4) Visual flame trail
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!fireball.isValid()) {
                    cancel();
                    return;
                }
                Location fbLoc = fireball.getLocation();
                fbLoc.getWorld().spawnParticle(Particle.FLAME, fbLoc, 10, 0.3, 0.3, 0.3, 0.02);
                fbLoc.getWorld().spawnParticle(Particle.LARGE_SMOKE, fbLoc, 5, 0.2, 0.2, 0.2, 0.02);
                fbLoc.getWorld().playSound(fbLoc, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 1f);
            }
        }.runTaskTimer(plugin, 0L, 2L);

        // Mark our Fireball with metadata
        fireball.setMetadata("Meteor", new FixedMetadataValue(plugin, true));

        // 5) Custom explosion logic
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onProjectileHit(org.bukkit.event.entity.ProjectileHitEvent event) {
                if (!(event.getEntity() instanceof Fireball)
                    || !event.getEntity().hasMetadata("Meteor")) return;
                Fireball fb = (Fireball) event.getEntity();
                if (!fb.equals(fireball)) return; // ensure it's the same meteor

                // Create explosion effect
                fb.getWorld().spawnParticle(Particle.EXPLOSION, fb.getLocation(), 1);
                fb.getWorld().playSound(fb.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);

                double radius = 4.0;
                for (Entity e : fb.getWorld().getNearbyEntities(fb.getLocation(), radius, radius, radius)) {
                    if (e instanceof LivingEntity && e != player) {
                        LivingEntity le = (LivingEntity) e;
                        le.damage(finalDamage, player);
                        le.setFireTicks(100); // 5s of fire
                    }
                }
                fb.remove();

                // Unregister so future meteors won't fire duplicates
                org.bukkit.event.entity.ProjectileHitEvent.getHandlerList().unregister(this);
            }
        }, plugin);
    }

    /** ------------------------------------------------
     *  BLACKHOLE: pulls mobs in, deals repeated damage
     *  ------------------------------------------------ */
    private void castBlackhole(Player player) {
        // 1) Gather INT
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        int playerInt = ps.baseIntelligence + ps.bonusIntelligence;

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        CustomItem cItem = ItemManager.getInstance().getCustomItemFromItemStack(mainHand);
        int weaponInt = (cItem != null) ? cItem.getIntel() : 0;

        // Example formula: 10 base + 0.5 * totalINT each "pull" tick
        double damage = 10.0 + 0.5 * (playerInt + weaponInt);

        // 2) Create blackhole ~10 blocks forward
        Location target = player.getEyeLocation().add(player.getLocation().getDirection().multiply(10));
        double pullRadius = 5.0;
        double damageRadius = 1.0;

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 1f, 1f);
        createBlackholeEffect(target, pullRadius);

        // 3) Repeated pulling & damage for ~2.5s (50 ticks)
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks++ >= 50) {
                    cancel();
                    return;
                }
                for (Entity e : player.getWorld().getNearbyEntities(target, pullRadius, pullRadius, pullRadius)) {
                    if (e instanceof LivingEntity && e != player) {
                        LivingEntity le = (LivingEntity) e;
                        Vector pullVec = target.toVector()
                            .subtract(le.getLocation().toVector())
                            .normalize().multiply(0.2);
                        le.setVelocity(pullVec);

                        // If close enough to center, deal damage
                        if (le.getLocation().distance(target) <= damageRadius) {
                            le.damage(damage, player);
                            le.getWorld().spawnParticle(Particle.CRIT, le.getLocation(), 10, 0.2, 0.2, 0.2);
                        }
                    }
                }
                // For visuals each tick
                player.getWorld().spawnParticle(Particle.WITCH, target, 10, 0.5, 0.5, 0.5, 0.1);
                player.getWorld().playSound(target, Sound.BLOCK_BEACON_AMBIENT, 0.5f, 1.2f);
            }
        }.runTaskTimer(plugin, 0L, 2L);
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
                    double rad = Math.toRadians(angle);
                    double x = radius * Math.cos(rad);
                    double z = radius * Math.sin(rad);
                    Location particleLoc = center.clone().add(x, 0, z);

                    // Make the ring rise/fall slightly
                    particleLoc.add(0, Math.sin(ticks / 10.0) * 0.5, 0);

                    center.getWorld().spawnParticle(Particle.PORTAL, particleLoc, 1, 0, 0, 0, 0);
                    center.getWorld().spawnParticle(Particle.SMOKE, particleLoc, 1, 0, 0, 0, 0);
                }
                center.getWorld().spawnParticle(Particle.DRAGON_BREATH, center, 5, 0.2, 0.2, 0.2, 0.02);
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    /** ------------------------------------------------
     *  HEAL: unchanged, no INT usage here
     *  ------------------------------------------------ */
    private void healPlayer(Player player, int baseAmount) {
        // Retrieve player's stats
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        int intelligence = ps.baseIntelligence + ps.bonusIntelligence;

        // Scale healing with intelligence
        double scaledHealing = baseAmount + (intelligence * 0.5); // Example scaling: 50% of INT added to base amount
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        double newHealth = Math.min(player.getHealth() + scaledHealing, maxHealth);

        // Apply the healing
        player.setHealth(newHealth);

        // Add visual and sound effects
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation(), 30, 1, 1, 1);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 1f, 1f);

        // Optional: Send a message to the player indicating how much they were healed
        player.sendMessage("§aYou have been healed for " + Math.round(scaledHealing) + " health!");
    }


    /** ------------------------------------------------
     *  TELEPORT: unchanged, no INT usage
     *  ------------------------------------------------ */
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
        // Simple check for block collisions near that spot
        for (int i = -1; i <= 1; i++) {
            Location tempLocation = target.clone().add(0, i, 0);
            if (ClickComboListener.isLocTpSafe(tempLocation)) {
                return tempLocation;
            }
        }
        return null;
    }
}
