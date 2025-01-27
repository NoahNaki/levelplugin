package me.nakilex.levelplugin.spells;

import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.utils.MetadataTrait;
import net.citizensnpcs.api.CitizensAPI;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.util.DataKey;
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
                castShurikenThrow(player);
                break;
            case "DAGGER_THROW":
                castShadowClone(player);
                break;
            default:
                player.sendMessage("§eUnknown Rogue Spell: " + effectKey);
                break;
        }
    }


    private void castShurikenThrow(Player player) {
        // The center location from which our "shuriken" will be formed
        final Location center = player.getLocation().clone();

        // Forward velocity (tweak as desired)
        final Vector forwardVelocity = center.getDirection().multiply(1.3);

        // We’ll store ArmorStands so we can manipulate/rotate/remove them later
        final List<ArmorStand> stands = new ArrayList<>();

        // Create 4 offsets in a + shape around the center
        final double radius = 0.2; // or even 0.15
        final Vector[] offsets = {
            new Vector(radius, 0, 0),
            new Vector(-radius, 0, 0),
            new Vector(0, 0, radius),
            new Vector(0, 0, -radius)
        };

        // For each offset, spawn an ArmorStand
        for (Vector offset : offsets) {
            Location spawnLoc = center.clone().add(offset);
            ArmorStand stand = (ArmorStand) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.ARMOR_STAND);

            // Make the stand invisible, remove base plate, no gravity, etc.
            stand.setInvisible(true);
            stand.setMarker(true);
            stand.setBasePlate(false);
            stand.setGravity(false);
            stand.setArms(true);

            // Give the stand a sword in its hand
            stand.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));

            // Make arms T-pose, so swords stick out horizontally
            // Adjust as needed if you want them oriented differently.
            // Example: tilt arms forward (around X) so the handle is closer to center
            stand.setRightArmPose(new EulerAngle(Math.toRadians(270), 0, Math.toRadians(90)));
            stand.setLeftArmPose(new EulerAngle(Math.toRadians(270), 0, Math.toRadians(-90)));
            stands.add(stand);
        }

        // Schedule a repeating task to move the stands forward & rotate them
        new BukkitRunnable() {
            // totalTicks controls how long the shuriken “flies” before disappearing
            private final int maxTicks = 60; // e.g., 3 seconds at 20 TPS
            private int ticksLived = 0;

            // We'll track a rotation angle in radians
            private double angle = 0.0;

            @Override
            public void run() {
                if (ticksLived++ >= maxTicks) {
                    // Time’s up – remove the armor stands
                    for (ArmorStand stand : stands) {
                        if (!stand.isDead()) {
                            stand.remove();
                        }
                    }
                    cancel();
                    return;
                }

                // Increase rotation angle slightly each tick
                angle += Math.toRadians(45); // 15 degrees per tick

                // Move the center forward
                center.add(forwardVelocity);

                // For each stand, calculate a new rotated position around the center
                for (int i = 0; i < stands.size(); i++) {
                    ArmorStand stand = stands.get(i);
                    Vector baseOffset = offsets[i].clone();

                    // Rotate the offset around the Y-axis by our current 'angle'
                    double cos = Math.cos(angle);
                    double sin = Math.sin(angle);

                    double rotatedX = baseOffset.getX() * cos - baseOffset.getZ() * sin;
                    double rotatedZ = baseOffset.getX() * sin + baseOffset.getZ() * cos;

                    // Generate the new location by adding rotated offset to center
                    Location newLoc = center.clone().add(rotatedX, 0, rotatedZ);

                    // Face outward from center (optional), so the stands rotate properly
                    float yawDegrees = (float) Math.toDegrees(Math.atan2(-rotatedX, rotatedZ));
                    newLoc.setYaw(yawDegrees);

                    // Move the ArmorStand
                    stand.teleport(newLoc);
                }
            }
        }.runTaskTimer(
            Bukkit.getPluginManager().getPlugin("LevelPlugin"),
            0L,
            1L
        );
    }

    private void castShadowClone(Player player) {
        UUID playerUUID = player.getUniqueId();

        // Debug: Log player UUID
        Bukkit.getLogger().info("[ShadowClone] Checking for existing shadow clone for player: " + player.getName() + " (UUID: " + playerUUID + ")");

        // Iterate over all NPCs in the registry
        NPC existingClone = null;
        for (NPC npc : CitizensAPI.getNPCRegistry()) {
            if (npc.hasTrait(MetadataTrait.class)) {
                MetadataTrait metadata = npc.getTrait(MetadataTrait.class);
                if (metadata.getOwner() != null && metadata.getOwner().equals(playerUUID)) {
                    existingClone = npc;
                    break;
                }
            }
        }

        if (existingClone != null) {
            // Swap positions
            Bukkit.getLogger().info("[ShadowClone] Found active shadow clone. Swapping positions...");
            Location cloneLocation = existingClone.getEntity().getLocation();
            Location playerLocation = player.getLocation();

            existingClone.teleport(playerLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
            player.teleport(cloneLocation);

            player.sendMessage("§aYou swapped places with your shadow clone!");
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
            return;
        }

        // No active shadow clone, create a new one
        Bukkit.getLogger().info("[ShadowClone] No active shadow clone found. Creating a new one...");
        Location cloneLocation = player.getLocation();

        NPC shadowClone = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, "Shadow Clone");
        shadowClone.spawn(cloneLocation);
        shadowClone.getOrAddTrait(MetadataTrait.class).setOwner(playerUUID);

        // Set the NPC's skin to match the player
        shadowClone.data().setPersistent("player-skin-name", player.getName());

        // Mimic player appearance
        if (shadowClone.getEntity() instanceof Player npcPlayer) {
            // Copy the player's armor
            npcPlayer.getInventory().setArmorContents(player.getInventory().getArmorContents());
            npcPlayer.getInventory().setItemInMainHand(player.getInventory().getItemInMainHand());
            npcPlayer.getInventory().setItemInOffHand(player.getInventory().getItemInOffHand());

            // Debug: Log armor application
            Bukkit.getLogger().info("[ShadowClone] Applied player's armor to shadow clone.");
        }

        player.sendMessage("§aYou created a shadow clone!");

        // Schedule removal
        new BukkitRunnable() {
            @Override
            public void run() {
                if (shadowClone.isSpawned()) {
                    Bukkit.getLogger().info("[ShadowClone] Expiring shadow clone for player: " + player.getName());
                    Location explosionLocation = shadowClone.getEntity().getLocation();
                    shadowClone.despawn();
                    shadowClone.destroy();

                    explosionLocation.getWorld().createExplosion(explosionLocation, 2.0f, false, false);
                    player.sendMessage("§cYour shadow clone exploded!");
                }
            }
        }.runTaskLater(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 100L); // 5 seconds
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
}
