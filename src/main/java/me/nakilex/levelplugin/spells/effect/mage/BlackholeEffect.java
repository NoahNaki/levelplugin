package me.nakilex.levelplugin.spells.effect.mage;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import de.slikey.effectlib.effect.CircleEffect;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BlackholeEffect implements SpellEffect {

    // track one blackhole task per player
    private static final Map<UUID, BlackholeTask> activeBlackholes = new ConcurrentHashMap<>();

    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        Main plugin = Main.getInstance();
        UUID pid = player.getUniqueId();

        // 1) Compute damage scaled by intelligence
        StatsManager.PlayerStats stats =
            StatsManager.getInstance().getPlayerStats(pid);
        int playerInt = stats.baseIntelligence + stats.bonusIntelligence;
        CustomItem cItem = ItemManager.getInstance()
            .getCustomItemFromItemStack(player.getInventory().getItemInMainHand());
        int weaponInt = cItem != null ? cItem.getIntel() : 0;

        double rawDamage = ctx.getBaseSpell().getBaseDamage() + playerInt + weaponInt;
        double dmgMultiplier = ctx.getFinalDamage() / ctx.getBaseSpell().getBaseDamage();
        double finalDamage = rawDamage * dmgMultiplier;

        // 2) Rune flags
        boolean allowMultiple = parseBoolean(ctx.getExtraParam("allowMultiple"), false);
        boolean allowMove     = parseBoolean(ctx.getExtraParam("allowMove"), false);

        // 3) Determine center point for the blackhole
        Block target = player.getTargetBlockExact(20);
        Location center;
        if (target != null) {
            center = target.getLocation().add(0.5, 1.5, 0.5);
        } else {
            center = player.getEyeLocation()
                .add(player.getLocation().getDirection().multiply(10));
        }

        // 4) Enforce single-instance logic
        BlackholeTask existing = activeBlackholes.get(pid);
        if (existing != null) {
            if (allowMove) {
                existing.setCenter(center);
                //player.sendMessage("Your blackhole has been relocated.");
                return;
            } else if (!allowMultiple) {
                player.sendMessage(ChatColor.RED + "You can only have one blackhole at a time.");
                return;
            }
            // if allowMultiple==true, fall through and spawn another
        }

        // 5) Read rest of rune-driven parameters
        double pullRadius       = parseDouble(ctx.getExtraParam("pullRadius"), 5.0);
        double radiusGrowthRate = parseDouble(ctx.getExtraParam("radiusGrowthRate"), 0.0);
        double rotationSpeed    = parseDouble(ctx.getExtraParam("rotationSpeed"), 0.0);
        int    durationTicks    = parseInt   (ctx.getExtraParam("durationTicks"), 50);
        boolean endExplosion    = parseBoolean(ctx.getExtraParam("endExplosion"), false);
        double explosionPower   = parseDouble(ctx.getExtraParam("explosionPower"), 2.0);

        // 6) Spawn & register new blackhole task
        BlackholeTask task = new BlackholeTask(
            plugin, player, center,
            finalDamage,
            pullRadius, radiusGrowthRate, rotationSpeed,
            durationTicks, endExplosion, explosionPower
        );
        task.runTaskTimer(plugin, 0L, 2L);
        activeBlackholes.put(pid, task);
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Task which handles visuals, pull, damage, and cleanup for one blackhole
    // ────────────────────────────────────────────────────────────────────────────
    private static class BlackholeTask extends BukkitRunnable {
        private final Main plugin;
        private final Player player;
        private final UUID pid;
        private Location center;
        private final double finalDamage;
        private final double pullRadius;
        private final double radiusGrowthRate;
        private final double rotationSpeed;
        private final int durationTicks;
        private final boolean endExplosion;
        private final double explosionPower;

        private final List<FallingBlock> pulls = new ArrayList<>();
        private int ticks = 0;
        private static final double CORE_RADIUS = 1.0;
        private static final int SPHERE_RINGS = 6;

        BlackholeTask(Main plugin, Player player, Location center,
                      double finalDamage,
                      double pullRadius, double radiusGrowthRate, double rotationSpeed,
                      int durationTicks, boolean endExplosion, double explosionPower) {
            this.plugin            = plugin;
            this.player            = player;
            this.pid               = player.getUniqueId();
            this.center            = center.clone();
            this.finalDamage       = finalDamage;
            this.pullRadius        = pullRadius;
            this.radiusGrowthRate  = radiusGrowthRate;
            this.rotationSpeed     = rotationSpeed;
            this.durationTicks     = durationTicks;
            this.endExplosion      = endExplosion;
            this.explosionPower    = explosionPower;
        }

        void setCenter(Location newCenter) {
            this.center = newCenter.clone();
        }

        @Override
        public void run() {
            // End condition: cleanup and cancel
            if (++ticks > durationTicks) {
                // remove any lingering blocks
                for (FallingBlock fb : pulls) {
                    if (!fb.isDead()) fb.remove();
                }
                pulls.clear();
                activeBlackholes.remove(pid);

                if (endExplosion) {
                    center.getWorld().createExplosion(center, (float)explosionPower, false, false);
                }
                cancel();
                return;
            }

            double currentRadius   = pullRadius + ticks * radiusGrowthRate;
            double rotationAngle   = ticks * rotationSpeed;

            // 1) Flat vortex ring
            for (double deg = 0; deg < 360; deg += 12) {
                double rad = Math.toRadians(deg + rotationAngle);
                Location v = center.clone().add(
                    Math.cos(rad) * currentRadius,
                    0,
                    Math.sin(rad) * currentRadius
                );
                center.getWorld().spawnParticle(Particle.LARGE_SMOKE, v, 0, 0,0,0,1);
            }

            // 2) Core sphere
            for (int lat = 0; lat < SPHERE_RINGS; lat++) {
                double phi = Math.PI * lat / (SPHERE_RINGS - 1);
                for (int lon = 0; lon < SPHERE_RINGS * 2; lon++) {
                    double theta = 2 * Math.PI * lon / (SPHERE_RINGS * 2);
                    double x = CORE_RADIUS * Math.sin(phi) * Math.cos(theta);
                    double y = CORE_RADIUS * Math.cos(phi);
                    double z = CORE_RADIUS * Math.sin(phi) * Math.sin(theta);
                    Location s = center.clone().add(x, y, z);
                    center.getWorld().spawnParticle(Particle.PORTAL, s, 0,0,0,0,1);
                }
            }

            // 2b) Purple circle in the centre
            CircleEffect c = new CircleEffect(Main.getInstance().getEffectManager());
            c.setLocation(center);
            c.particle = Particle.WITCH;
            c.radius = (float) CORE_RADIUS;
            c.particles = 20;
            c.iterations = 1;
            c.run();

            // 3) Spawn ground‐blocks every 4 ticks
            if (ticks % 4 == 0) {
                double ang  = Math.random() * 2 * Math.PI;
                double dist = Math.random() * currentRadius;
                Location sample = center.clone().add(
                    Math.cos(ang) * dist, 0, Math.sin(ang) * dist
                );
                Block ground = sample.getWorld().getHighestBlockAt(sample);
                Location spawnLoc = ground.getLocation().add(0.5, 0.1, 0.5);
                FallingBlock fb = center.getWorld().spawnFallingBlock(
                    spawnLoc, ground.getBlockData()
                );
                fb.setGravity(false);
                fb.setDropItem(false);
                pulls.add(fb);
            }

            // 4) Pull existing blocks toward core
            Iterator<FallingBlock> it = pulls.iterator();
            while (it.hasNext()) {
                FallingBlock fb = it.next();
                Vector toCenter = center.toVector().subtract(fb.getLocation().toVector());
                fb.setVelocity(toCenter.normalize().multiply(0.2));
                if (fb.getLocation().distanceSquared(center) <= CORE_RADIUS * CORE_RADIUS) {
                    fb.remove();
                    it.remove();
                }
            }

            // 5) Damage & pull entities
            for (Entity e : center.getWorld().getNearbyEntities(center, pullRadius, pullRadius, pullRadius)) {
                if (!(e instanceof LivingEntity le) || le == player) continue;
                if (le instanceof Player
                    && !DuelManager.getInstance().areInDuel(pid, ((Player) le).getUniqueId())) continue;

                double dist = le.getLocation().distance(center);
                Vector dir = center.toVector().subtract(le.getLocation().toVector()).normalize();
                le.setVelocity(dir.multiply(0.2));
                if (dist <= 1.5) {
                    SpellUtils.dealWithChat(player, le, finalDamage, "Blackhole");
                }
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Helper parsers
    // ────────────────────────────────────────────────────────────────────────────
    private double parseDouble(Object param, double defaultVal) {
        if (param instanceof Number) return ((Number) param).doubleValue();
        if (param instanceof String) {
            try { return Double.parseDouble((String) param); }
            catch (NumberFormatException ignored) {}
        }
        return defaultVal;
    }

    private int parseInt(Object param, int defaultVal) {
        if (param instanceof Number) return ((Number) param).intValue();
        if (param instanceof String) {
            try { return Integer.parseInt((String) param); }
            catch (NumberFormatException ignored) {}
        }
        return defaultVal;
    }

    private String parseString(Object param, String defaultVal) {
        return (param instanceof String) ? (String) param : defaultVal;
    }

    private boolean parseBoolean(Object param, boolean defaultVal) {
        if (param instanceof Boolean) return (Boolean) param;
        if (param instanceof String)  return Boolean.parseBoolean((String) param);
        return defaultVal;
    }
}
