package me.nakilex.levelplugin.spells;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
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
                // 150% weapon damage AoE, knockback enemies in 5-block radius
                // We'll do a simple effect: spawn particles, play sound
                spawnAoEParticles(player.getLocation(), 5, Particle.EXPLOSION_LARGE);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1f, 0.8f);
                player.sendMessage("§eYou slam the ground, dealing AoE damage and knockback!");
                break;
            }
            case "SHIELD_WALL": {
                // def boost by 50% for 10s
                player.sendMessage("§eYou raise a shield wall, reducing damage for 10s!");
                break;
            }
            case "BATTLE_CRY": {
                // +25% atk for allies within 10 blocks for 15s
                player.sendMessage("§eYou unleash a Battle Cry, buffing allies’ damage!");
                break;
            }
            case "CHARGE": {
                // Dash forward 10 blocks
                Vector dir = player.getLocation().getDirection().normalize().multiply(2);
                for (int i = 0; i < 5; i++) {
                    player.setVelocity(dir);
                }
                player.sendMessage("§eYou Charge forward, damaging enemies in your path!");
                break;
            }

            // Rogue
            case "SHADOW_STEP": {
                // Teleport behind nearest enemy within 10 blocks
                player.sendMessage("§eYou vanish and appear behind your enemy with a critical strike!");
                break;
            }
            case "SMOKE_BOMB": {
                // Blind + slow enemies for 5s
                spawnAoEParticles(player.getLocation(), 5, Particle.SMOKE_LARGE);
                player.sendMessage("§eYou toss a smoke bomb, blinding enemies!");
                break;
            }
            case "BLADE_FURY": {
                // spin multiple strikes
                player.sendMessage("§eYou spin wildly, hitting all enemies around you!");
                break;
            }
            case "VANISH": {
                // invisibility for 8s, movement speed
                player.sendMessage("§eYou vanish into the shadows, speed +30% for 8s!");
                break;
            }

            // Mage
            case "FIREBALL": {
                // 120% weapon dmg AoE 4-block radius
                spawnAoEParticles(player.getLocation(), 4, Particle.FLAME);
                player.sendMessage("§eYou launch a Fireball!");
                break;
            }
            case "ICE_BARRIER": {
                // shield for 6s, slows attackers
                player.sendMessage("§eIce Barrier active, absorbing damage for 6s!");
                break;
            }
            case "TELEPORT": {
                // instantly teleport 15 blocks forward
                player.teleport(player.getLocation().add(player.getLocation().getDirection().multiply(15)));
                player.sendMessage("§eYou teleport forward!");
                break;
            }
            case "METEOR": {
                // 250% AoE in 6-block radius after 2s delay
                player.sendMessage("§eA meteor is summoned from the sky!");
                Bukkit.getScheduler().runTaskLater(
                    Bukkit.getPluginManager().getPlugin("LevelPlugin"),
                    () -> spawnAoEParticles(player.getLocation(), 6, Particle.LAVA),
                    40L
                );
                break;
            }

            // Archer
            case "POWER_SHOT": {
                // 200% dmg arrow
                player.sendMessage("§eYou unleash a Power Shot!");
                break;
            }
            case "ARROW_STORM": {
                // 10 arrows in a cone, each 50% dmg
                player.sendMessage("§eYou fire a storm of arrows!");
                break;
            }
            case "EXPLOSIVE_ARROW": {
                // AoE 4-block radius, 150% dmg
                spawnAoEParticles(player.getLocation(), 4, Particle.EXPLOSION_LARGE);
                player.sendMessage("§eYou shoot an explosive arrow!");
                break;
            }
            case "GRAPPLE_HOOK": {
                // Pull yourself toward target block or enemy
                player.sendMessage("§eYou fire a grapple hook!");
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
}
