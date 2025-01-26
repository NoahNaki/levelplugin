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
     *  MAGE BASIC SKILL (beam that deals single hit)
     *  ------------------------------------------------ */
    private void mageBasicSkill(Player player) {
        // 1) Check if player is actually a Mage
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player);
        if (!ps.playerClass.name().equalsIgnoreCase("mage")) {
            return;
        }

        // 2) Check if main-hand item is valid for a 'wand'
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand == null
            || (mainHand.getType() != Material.STICK && mainHand.getType() != Material.BLAZE_ROD)) {
            return;
        }

        // 3) Cooldown check (500ms)
        UUID playerUUID = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (mageBasicCooldown.containsKey(playerUUID)) {
            long lastUse = mageBasicCooldown.get(playerUUID);
            if (now - lastUse < 500) {
                return;
            }
        }
        mageBasicCooldown.put(playerUUID, now);

        // 4) Avoid conflicts with partial combos
        String activeCombo = ClickComboListener.getActiveCombo(player);
        if (!activeCombo.isEmpty() && activeCombo.length() < 3) {
            return;
        }

        // 5) Calculate total Intelligence from:
        //    (player's base + bonus) + (weapon's baseIntel + bonusIntel)
        int playerInt = ps.baseIntelligence + ps.bonusIntelligence;

        CustomItem cItem = ItemManager.getInstance().getCustomItemFromItemStack(mainHand);
        int weaponInt = (cItem != null) ? cItem.getIntel() : 0;

        // Example damage formula: 6 base + (0.4 * totalINT)
        double damage = 6.0 + 0.4 * (playerInt + weaponInt);

        // 6) Ray logic: go 20 blocks
        Location start = player.getEyeLocation();
        Vector direction = start.getDirection().normalize();
        double beamLength = 20.0;

        for (double i = 0; i < beamLength; i += 0.5) {
            Location point = start.clone().add(direction.clone().multiply(i));
            player.getWorld().spawnParticle(Particle.END_ROD, point, 1, 0.1, 0.1, 0.1, 0.1);

            // Check for entities in small radius
            for (Entity entity : player.getWorld().getNearbyEntities(point, 0.5, 0.5, 0.5)) {
                if (entity instanceof LivingEntity && entity != player) {
                    LivingEntity target = (LivingEntity) entity;
                    target.damage(damage, player); // single custom damage
                    player.getWorld().spawnParticle(Particle.WITCH, target.getLocation(), 10, 0.2, 0.2, 0.2);
                    player.getWorld().playSound(target.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 1f, 1f);
                    return; // stop the beam after hitting something
                }
            }

            // If we hit a non-passable block, break
            if (!point.getBlock().isPassable()) {
                player.getWorld().playSound(point, Sound.BLOCK_GLASS_BREAK, 1f, 1f);
                break;
            }
        }

        // If beam hits nothing, just play a sound at the end
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITCH_THROW, 1f, 1f);
    }

    /** ------------------------------------------------
     *  METEOR: AoE damage on impact, purely from INT
     *  ------------------------------------------------ */
    private void castMeteor(Player player) {
        // 1) Gather INT from player + weapon
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player);
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
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player);
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
    private void healPlayer(Player player, int amount) {
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        double newHealth = Math.min(player.getHealth() + amount, maxHealth);
        player.setHealth(newHealth);

        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation(), 30, 1, 1, 1);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 1f, 1f);
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
