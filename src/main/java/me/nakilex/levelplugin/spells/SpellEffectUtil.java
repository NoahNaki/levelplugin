package me.nakilex.levelplugin.spells;

import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.Bukkit;
import org.bukkit.EntityEffect;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Light;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

public final class SpellEffectUtil {
    public static final String BYPASS_STAT_SCALING_META = "BypassStatScaling";
    private static final int INVULNERABILITY_BYPASS_WINDOW_TICKS = 12;
    private static final Map<UUID, Integer> INVULNERABILITY_BYPASS_REMAINING = new HashMap<>();
    private static final Map<UUID, Integer> ORIGINAL_MAX_NO_DAMAGE_TICKS = new HashMap<>();
    private static BukkitTask invulnerabilityBypassTickerTask;
    private static final double HURT_KNOCKBACK_HORIZONTAL = 0.20;
    private static final double HURT_KNOCKBACK_VERTICAL = 0.08;

    private SpellEffectUtil() {
    }

    public static List<LivingEntity> getLivingTargets(Location center, double radius,
                                                      Predicate<LivingEntity> filter) {
        if (center == null || center.getWorld() == null) {
            return List.of();
        }
        double radiusSq = radius * radius;
        List<LivingEntity> targets = new ArrayList<>();
        for (var entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            if (living.isDead() || living instanceof ArmorStand) {
                continue;
            }
            if (living.getLocation().distanceSquared(center) > radiusSq) {
                continue;
            }
            if (filter != null && !filter.test(living)) {
                continue;
            }
            targets.add(living);
        }
        return targets;
    }

    public static void applyAreaDamage(Player source, Location center, double radius, double damage) {
        if (source == null || center == null) {
            return;
        }
        for (LivingEntity target : getLivingTargets(center, radius, living -> !living.equals(source))) {
            target.damage(damage, source);
        }
    }

    public static double computeIntTecScaledDamage(Player caster,
                                                   double baseDamage,
                                                   double intelligenceScale,
                                                   double techniqueScale) {
        if (caster == null) {
            return Math.max(0.0, baseDamage);
        }
        StatsManager.PlayerStats stats = StatsManager.getInstance().getPlayerStats(caster.getUniqueId());
        int intelligence = stats.baseIntelligence + stats.bonusIntelligence;
        int technique = stats.baseTechnique + stats.bonusTechnique;
        double value = Math.max(0.0, baseDamage + intelligence * intelligenceScale);
        return value * (1.0 + technique * techniqueScale);
    }

    public static void applyDirectSpellDamage(Plugin plugin,
                                              Player caster,
                                              LivingEntity target,
                                              double damage) {
        applyDirectSpellDamage(plugin, caster, target, damage, false);
    }

    public static void applyDirectSpellDamage(Plugin plugin,
                                              Player caster,
                                              LivingEntity target,
                                              double damage,
                                              boolean resetInvulnerabilityFrames) {
        if (plugin == null || caster == null || target == null || damage <= 0.0 || target.isDead()) {
            return;
        }

        double startingHealth = target.getHealth();
        caster.setMetadata(BYPASS_STAT_SCALING_META, new FixedMetadataValue(plugin, true));
        try {
            if (resetInvulnerabilityFrames) {
                withTemporaryInvulnerabilityBypass(target, () -> target.damage(damage, caster));
                registerInvulnerabilityBypassWindow(plugin, target, INVULNERABILITY_BYPASS_WINDOW_TICKS);
            } else {
                target.damage(damage, caster);
            }
        } finally {
            caster.removeMetadata(BYPASS_STAT_SCALING_META, plugin);
        }

        if (resetInvulnerabilityFrames) {
            if (didNotTakeDamage(startingHealth, target)) {
                applyGuaranteedHealthDamage(target, damage);
            }
            playHurtFeedback(target, caster);
            applyHurtKnockback(target, caster);
        }
    }

    private static void playHurtFeedback(LivingEntity target, Player attacker) {
        if (target == null || target.isDead()) {
            return;
        }
        float yaw = attacker != null ? attacker.getLocation().getYaw() : target.getLocation().getYaw();
        try {
            Method playHurtAnimation = target.getClass().getMethod("playHurtAnimation", float.class);
            playHurtAnimation.invoke(target, yaw);
            return;
        } catch (ReflectiveOperationException ignored) {
            // Fallback for API versions that do not expose playHurtAnimation(float).
        }
        target.playEffect(EntityEffect.HURT);
    }

    private static void applyHurtKnockback(LivingEntity target, Player attacker) {
        if (target == null || attacker == null || target.isDead() || target.getWorld() == null || attacker.getWorld() == null) {
            return;
        }
        if (!target.getWorld().equals(attacker.getWorld())) {
            return;
        }

        Vector away = target.getLocation().toVector().subtract(attacker.getLocation().toVector());
        away.setY(0.0);
        if (away.lengthSquared() < 0.0001) {
            away = attacker.getLocation().getDirection().clone().setY(0.0).multiply(-1.0);
        }
        if (away.lengthSquared() < 0.0001) {
            return;
        }
        away.normalize().multiply(HURT_KNOCKBACK_HORIZONTAL).setY(HURT_KNOCKBACK_VERTICAL);
        target.setVelocity(target.getVelocity().multiply(0.8).add(away));
    }

    private static boolean didNotTakeDamage(double startingHealth, LivingEntity target) {
        if (target == null || target.isDead()) {
            return false;
        }
        return target.getHealth() >= startingHealth - 0.0001;
    }

    private static void applyGuaranteedHealthDamage(LivingEntity target, double damage) {
        if (target == null || target.isDead() || damage <= 0.0) {
            return;
        }
        target.setHealth(Math.max(0.0, target.getHealth() - damage));
    }

    private static void registerInvulnerabilityBypassWindow(Plugin plugin,
                                                            LivingEntity target,
                                                            int durationTicks) {
        if (plugin == null || target == null || durationTicks <= 0 || !target.isValid() || target.isDead()) {
            return;
        }
        UUID targetId = target.getUniqueId();
        INVULNERABILITY_BYPASS_REMAINING.put(targetId, durationTicks);
        ORIGINAL_MAX_NO_DAMAGE_TICKS.putIfAbsent(targetId, target.getMaximumNoDamageTicks());
        ensureInvulnerabilityBypassTicker(plugin);
    }

    private static void ensureInvulnerabilityBypassTicker(Plugin plugin) {
        if (plugin == null) {
            return;
        }
        if (invulnerabilityBypassTickerTask != null && !invulnerabilityBypassTickerTask.isCancelled()) {
            return;
        }
        invulnerabilityBypassTickerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (INVULNERABILITY_BYPASS_REMAINING.isEmpty()) {
                return;
            }
            Iterator<Map.Entry<UUID, Integer>> iterator = INVULNERABILITY_BYPASS_REMAINING.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, Integer> entry = iterator.next();
                var entity = Bukkit.getEntity(entry.getKey());
                if (!(entity instanceof LivingEntity living) || !living.isValid() || living.isDead()) {
                    ORIGINAL_MAX_NO_DAMAGE_TICKS.remove(entry.getKey());
                    iterator.remove();
                    continue;
                }
                living.setMaximumNoDamageTicks(0);
                living.setNoDamageTicks(0);
                living.setLastDamage(0.0);

                int remaining = entry.getValue() - 1;
                if (remaining <= 0) {
                    int originalMax = ORIGINAL_MAX_NO_DAMAGE_TICKS.getOrDefault(entry.getKey(), living.getMaximumNoDamageTicks());
                    living.setMaximumNoDamageTicks(originalMax);
                    ORIGINAL_MAX_NO_DAMAGE_TICKS.remove(entry.getKey());
                    iterator.remove();
                } else {
                    entry.setValue(remaining);
                }
            }
        }, 0L, 1L);
    }

    public static void withTemporaryInvulnerabilityBypass(LivingEntity target, Runnable action) {
        if (target == null || action == null) {
            return;
        }

        int originalMaxNoDamageTicks = target.getMaximumNoDamageTicks();
        target.setMaximumNoDamageTicks(0);
        target.setNoDamageTicks(0);
        target.setLastDamage(0.0);
        try {
            action.run();
        } finally {
            target.setLastDamage(0.0);
            target.setNoDamageTicks(0);
            target.setMaximumNoDamageTicks(originalMaxNoDamageTicks);
        }
    }

    public static Location moveTemporaryLight(Location currentLight, Location target, int lightLevel) {
        Location targetLight = normalizeToBlock(target);
        if (targetLight == null) {
            clearTemporaryLight(currentLight);
            return null;
        }

        Location existing = normalizeToBlock(currentLight);
        if (existing != null
                && existing.getWorld() != null
                && targetLight.getWorld() != null
                && existing.getWorld().equals(targetLight.getWorld())
                && existing.getBlockX() == targetLight.getBlockX()
                && existing.getBlockY() == targetLight.getBlockY()
                && existing.getBlockZ() == targetLight.getBlockZ()) {
            return existing;
        }

        clearTemporaryLight(existing);
        return placeTemporaryLight(targetLight, lightLevel) ? targetLight : null;
    }

    public static void clearTemporaryLight(Location location) {
        Location normalized = normalizeToBlock(location);
        if (normalized == null || normalized.getWorld() == null) {
            return;
        }
        Material lightMaterial = Material.matchMaterial("LIGHT");
        if (lightMaterial == null) {
            return;
        }

        Block block = normalized.getWorld().getBlockAt(normalized);
        if (block.getType() == lightMaterial) {
            block.setType(Material.AIR, false);
        }
    }

    private static boolean placeTemporaryLight(Location location, int lightLevel) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        Material lightMaterial = Material.matchMaterial("LIGHT");
        if (lightMaterial == null) {
            return false;
        }

        Block block = location.getWorld().getBlockAt(location);
        if (!block.isEmpty()) {
            return false;
        }

        BlockData blockData = lightMaterial.createBlockData();
        if (blockData instanceof Light light) {
            light.setLevel(Math.max(1, Math.min(15, lightLevel)));
            blockData = light;
        }
        block.setBlockData(blockData, false);
        return true;
    }

    private static Location normalizeToBlock(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        return new Location(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public static void spawnFireProjectileTrail(Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        World world = location.getWorld();
        world.spawnParticle(Particle.FLAME, location, 3, 0.05, 0.05, 0.05, 0.008);
        world.spawnParticle(Particle.SMOKE, location, 1, 0.03, 0.03, 0.03, 0.002);
    }

    public static void spawnFireImpactEffect(Location impact) {
        if (impact == null || impact.getWorld() == null) {
            return;
        }
        World world = impact.getWorld();
        world.spawnParticle(Particle.FLAME, impact, 12, 0.28, 0.18, 0.28, 0.03);
        world.spawnParticle(Particle.SMOKE, impact, 6, 0.2, 0.1, 0.2, 0.008);
    }

    public static BukkitTask startDamageOverTime(JavaPlugin plugin,
                                                 Player source,
                                                 Location center,
                                                 double radius,
                                                 double damage,
                                                 int periodTicks,
                                                 int totalTicks) {
        if (plugin == null || source == null || center == null) {
            return null;
        }
        int safePeriod = Math.max(1, periodTicks);
        int safeTotal = Math.max(safePeriod, totalTicks);
        return new BukkitRunnable() {
            private int elapsed = 0;

            @Override
            public void run() {
                if (!source.isOnline()) {
                    cancel();
                    return;
                }
                applyAreaDamage(source, center, radius, damage);
                elapsed += safePeriod;
                if (elapsed >= safeTotal) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, safePeriod);
    }

    public static void spawnRingParticles(Location center, double radius, Particle particle, int points, double yOffset) {
        if (center == null || center.getWorld() == null || radius <= 0.0 || points <= 0) {
            return;
        }
        World world = center.getWorld();
        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0 * i) / points;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            world.spawnParticle(particle, center.clone().add(x, yOffset, z), 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    public static void spawnRosePatternParticles(Location center,
                                                 double radius,
                                                 int petals,
                                                 int points,
                                                 Particle particle,
                                                 double yOffset) {
        if (center == null || center.getWorld() == null || radius <= 0.0 || petals <= 0 || points <= 0) {
            return;
        }
        World world = center.getWorld();
        for (int i = 0; i < points; i++) {
            double t = (Math.PI * 2.0 * i) / points;
            double roseRadius = radius * Math.cos(petals * t);
            double x = roseRadius * Math.cos(t);
            double z = roseRadius * Math.sin(t);
            world.spawnParticle(particle, center.clone().add(x, yOffset, z), 1, 0.0, 0.0, 0.0, 0.0);
        }
    }
}
