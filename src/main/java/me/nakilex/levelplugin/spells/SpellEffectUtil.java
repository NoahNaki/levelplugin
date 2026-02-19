package me.nakilex.levelplugin.spells;

import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class SpellEffectUtil {
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

    public static double computeIntTechniqueDamage(Player source,
                                                   double baseDamage,
                                                   double intelligenceScale,
                                                   double techniqueScale) {
        if (source == null) {
            return baseDamage;
        }
        StatsManager.PlayerStats stats = StatsManager.getInstance().getPlayerStats(source.getUniqueId());
        int totalIntelligence = stats.baseIntelligence + stats.bonusIntelligence;
        int totalTechnique = stats.baseTechnique + stats.bonusTechnique;
        double damage = baseDamage + (totalIntelligence * intelligenceScale);
        return damage * (1.0 + (totalTechnique * techniqueScale));
    }
}
