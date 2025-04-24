package me.nakilex.levelplugin.spells;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.party.Party;
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
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MageSpell implements Listener {

    private final Main plugin = Main.getInstance();
    private final Logger logger = plugin.getLogger();

    private final Map<UUID, Long> mageBasicCooldown = new HashMap<>();
    private final Map<UUID, Double> teleportManaCosts = new HashMap<>();
    private final Map<UUID, Long> lastTeleportTimes = new HashMap<>();
    private static final Set<UUID> activeBlackholes = new HashSet<>();

    private static final double INITIAL_TELEPORT_MANA_COST = 5.0;
    private static final double TELEPORT_MANA_MULTIPLIER = 1.2;
    private static final long TELEPORT_RESET_TIME = 3000L;

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
        plugin.getLogger().info("mageBasicSkill triggered by " + player.getName());

        // 1) Compute damage
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        double damage = 5.0 + ps.baseIntelligence + ps.bonusIntelligence * 0.5;

        // 2) Raycast setup
        Location start = player.getEyeLocation();
        Vector direction = start.getDirection().normalize();
        World world = player.getWorld();

        world.playSound(start, Sound.ENTITY_GHAST_SHOOT, 1f, 1.2f);
        world.spawnParticle(Particle.END_ROD, start, 5, 0.1, 0.1, 0.1, 0.02);

        int range = 20;
        for (int i = 0; i < range; i++) {
            Location current = start.clone().add(direction.clone().multiply(i));
            world.spawnParticle(Particle.CRIT, current, 1, 0, 0, 0, 0);

            // 3) Hit entity
            for (Entity entity : world.getNearbyEntities(current, 0.5, 0.5, 0.5)) {
                if (!(entity instanceof LivingEntity) || entity == player) continue;
                LivingEntity target = (LivingEntity) entity;

                // knockback & VFX
                target.setVelocity(direction.clone().multiply(0.2));
                world.spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation(), 10, 0.2, 0.2, 0.2, 0.02);
                world.playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 1f, 1.5f);

                // unified damage + chat
                me.nakilex.levelplugin.spells.utils.SpellUtils.dealWithChat(
                    player,
                    target,
                    damage,
                    "Basic Mage Attack"
                );
                return;
            }

            // 4) Hit block
            if (current.getBlock().getType().isSolid()) {
                world.spawnParticle(Particle.SMOKE, current, 5, 0.2, 0.2, 0.2, 0.05);
                world.playSound(current, Sound.BLOCK_STONE_HIT, 1f, 0.8f);
                return;
            }
        }

        // 5) Missed everything
        Location end = start.clone().add(direction.multiply(range));
        world.spawnParticle(Particle.SMOKE, end, 5, 0.2, 0.2, 0.2, 0.05);
    }



    private void castMeteor(Player player) {
        plugin.getLogger().info("castMeteor called by " + player.getName());

        // Compute damage
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        int playerInt = ps.baseIntelligence + ps.bonusIntelligence;
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        CustomItem cItem = ItemManager.getInstance().getCustomItemFromItemStack(mainHand);
        int weaponInt = (cItem != null) ? cItem.getIntel() : 0;
        double finalDamage = 6.0 + 1.0 * (playerInt + weaponInt);

        // Determine target location
        Location target = Optional.ofNullable(player.getTargetBlockExact(20))
            .map(b -> b.getLocation().add(0.5, 1, 0.5))
            .orElseGet(() -> player.getLocation().add(player.getLocation().getDirection().normalize().multiply(5)));

        // Launch fireball
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1f);
        Location spawnLocation = target.clone().add(0, 15, 0);
        Fireball fireball = player.getWorld().spawn(spawnLocation, Fireball.class);
        fireball.setShooter(player);
        fireball.setVelocity(new Vector(0, -1.5, 0));
        fireball.setIsIncendiary(false);
        fireball.setYield(0f);
        fireball.setMetadata("Meteor", new FixedMetadataValue(plugin, true));

        // Trail effect
        new BukkitRunnable() {
            @Override public void run() {
                if (!fireball.isValid()) { cancel(); return; }
                Location fbLoc = fireball.getLocation();
                fbLoc.getWorld().spawnParticle(Particle.FLAME, fbLoc, 10, 0.3, 0.3, 0.3, 0.02);
                fbLoc.getWorld().spawnParticle(Particle.LARGE_SMOKE, fbLoc, 5, 0.2, 0.2, 0.2, 0.02);
                fbLoc.getWorld().playSound(fbLoc, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 1f);
            }
        }.runTaskTimer(plugin, 0L, 2L);

        // Impact listener
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler public void onProjectileHit(ProjectileHitEvent event) {
                if (!(event.getEntity() instanceof Fireball fb)
                    || !fb.hasMetadata("Meteor")
                    || !fb.equals(fireball)) return;

                World w = fb.getWorld();
                Location loc = fb.getLocation();
                w.spawnParticle(Particle.EXPLOSION, loc, 1);
                w.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);

                double radius = 4.0;
                for (Entity e : w.getNearbyEntities(loc, radius, radius, radius)) {
                    if (e instanceof LivingEntity le && le != player) {
                        le.setFireTicks(100);
                        // unified damage + chat
                        me.nakilex.levelplugin.spells.utils.SpellUtils.dealWithChat(
                            player,
                            le,
                            finalDamage,
                            "Meteor"
                        );
                    }
                }

                fb.remove();
                ProjectileHitEvent.getHandlerList().unregister(this);
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
        // Prevent overlapping blackholes
        if (!activeBlackholes.isEmpty()) {
            String msg = "§cA blackhole is already active!";
            player.sendMessage(msg);
            throw new SpellCastCancelledException(msg);
        }
        UUID pid = player.getUniqueId();
        activeBlackholes.add(pid);

        // Compute damage
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(pid);
        double damage = 10.0 + 0.5 * (ps.baseIntelligence + ps.bonusIntelligence);

        // Setup center & radii
        Location center   = player.getEyeLocation().add(player.getLocation().getDirection().multiply(10));
        double pullRadius = 5.0;
        double damageRadius = 5.0;

        // Initial VFX/SFX
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 1f, 1f);
        createBlackholeEffect(center, pullRadius);

        // Pull & damage loop
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks++ >= 50) {
                    activeBlackholes.remove(pid);
                    cancel();
                    return;
                }

                for (Entity e : player.getWorld().getNearbyEntities(center, pullRadius, pullRadius, pullRadius)) {
                    if (!(e instanceof LivingEntity) || e == player) continue;
                    if (e instanceof Player
                        && !DuelManager.getInstance().areInDuel(pid, ((Player) e).getUniqueId())) continue;

                    LivingEntity le = (LivingEntity) e;
                    // pull them in
                    Vector pullVec = center.toVector().subtract(le.getLocation().toVector())
                        .normalize().multiply(0.2);
                    le.setVelocity(pullVec);

                    // when in range, apply damage + chat & fire once
                    if (le.getLocation().distance(center) <= damageRadius) {
                        le.setFireTicks(100);
                        me.nakilex.levelplugin.spells.utils.SpellUtils.dealWithChat(
                            player,
                            le,
                            damage,
                            "Blackhole"
                        );
                        return;
                    }
                }

                // ongoing VFX/SFX
                for (double angle = 0; angle < 360; angle += 10) {
                    double rad = Math.toRadians(angle);
                    double x   = pullRadius * Math.cos(rad);
                    double z   = pullRadius * Math.sin(rad);
                    Location dust = center.clone().add(x, 0, z)
                        .add(0, Math.sin(ticks / 10.0) * 0.5, 0);
                    center.getWorld().spawnParticle(Particle.PORTAL, dust, 1, 0, 0, 0, 0);
                }
                center.getWorld().playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.5f, 1.2f);
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
                    Location loc = center.clone().add(x, 0, z);
                    loc.add(0, Math.sin(ticks / 10.0) * 0.5, 0);
                    center.getWorld().spawnParticle(Particle.PORTAL, loc, 1, 0, 0, 0, 0);
                    center.getWorld().spawnParticle(Particle.SMOKE, loc, 1, 0, 0, 0, 0);
                }
                center.getWorld().spawnParticle(Particle.DRAGON_BREATH, center, 5, 0.2, 0.2, 0.2, 0.02);
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void healPlayer(Player player, int baseAmount) {
        // 1) Compute how much to heal
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        int intel = ps.baseIntelligence + ps.bonusIntelligence;
        double amount = baseAmount + (intel * 0.5);

        // 2) Gather all targets: caster + nearby party members
        List<Player> toHeal = new ArrayList<>();
        toHeal.add(player);

        Party party = Main.getInstance()
            .getPartyManager()
            .getParty(player.getUniqueId());
        if (party != null) {
            for (UUID memberId : party.getMembers()) {
                if (memberId.equals(player.getUniqueId())) continue;
                Player member = Bukkit.getPlayer(memberId);
                if (member != null
                    && member.isOnline()
                    && member.getWorld().equals(player.getWorld())
                    && member.getLocation().distanceSquared(player.getLocation()) <= 10 * 10) {
                    toHeal.add(member);
                }
            }
        }

        // 3) Apply heal + VFX/SFX + messaging
        for (Player target : toHeal) {
            double maxHp = target.getAttribute(Attribute.MAX_HEALTH).getValue();
            double newHp = Math.min(target.getHealth() + amount, maxHp);
            target.setHealth(newHp);

            // Particles & sound
            target.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                target.getLocation(),
                30, 1, 1, 1, 0.2);
            target.getWorld().playSound(target.getLocation(),
                Sound.BLOCK_AMETHYST_BLOCK_HIT,
                1f, 1f);

            // Message
            if (target.equals(player)) {
                target.sendMessage("§aYou have been healed for " + Math.round(amount) + " health!");
            } else {
                target.sendMessage("§a" + player.getName()
                    + " healed you for "
                    + Math.round(amount) + " health!");
            }
        }
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
            Location temp = target.clone().add(0, i, 0);
            if (ClickComboListener.isLocTpSafe(temp)) {
                return temp;
            }
        }
        return null;
    }

    private String capitalize(String s) {
        s = s.toLowerCase();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}