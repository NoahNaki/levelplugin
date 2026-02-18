package me.nakilex.levelplugin.spells;

import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class SpellEffectUtil {
    public static final String BYPASS_STAT_SCALING_META = "BypassStatScaling";

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
        if (plugin == null || caster == null || target == null || damage <= 0.0) {
            return;
        }
        caster.setMetadata(BYPASS_STAT_SCALING_META, new FixedMetadataValue(plugin, true));
        try {
            target.damage(damage, caster);
        } finally {
            caster.removeMetadata(BYPASS_STAT_SCALING_META, plugin);
        }
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
}
