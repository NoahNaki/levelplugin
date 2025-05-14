package me.nakilex.levelplugin.spells;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.party.Party;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.listener.ClickComboListener;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
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

    private final Map<UUID, BlackholeTasks> playerBlackholes = new HashMap<>();

    private static class BlackholeTasks {
        BukkitRunnable pullTask;
        BukkitRunnable effectTask;
    }

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
                    StatsManager.PlayerStats stats = StatsManager
                        .getInstance()
                        .getPlayerStats(player.getUniqueId());
                    int totalAgi = stats.baseAgility + stats.bonusAgility;

                    final int baseDistance = 8;
                    final double agiMultiplier = 0.05;
                    int scaledDistance = baseDistance + (int)(totalAgi * agiMultiplier);

                    scaledDistance = Math.max(baseDistance, Math.min(scaledDistance, 30));

                    teleportPlayer(player, scaledDistance, 150);
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

        world.spawnParticle(Particle.END_ROD, start, 5, 0.1, 0.1, 0.1, 0.02);

        int range = 20;
        for (int i = 0; i < range; i++) {
            Location current = start.clone().add(direction.clone().multiply(i));
            world.spawnParticle(Particle.CRIT, current, 1, 0, 0, 0, 0);

            // 3) Hit entity
            for (Entity entity : world.getNearbyEntities(current, 0.5, 0.5, 0.5)) {
                if (!(entity instanceof LivingEntity) || entity == player) continue;
                LivingEntity target = (LivingEntity) entity;

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

    /** true = we are allowed to hurt that target */
    private boolean canHit(Player caster, Entity target) {
        // Mobs are always valid targets
        if (!(target instanceof Player)) return true;
        Player p = (Player) target;
        // Only allow damage if they're in a duel together
        return DuelManager.getInstance()
            .areInDuel(caster.getUniqueId(), p.getUniqueId());
    }

    public void castMeteor(Player player) {
        plugin.getLogger().info("castMeteor called by " + player.getName());

        // 1) Compute damage
        StatsManager.PlayerStats ps = StatsManager.getInstance()
            .getPlayerStats(player.getUniqueId());
        int playerInt = ps.baseIntelligence + ps.bonusIntelligence;
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        CustomItem cItem = ItemManager.getInstance().getCustomItemFromItemStack(mainHand);
        int weaponInt = (cItem != null) ? cItem.getIntel() : 0;
        double finalDamage = 6.0 + 1.0 * (playerInt + weaponInt);

        World world = player.getWorld();

        // 2) Determine true impact point
        Block targetBlock = player.getTargetBlockExact(20);
        Location impact = (targetBlock != null)
            ? targetBlock.getLocation().add(0.5, 1, 0.5)
            : player.getLocation().add(player.getLocation().getDirection().multiply(20));

        // 3) Compute spawn location (high above)
        Location spawn = impact.clone().add(0, 30, 0);

        // 4) Play launch sound
        world.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1f);

        // 5) Animate the meteor descending
        new BukkitRunnable() {
            final Vector step = impact.toVector()
                .subtract(spawn.toVector())
                .normalize()
                .multiply(1.0); // 1 block per tick = 20 blocks/sec
            Location loc = spawn.clone();
            boolean exploded = false;

            @Override public void run() {
                if (exploded) {
                    cancel();
                    return;
                }

                // advance
                loc.add(step);

                // trail effects
                world.spawnParticle(Particle.FLAME, loc, 8, 0.2, 0.2, 0.2, 0.02);
                world.spawnParticle(Particle.LARGE_SMOKE, loc, 5, 0.2, 0.2, 0.2, 0.02);
                world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 0.3f, 1f);

                // 6a) Entity collision?
                for (Entity e : world.getNearbyEntities(loc, 1, 1, 1)) {
                    if (!(e instanceof LivingEntity le) || le == player) continue;
                    if (!canHit(player, le)) continue;

                    explodeAt(loc);
                    le.setFireTicks(100);
                    SpellUtils.dealWithChat(player, le, finalDamage, "Meteor");
                    exploded = true;
                    return;
                }

                // 6b) Reached impact point?
                if (loc.distanceSquared(impact) < 1.0) {
                    explodeAt(impact);
                    exploded = true;
                }
            }

            private void explodeAt(Location here) {
                world.spawnParticle(Particle.EXPLOSION, here, 1);
                world.playSound(here, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
                double radius = 4.0;
                for (Entity e : world.getNearbyEntities(here, radius, radius, radius)) {
                    if (!(e instanceof LivingEntity le) || le == player) continue;
                    if (!canHit(player, le)) continue;
                    le.setFireTicks(100);
                    SpellUtils.dealWithChat(player, le, finalDamage, "Meteor");
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }


    @EventHandler
    public void onFireballDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Fireball fb)) return;
        if (!fb.hasMetadata("Meteor"))            return;  // only our meteors
        if (!(event.getEntity() instanceof Player victim)) return;

        Player shooter = (fb.getShooter() instanceof Player p) ? p : null;
        if (shooter == null) return;

        // if they’re not in a duel together, cancel the hit entirely
        if (!DuelManager.getInstance()
            .areInDuel(shooter.getUniqueId(), victim.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    // Custom exception to signal that a spell cast was cancelled and should not consume mana
    public static class SpellCastCancelledException extends RuntimeException {
        public SpellCastCancelledException(String message) {
            super(message);
        }
    }

    private void castBlackhole(Player player) throws SpellCastCancelledException {
        UUID pid = player.getUniqueId();

        // Cancel any existing blackhole for this player
        if (playerBlackholes.containsKey(pid)) {
            BlackholeTasks old = playerBlackholes.remove(pid);
            if (old.pullTask != null) old.pullTask.cancel();
            if (old.effectTask != null) old.effectTask.cancel();
        }

        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(pid);
        double damage = 10.0 + 0.5 * (ps.baseIntelligence + ps.bonusIntelligence);

        Block targetBlock = player.getTargetBlockExact(20);
        Location center;
        if (targetBlock != null) {
            center = targetBlock.getLocation().add(0.5, 1, 0.5);
        } else {
            center = player.getEyeLocation().add(player.getLocation().getDirection().multiply(10));
        }

        double pullRadius = 5.0;
        double damageRadius = 5.0;

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1f, 1f);

        // Start effect and pull tasks
        BlackholeTasks tasks = new BlackholeTasks();
        tasks.effectTask = createBlackholeEffect(center, pullRadius);
        tasks.pullTask = createPullAndDamageTask(player, center, pullRadius, damageRadius, damage);

        playerBlackholes.put(pid, tasks);
    }

    private BukkitRunnable createBlackholeEffect(Location center, double radius) {
        BukkitRunnable effectTask = new BukkitRunnable() {
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
                    Location loc = center.clone().add(x, 0, z).add(0, Math.sin(ticks / 10.0) * 0.5, 0);
                    center.getWorld().spawnParticle(Particle.PORTAL, loc, 1, 0, 0, 0, 0);
                    center.getWorld().spawnParticle(Particle.SMOKE, loc, 1, 0, 0, 0, 0);
                }
                center.getWorld().spawnParticle(Particle.DRAGON_BREATH, center, 5, 0.2, 0.2, 0.2, 0.02);
            }
        };
        effectTask.runTaskTimer(plugin, 0L, 2L);
        return effectTask;
    }

    private BukkitRunnable createPullAndDamageTask(Player player, Location center, double pullRadius, double damageRadius, double damage) {
        BukkitRunnable pullTask = new BukkitRunnable() {
            int ticks = 0;

            @Override public void run() {
                if (ticks++ >= 50) {
                    playerBlackholes.remove(player.getUniqueId());
                    cancel();
                    return;
                }

                for (Entity e : player.getWorld().getNearbyEntities(center, pullRadius, pullRadius, pullRadius)) {
                    if (!(e instanceof LivingEntity) || e == player) continue;
                    if (e instanceof Player && !DuelManager.getInstance().areInDuel(player.getUniqueId(), ((Player) e).getUniqueId())) continue;

                    LivingEntity le = (LivingEntity) e;
                    Vector pullVec = center.toVector().subtract(le.getLocation().toVector()).normalize().multiply(0.2);
                    le.setVelocity(pullVec);

                    if (le.getLocation().distance(center) <= damageRadius) {
                        le.setFireTicks(100);
                        SpellUtils.dealWithChat(player, le, damage, "Blackhole");
                        return;
                    }
                }
            }
        };
        pullTask.runTaskTimer(plugin, 0L, 2L);
        return pullTask;
    }

    /** Heals the caster and nearby party members, but never a duel opponent. */
    private void healPlayer(Player player, int baseAmount) {

        /* ── 1) Compute heal amount ─────────────────────────────── */
        StatsManager.PlayerStats ps = StatsManager.getInstance()
            .getPlayerStats(player.getUniqueId());
        int    intel = ps.baseIntelligence + ps.bonusIntelligence;
        double heal  = baseAmount + (intel * 0.5);

        /* ── 2) Build target list (self + party-mates) ──────────── */
        List<Player> toHeal = new ArrayList<>();
        toHeal.add(player);                                    // self is always OK

        DuelManager dm = DuelManager.getInstance();
        Party party    = Main.getInstance()
            .getPartyManager()
            .getParty(player.getUniqueId());

        if (party != null) {
            for (UUID id : party.getMembers()) {
                if (id.equals(player.getUniqueId())) continue;          // skip self

                Player m = Bukkit.getPlayer(id);
                if (m == null || !m.isOnline()) continue;

                // nearby & same world
                if (!m.getWorld().equals(player.getWorld())
                    || m.getLocation().distanceSquared(player.getLocation()) > 10*10)
                    continue;

                // ✘ skip if this party-mate is currently duelling the caster
                if (dm.areInDuel(player.getUniqueId(), m.getUniqueId())) continue;

                toHeal.add(m);   // ✔ safe to heal
            }
        }

        /* ── 3) Apply heal, VFX/SFX, messages ───────────────────── */
        for (Player target : toHeal) {

            double maxHp = target.getAttribute(Attribute.MAX_HEALTH).getValue();
            target.setHealth(Math.min(target.getHealth() + heal, maxHp));

            target.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                target.getLocation(), 30, 1, 1, 1, 0.2);
            target.getWorld().playSound(target.getLocation(),
                Sound.BLOCK_AMETHYST_BLOCK_HIT, 1f, 1f);

            if (target.equals(player)) {
                target.sendMessage("§aYou have been healed for " + Math.round(heal) + " health!");
            } else {
                target.sendMessage("§a" + player.getName()
                    + " healed you for " + Math.round(heal) + " health!");
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