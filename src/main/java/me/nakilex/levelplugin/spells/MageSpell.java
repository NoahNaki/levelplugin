package me.nakilex.levelplugin.spells;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.duels.managers.DuelManager;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MageSpell implements Listener {

    private final Main plugin = Main.getInstance();
    private final Logger logger = plugin.getLogger();

    private final Map<UUID, Long> mageBasicCooldown  = new HashMap<>();
    private final Map<UUID, Double> teleportManaCosts = new HashMap<>();
    private final Map<UUID, Long> lastTeleportTimes  = new HashMap<>();
    private static final Set<UUID> activeBlackholes = new HashSet<>();

    private static final double INITIAL_TELEPORT_MANA_COST = 5.0;
    private static final double TELEPORT_MANA_MULTIPLIER    = 1.2;
    private static final long   TELEPORT_RESET_TIME        = 3000L;

    public void castMageSpell(Player player, String effectKey) {
        try {
            logger.info("castMageSpell called for player=" + player.getName() + " effectKey=" + effectKey);
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
                    logger.warning("Unknown Mage Spell: " + effectKey);
                    player.sendMessage("§eUnknown Mage Spell: " + effectKey);
            }
        } catch (SpellCastCancelledException cancelled) {
            // Spell cancelled, abort without mana deduction
            return;
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error casting mage spell", ex);
        }
    }

    public void mageBasicSkill(Player player) {
        logger.info("mageBasicSkill triggered by " + player.getName());
        int range = 20;
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        double damage = 5.0 + ps.baseIntelligence + ps.bonusIntelligence * 0.5;
        logger.info("Calculated basic damage: " + damage);

        Location start = player.getEyeLocation();
        Vector direction = start.getDirection().normalize();
        World world = player.getWorld();

        world.playSound(start, Sound.ENTITY_GHAST_SHOOT, 1f, 1.2f);
        world.spawnParticle(Particle.END_ROD, start, 5, 0.1, 0.1, 0.1, 0.02);

        for (int i = 0; i < range; i++) {
            Location current = start.clone().add(direction.clone().multiply(i));
            world.spawnParticle(Particle.CRIT, current, 1, 0, 0, 0, 0);
            for (Entity entity : world.getNearbyEntities(current, 0.5, 0.5, 0.5)) {
                if (entity instanceof LivingEntity && entity != player) {
                    LivingEntity target = (LivingEntity) entity;
                    logger.info("Basic hit entity: " + target.getType() + " at " + current);
                    target.damage(damage, player);
                    target.setVelocity(direction.clone().multiply(0.2));
                    world.spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation(), 10, 0.2, 0.2, 0.2, 0.02);
                    world.playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 1f, 1.5f);
                    return;
                }
            }
            if (current.getBlock().getType().isSolid()) {
                logger.info("Basic attack hit solid block at " + current);
                world.spawnParticle(Particle.SMOKE, current, 5, 0.2, 0.2, 0.2, 0.05);
                world.playSound(current, Sound.BLOCK_STONE_HIT, 1f, 0.8f);
                return;
            }
        }

        Location end = start.clone().add(direction.multiply(range));
        logger.info("Basic ray ended without hit. End point: " + end);
        world.spawnParticle(Particle.SMOKE, end, 5, 0.2, 0.2, 0.2, 0.05);
    }

    private void castMeteor(Player player) {
        logger.info("castMeteor called by " + player.getName());
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        int playerInt = ps.baseIntelligence + ps.bonusIntelligence;

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        CustomItem cItem = ItemManager.getInstance().getCustomItemFromItemStack(mainHand);
        int weaponInt = (cItem != null) ? cItem.getIntel() : 0;

        double finalDamage = 10.0 + 2.0 * (playerInt + weaponInt);
        logger.info("Meteor damage computed: " + finalDamage);

        Location target = player.getTargetBlockExact(20) != null
            ? player.getTargetBlockExact(20).getLocation().add(0.5, 1, 0.5)
            : player.getLocation().add(player.getLocation().getDirection().normalize().multiply(5));
        logger.info("Meteor target location: " + target);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1f);
        Location spawnLocation = target.clone().add(0, 15, 0);
        Fireball fireball = player.getWorld().spawn(spawnLocation, Fireball.class);
        fireball.setShooter(player);
        fireball.setVelocity(new Vector(0, -1.5, 0));
        fireball.setIsIncendiary(false);
        fireball.setYield(0f);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!fireball.isValid()) {
                    logger.info("Meteor fireball no longer valid, cancelling trail task.");
                    cancel();
                    return;
                }
                Location fbLoc = fireball.getLocation();
                fbLoc.getWorld().spawnParticle(Particle.FLAME, fbLoc, 10, 0.3, 0.3, 0.3, 0.02);
                fbLoc.getWorld().spawnParticle(Particle.LARGE_SMOKE, fbLoc, 5, 0.2, 0.2, 0.2, 0.02);
                fbLoc.getWorld().playSound(fbLoc, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 1f);
            }
        }.runTaskTimer(plugin, 0L, 2L);

        fireball.setMetadata("Meteor", new FixedMetadataValue(plugin, true));

        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onProjectileHit(org.bukkit.event.entity.ProjectileHitEvent event) {
                if (!(event.getEntity() instanceof Fireball) || !event.getEntity().hasMetadata("Meteor")) return;
                Fireball fb = (Fireball) event.getEntity();
                if (!fb.equals(fireball)) return;
                logger.info("Meteor impacted at: " + fb.getLocation());

                fb.getWorld().spawnParticle(Particle.EXPLOSION, fb.getLocation(), 1);
                fb.getWorld().playSound(fb.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);

                double radius = 4.0;
                for (Entity e : fb.getWorld().getNearbyEntities(fb.getLocation(), radius, radius, radius)) {
                    if (e instanceof LivingEntity && e != player) {
                        LivingEntity le = (LivingEntity) e;
                        logger.info("Meteor hit entity: " + le.getType());
                        le.damage(finalDamage, player);
                        le.setFireTicks(100);
                    }
                }
                fb.remove();
                org.bukkit.event.entity.ProjectileHitEvent.getHandlerList().unregister(this);
            }
        }, plugin);
    }

    // Custom exception to signal that a spell cast was cancelled and should not consume mana
    public static class SpellCastCancelledException extends RuntimeException {
        public SpellCastCancelledException(String message) {
            super(message);
        }
    }

    private void castBlackhole(Player player) throws SpellCastCancelledException {
        // If any blackhole is currently active, cancel new cast and abort mana deduction
        if (!activeBlackholes.isEmpty()) {
            String msg = "§cA blackhole is already active!";
            player.sendMessage(msg);
            throw new SpellCastCancelledException(msg);
        }

        UUID pid = player.getUniqueId();

        // Register this player's blackhole
        activeBlackholes.add(pid);
        logger.info("Registered blackhole for player " + player.getName());

        // Gather stats
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(pid);
        double damage = 10.0 + 0.5 * (ps.baseIntelligence + ps.bonusIntelligence);

        // Compute center
        Location target = player.getEyeLocation().add(player.getLocation().getDirection().multiply(10));
        double pullRadius   = 5.0;
        double damageRadius = 1.0;

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 1f, 1f);
        createBlackholeEffect(target, pullRadius);

        // Pull and damage task
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks++ >= 50) {
                    logger.info("Blackhole expired for " + player.getName());
                    activeBlackholes.remove(pid);
                    cancel();
                    return;
                }

                for (Entity e : player.getWorld().getNearbyEntities(target, pullRadius, pullRadius, pullRadius)) {
                    if (!(e instanceof LivingEntity) || e == player) continue;
                    if (e instanceof Player && !DuelManager.getInstance().areInDuel(pid, ((Player)e).getUniqueId())) continue;

                    LivingEntity le = (LivingEntity) e;
                    Vector pullVec = target.toVector().subtract(le.getLocation().toVector()).normalize().multiply(0.2);
                    le.setVelocity(pullVec);

                    if (le.getLocation().distance(target) <= damageRadius) {
                        le.damage(damage, player);
                        le.getWorld().spawnParticle(Particle.CRIT, le.getLocation(), 10, 0.2, 0.2, 0.2, 0.02);
                    }
                }

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
                    double x   = radius * Math.cos(rad);
                    double z   = radius * Math.sin(rad);
                    Location loc = center.clone().add(x, 0, z);
                    loc.add(0, Math.sin(ticks / 10.0) * 0.5, 0);
                    center.getWorld().spawnParticle(Particle.PORTAL, loc, 1, 0, 0, 0, 0);
                    center.getWorld().spawnParticle(Particle.SMOKE,  loc, 1, 0, 0, 0, 0);
                }
                center.getWorld().spawnParticle(Particle.DRAGON_BREATH, center, 5, 0.2, 0.2, 0.2, 0.02);
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void healPlayer(Player player, int baseAmount) {
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        int intelligence = ps.baseIntelligence + ps.bonusIntelligence;
        double scaledHealing = baseAmount + (intelligence * 0.5);
        double maxHealth     = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        double newHealth     = Math.min(player.getHealth() + scaledHealing, maxHealth);

        player.setHealth(newHealth);
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation(), 30, 1, 1, 1);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 1f, 1f);
        player.sendMessage("§aYou have been healed for " + Math.round(scaledHealing) + " health!");
    }

    private void teleportPlayer(Player player, int distance, int particles) {
        Location target       = player.getLocation().add(player.getLocation().getDirection().multiply(distance));
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
            Location temp = target.clone().add(0, i, 0);
            if (ClickComboListener.isLocTpSafe(temp)) {
                return temp;
            }
        }
        return null;
    }
}
