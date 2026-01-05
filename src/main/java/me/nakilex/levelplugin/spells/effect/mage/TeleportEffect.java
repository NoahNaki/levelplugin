package me.nakilex.levelplugin.spells.effect.mage;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import me.nakilex.levelplugin.spells.context.SpellCastContextCompat;
import me.nakilex.levelplugin.utils.TeleportUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class TeleportEffect implements SpellEffect {

    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        Main plugin = Main.getInstance();
        UUID pid = player.getUniqueId();

        // 1) Rune-driven params (larger default Aftershock AOE)
        int    baseDistance       = parseInt   (ctx.getExtraParam("baseDistance"),      8);
        double agiMultiplier      = parseDouble(ctx.getExtraParam("agiMultiplier"),   0.05);
        int    maxDistance        = parseInt   (ctx.getExtraParam("maxDistance"),      30);
        double damageTrail        = parseDouble(ctx.getExtraParam("damageTrail"),     0.0);
        double damageTrailStep    = parseDouble(ctx.getExtraParam("damageTrailStep"), 1.0);
        double damageTrailRange   = parseDouble(ctx.getExtraParam("damageTrailRange"), 4.0); // bigger AOE
        boolean leaveTrail        = parseBoolean(ctx.getExtraParam("leaveTrail"),     false);
        String trailParticle      = parseString(ctx.getExtraParam("trailParticle"),  "DRAGON_BREATH");
        int    trailCount         = parseInt   (ctx.getExtraParam("trailCount"),       150);
        boolean explosionOnArrive = parseBoolean(ctx.getExtraParam("explosionOnArrive"), false);
        double explosionPower     = parseDouble(ctx.getExtraParam("explosionPower"),   2.0);
        int    chainTeleports     = parseInt   (ctx.getExtraParam("chainTeleports"),   0);
        int    chainInterval      = parseInt   (ctx.getExtraParam("chainInterval"),    10);
        double safeSearchRange    = parseDouble(ctx.getExtraParam("safeSearchRange"),  1.0);

        // 2) Compute fixed distance & direction
        StatsManager.PlayerStats stats = StatsManager.getInstance().getPlayerStats(pid);
        int totalAgi = stats.baseAgility + stats.bonusAgility;
        final int distance = Math.max(
            baseDistance,
            Math.min(baseDistance + (int)(totalAgi * agiMultiplier), maxDistance)
        );
        final Vector dir = player.getLocation().getDirection().normalize();

        // 3) Prepare a counter for logging
        AtomicInteger teleportCount = new AtomicInteger();

        // 4) A helper to do one teleport (logs, particles, damage‐trail, explosion, etc.)
        Runnable doTeleport = () -> {
            int count = teleportCount.incrementAndGet();
            Location origin = player.getLocation();
            Location rawTarget = TeleportUtils.resolveLineOfSightTarget(player, dir, distance, 0.5);
            if (rawTarget == null) {
                rawTarget = origin.clone().add(dir.clone().multiply(distance));
            }
            Location safe = findSafeLocation(rawTarget, safeSearchRange, player);
            if (safe == null) {
                safe = findNearbySafeLocation(rawTarget, Math.max(3, (int) Math.ceil(safeSearchRange)), player);
            }
            if (safe == null) {
                plugin.getLogger().warning("[TeleportEffect] Teleport #" + count + " failed: no safe spot");
                return;
            }
            Location facing = origin.clone();
            safe.setYaw(facing.getYaw());
            safe.setPitch(facing.getPitch());

            double actualDist = origin.distance(safe);
            plugin.getLogger().info(String.format(
                "[TeleportEffect] Teleport #%d: from %s to %s (dist=%.1f)",
                count, formatLoc(origin), formatLoc(safe), actualDist
            ));

            // Aftershock: damage along path
            if (damageTrail > 0) {
                Location stepLoc = origin.clone();
                int steps = (int)Math.ceil(actualDist / damageTrailStep);
                for (int i = 0; i < steps; i++) {
                    stepLoc.add(dir.clone().multiply(damageTrailStep));
                    for (Entity e : origin.getWorld().getNearbyEntities(
                        stepLoc, damageTrailRange, damageTrailRange, damageTrailRange)) {
                        if (e instanceof LivingEntity le && !le.equals(player)) {
                            SpellUtils.dealWithChat(player, le, damageTrail, "Aftershock");
                        }
                    }
                }
            }

            // Trail of particles
            if (leaveTrail) {
                Vector delta = safe.toVector().subtract(origin.toVector())
                    .normalize()
                    .multiply(actualDist / (double)trailCount);
                Location p = origin.clone();
                for (int i = 0; i < trailCount; i++) {
                    origin.getWorld().spawnParticle(
                        Particle.valueOf(trailParticle), p, 1, 0,0,0,0
                    );
                    p.add(delta);
                }
            }

            // The blink itself (no blindness)
            TeleportUtils.teleportWithEffect(player, safe, 0L, false);

            // Mark the spell as successfully cast so mana is consumed
            SpellCastContextCompat.markSuccess(ctx, true);

            // Explosion on arrival (no knockback)
            if (explosionOnArrive) {
                safe.getWorld().createExplosion(safe, (float)explosionPower, false, false);
                player.setVelocity(new Vector(0,0,0));
                player.setFallDistance(0);
            }
        };

        // 5) Execute the first teleport immediately
        doTeleport.run();

        // 6) Schedule chain teleports
        for (int i = 1; i <= chainTeleports; i++) {
            new BukkitRunnable() {
                @Override public void run() {
                    doTeleport.run();
                }
            }.runTaskLater(plugin, chainInterval * i);
        }
    }

    // Helper to format a Location as "x,y,z"
    private String formatLoc(Location loc) {
        return String.format("%.1f,%.1f,%.1f",
            loc.getX(), loc.getY(), loc.getZ()
        );
    }



    /**
     * Checks vertical offsets from -range to +range for a safe spot.
     */
    private Location findSafeLocation(Location target, double range, Player player) {
        for (int dy = (int)-range; dy <= range; dy++) {
            Location temp = target.clone().add(0, dy, 0);
            if (isLocationTpSafe(temp)) {
                return temp;
            }
        }
        return null;
    }

    private Location findNearbySafeLocation(Location target, int radius, Player player) {
        if (target == null || target.getWorld() == null) {
            return null;
        }
        for (int r = 0; r <= radius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) {
                        continue;
                    }
                    Location candidate = target.clone().add(dx, 0, dz);
                    Location safe = findSafeLocation(candidate, 2, player);
                    if (safe != null) {
                        return safe;
                    }
                }
            }
        }
        return null;
    }

    private boolean isLocationTpSafe(Location location) {
        var block = location.getBlock();
        if (block.isLiquid() || !block.isPassable()) return false;
        return location.clone().add(0, 1, 0).getBlock().isPassable();
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Helper parsers for rune params
    // ────────────────────────────────────────────────────────────────────────────
    private double parseDouble(Object param, double defaultVal) {
        if (param instanceof Number) return ((Number)param).doubleValue();
        if (param instanceof String) {
            try { return Double.parseDouble((String)param); }
            catch (NumberFormatException ignored) {}
        }
        return defaultVal;
    }

    private int parseInt(Object param, int defaultVal) {
        if (param instanceof Number) return ((Number)param).intValue();
        if (param instanceof String) {
            try { return Integer.parseInt((String)param); }
            catch (NumberFormatException ignored) {}
        }
        return defaultVal;
    }

    private boolean parseBoolean(Object param, boolean defaultVal) {
        if (param instanceof Boolean) return (Boolean)param;
        if (param instanceof String)  return Boolean.parseBoolean((String)param);
        return defaultVal;
    }

    private String parseString(Object param, String defaultVal) {
        return (param instanceof String) ? (String)param : defaultVal;
    }
}
