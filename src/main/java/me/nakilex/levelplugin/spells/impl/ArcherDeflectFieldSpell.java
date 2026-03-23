package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellEffectUtil;
import me.nakilex.levelplugin.spells.SpellHandler;
import me.nakilex.levelplugin.utils.PotionEffectUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class ArcherDeflectFieldSpell implements SpellHandler {
    private final Main plugin;
    private final int durationTicks;
    private final double fieldRadius;

    public ArcherDeflectFieldSpell(Main plugin,
                                   int durationTicks,
                                   double fieldRadius) {
        this.plugin = plugin;
        this.durationTicks = Math.max(20, durationTicks);
        this.fieldRadius = Math.max(1.0, fieldRadius);
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        PotionEffectUtil.applyHiddenEffect(caster, PotionEffectType.RESISTANCE, durationTicks, 0);
        caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_SHIELD_BLOCK, 0.9f, 1.4f);

        new BukkitRunnable() {
            private int elapsed;

            @Override
            public void run() {
                if (!caster.isOnline() || elapsed >= durationTicks) {
                    cancel();
                    return;
                }
                Location center = caster.getLocation().clone().add(0.0, 1.0, 0.0);
                center.getWorld().spawnParticle(Particle.CRIT, center, 10, fieldRadius * 0.25, 0.22, fieldRadius * 0.25, 0.02);
                center.getWorld().spawnParticle(Particle.CLOUD, center, 8, fieldRadius * 0.20, 0.18, fieldRadius * 0.20, 0.01);

                for (Entity entity : center.getWorld().getNearbyEntities(center, fieldRadius, fieldRadius, fieldRadius)) {
                    if (entity instanceof Projectile projectile) {
                        if (projectile.getShooter() instanceof Player shooter && shooter.getUniqueId().equals(caster.getUniqueId())) {
                            continue;
                        }
                        Vector away = projectile.getLocation().toVector().subtract(caster.getLocation().toVector());
                        if (away.lengthSquared() <= 0.000001) {
                            away = caster.getLocation().getDirection().clone().multiply(-1.0);
                        }
                        projectile.setVelocity(away.normalize().multiply(1.1).setY(0.12));
                        continue;
                    }
                    if (entity instanceof LivingEntity living && !living.equals(caster)) {
                        SpellEffectUtil.applyStun(living, 6, false);
                    }
                }
                elapsed += 4;
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }
}
