package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellEffectUtil;
import me.nakilex.levelplugin.spells.SpellHandler;
import me.nakilex.levelplugin.utils.PotionEffectUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ArcherSkyboundSpell implements SpellHandler {
    private static final Map<UUID, SlamWindow> ACTIVE_SLAM_WINDOWS = new ConcurrentHashMap<>();

    private final Main plugin;
    private final double liftVelocity;
    private final int slowFallingTicks;
    private final double slamRadius;
    private final double slamDamage;

    public ArcherSkyboundSpell(Main plugin,
                               double liftVelocity,
                               int slowFallingTicks,
                               double slamRadius,
                               double slamDamage) {
        this.plugin = plugin;
        this.liftVelocity = Math.max(0.45, liftVelocity);
        this.slowFallingTicks = Math.max(20, slowFallingTicks);
        this.slamRadius = Math.max(1.0, slamRadius);
        this.slamDamage = Math.max(0.1, slamDamage);
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        caster.setVelocity(caster.getVelocity().add(new Vector(0.0, liftVelocity, 0.0)));
        PotionEffectUtil.applyHiddenEffect(caster, PotionEffectType.SLOW_FALLING, slowFallingTicks, 0);
        ACTIVE_SLAM_WINDOWS.put(caster.getUniqueId(),
                new SlamWindow(System.currentTimeMillis() + (slowFallingTicks * 50L), slamRadius, slamDamage));

        Location center = caster.getLocation().clone().add(0.0, 1.0, 0.0);
        center.getWorld().spawnParticle(Particle.CLOUD, center, 28, 0.35, 0.18, 0.35, 0.02);
        center.getWorld().spawnParticle(Particle.CRIT, center, 16, 0.25, 0.12, 0.25, 0.02);
        center.getWorld().playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 1.5f);
    }

    public static boolean tryTriggerAerialBurst(Main plugin, Player player) {
        if (plugin == null || player == null || !player.isOnline()) {
            return false;
        }
        SlamWindow window = ACTIVE_SLAM_WINDOWS.get(player.getUniqueId());
        if (window == null || System.currentTimeMillis() > window.expiresAt()) {
            ACTIVE_SLAM_WINDOWS.remove(player.getUniqueId());
            return false;
        }
        if (player.isOnGround()) {
            return false;
        }

        ACTIVE_SLAM_WINDOWS.remove(player.getUniqueId());
        PotionEffectUtil.removeEffect(player, PotionEffectType.SLOW_FALLING);
        player.setVelocity(new Vector(0.0, -1.75, 0.0));

        new BukkitRunnable() {
            private int ticks;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                player.setFallDistance(0.0f);
                if (player.isOnGround() || ticks++ >= 24) {
                    Location impact = player.getLocation().clone().add(0.0, 0.1, 0.0);
                    SpellEffectUtil.applyAreaDamage(player, impact, window.radius(), window.damage());
                    impact.getWorld().spawnParticle(Particle.EXPLOSION, impact, 1, 0.0, 0.0, 0.0, 0.0);
                    impact.getWorld().spawnParticle(Particle.CLOUD, impact, 28, 0.5, 0.12, 0.5, 0.01);
                    impact.getWorld().spawnParticle(Particle.CRIT, impact, 18, 0.45, 0.15, 0.45, 0.04);
                    impact.getWorld().playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.35f);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
        return true;
    }

    private record SlamWindow(long expiresAt, double radius, double damage) {
    }
}
