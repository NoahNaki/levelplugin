package me.nakilex.levelplugin.spells.effect.archer;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.UUID;

/**
 * Fires explosive arrows that detonate on impact.
 */
public class ExplosiveArrowShotEffect implements SpellEffect {
    private static final String META_KEY = "ExplosiveArrow";

    private int parseInt(Object obj, int def) {
        if (obj instanceof Number n) return n.intValue();
        if (obj instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {}
        }
        if (obj instanceof List<?> list) {
            int sum = 0;
            for (Object o : list) {
                if (o instanceof Number n) sum += n.intValue();
            }
            return sum;
        }
        return def;
    }

    private double parseDouble(Object obj, double def) {
        if (obj instanceof Number n) return n.doubleValue();
        if (obj instanceof String s) {
            try { return Double.parseDouble(s); } catch (NumberFormatException ignored) {}
        }
        return def;
    }

    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        UUID pid = player.getUniqueId();
        int extra = parseInt(ctx.getExtraParam("extraProjectiles"), 0);
        int total = 1 + extra;
        double speedMultiplier = parseDouble(ctx.getExtraParam("velocityMultiplier"), 1.0);
        double explosionPower = parseDouble(ctx.getExtraParam("explosionPower"), 2.0);

        double baseAtk = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue();
        int str = me.nakilex.levelplugin.player.attributes.managers.StatsManager
                .getInstance()
                .getStatValue(player, me.nakilex.levelplugin.player.attributes.managers.StatsManager.StatType.STR);
        double damage = baseAtk + (str * 0.5);

        for (int i = 0; i < total; i++) {
            Vector dir = player.getLocation().getDirection().clone();
            if (i > 0) {
                double spread = 0.1;
                dir.add(new Vector(
                        (Math.random() - 0.5) * spread,
                        (Math.random() - 0.5) * spread,
                        (Math.random() - 0.5) * spread
                ));
            }
            Arrow arrow = player.launchProjectile(Arrow.class, dir.multiply(2 * speedMultiplier));
            arrow.setDamage(damage);
            arrow.setCustomName("ExplosiveArrow");
            arrow.setCustomNameVisible(false);
            arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
            arrow.setMetadata(META_KEY, new FixedMetadataValue(Main.getInstance(), explosionPower));

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!arrow.isValid() || arrow.isDead()) { cancel(); return; }
                    if (arrow.isOnGround()) {
                        explode();
                        return;
                    }
                    for (Entity e : arrow.getNearbyEntities(0.25,0.25,0.25)) {
                        if (e instanceof LivingEntity le && !le.equals(player)) {
                            SpellUtils.dealWithChat(player, le, damage, "Explosive Arrow");
                            explode();
                            return;
                        }
                    }
                }
                private void explode() {
                    arrow.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, arrow.getLocation(), 1);
                    arrow.getWorld().playSound(arrow.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
                    arrow.getWorld().createExplosion(arrow.getLocation(), (float)explosionPower, false, false, player);
                    arrow.remove();
                    cancel();
                }
            }.runTaskTimer(Main.getInstance(), 0L, 1L);
        }
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1f, 1f);
        player.getWorld().spawnParticle(Particle.CRIT, player.getLocation(), 10, 0.5, 0.5, 0.5);
    }
}
