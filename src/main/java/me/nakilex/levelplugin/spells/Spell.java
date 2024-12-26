package me.nakilex.levelplugin.spells;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import me.nakilex.levelplugin.listeners.ClickComboListener;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Snowball;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.Bukkit;

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
                spawnAoEParticles(player.getLocation(), 5, Particle.EXPLOSION);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1f, 0.8f);
                player.sendMessage("§eYou slam the ground, dealing AoE damage and knockback!");
                break;
            }
            case "SHIELD_WALL": {
                player.sendMessage("§eYou raise a shield wall, reducing damage for 10s!");
                break;
            }
            case "BATTLE_CRY": {
                player.sendMessage("§eYou unleash a Battle Cry, buffing allies’ damage!");
                break;
            }
            case "CHARGE": {
                Vector dir = player.getLocation().getDirection().normalize().multiply(2);
                player.setVelocity(dir);
                player.sendMessage("§eYou Charge forward, damaging enemies in your path!");
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
                player.sendMessage("§eYou vanish and appear behind your enemy with a critical strike!");
                break;
            }
            case "BLADE_FURY": {
                player.sendMessage("§eYou spin wildly, hitting all enemies around you!");
                break;
            }
            case "SMOKE_BOMB": {
                spawnAoEParticles(player.getLocation(), 5, Particle.LARGE_SMOKE);
                player.sendMessage("§eYou toss a smoke bomb, blinding enemies!");
                break;
            }
            case "VANISH": {
                player.sendMessage("§eYou vanish into the shadows, speed +30% for 8s!");
                break;
            }

            // Archer
            case "POWER_SHOT": {
                player.sendMessage("§eYou unleash a Power Shot!");
                break;
            }
            case "EXPLOSIVE_ARROW": {
                spawnAoEParticles(player.getLocation(), 4, Particle.EXPLOSION);
                player.sendMessage("§eYou shoot an explosive arrow!");
                break;
            }
            case "GRAPPLE_HOOK": {
                player.sendMessage("§eYou fire a grapple hook!");
                break;
            }
            case "ARROW_STORM": {
                player.sendMessage("§eYou fire a storm of arrows!");
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
        String activeCombo = me.nakilex.levelplugin.listeners.ClickComboListener.getActiveCombo(player);

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
}
