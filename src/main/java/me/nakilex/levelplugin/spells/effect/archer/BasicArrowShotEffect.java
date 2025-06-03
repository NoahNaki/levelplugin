// src/main/java/me/nakilex/levelplugin/spells/effect/archer/BasicArrowShotEffect.java
package me.nakilex.levelplugin.spells.effect.archer;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.UUID;

/**
 * Fires one or more basic arrows out of the caster’s bow.
 * Supports an "extraProjectiles" rune param to add additional arrows.
 */
public class BasicArrowShotEffect implements SpellEffect {
    private static final String META_KEY = "BasicAttack";

    private boolean parseBoolean(Object obj, boolean def) {
        if (obj instanceof Boolean b) return b;
        if (obj instanceof String s) return Boolean.parseBoolean(s);
        return def;
    }

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

    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        UUID pid = player.getUniqueId();

        // Debug: list all raw extraProjectiles entries from context
        Object rawExtra = ctx.getExtraParam("extraProjectiles");
        if (rawExtra == null) {
            Bukkit.getLogger().info("[DBG] BasicArrowShotEffect -> rawExtra is null");
        } else {
            Bukkit.getLogger().info("[DBG] BasicArrowShotEffect -> rawExtra class=" + rawExtra.getClass().getSimpleName() + ", value=" + rawExtra);
        }

        // Determine number of arrows: base 1 + extraProjectiles from runes
        int extra = 0;
        if (rawExtra instanceof Number) {
            extra = ((Number) rawExtra).intValue();
        } else if (rawExtra instanceof List<?>) {
            List<?> list = (List<?>) rawExtra;
            int sum = 0;
            for (Object n : list) {
                if (n instanceof Number) {
                    int val = ((Number) n).intValue();
                    sum += val;
                    Bukkit.getLogger().info("[DBG] BasicArrowShotEffect -> list entry=" + val);
                }
            }
            extra = sum;
        }
        // Debug: log total extra projectiles
        Bukkit.getLogger().info("[DBG] BasicArrowShotEffect -> total extraProjectiles=" + extra);
        int totalArrows = 1 + extra;
        Bukkit.getLogger().info("[DBG] BasicArrowShotEffect -> totalArrows=" + totalArrows);

        boolean instantShot = parseBoolean(ctx.getExtraParam("instantShot"), false);
        int pierceLevel     = parseInt(ctx.getExtraParam("pierceLevel"), 0);

        // Calculate damage per arrow: weapon base + STR modifier
        double baseAtk = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue();
        int str = me.nakilex.levelplugin.player.attributes.managers.StatsManager
            .getInstance()
            .getStatValue(player, me.nakilex.levelplugin.player.attributes.managers.StatsManager.StatType.STR);
        double damage = baseAtk + (str * 0.5);

        Runnable shootArrows = () -> {
            for (int i = 0; i < totalArrows; i++) {
                Vector dir = player.getLocation().getDirection().clone();
                if (i > 0) {
                    double spread = 0.1;
                    dir.add(new Vector(
                        (Math.random() - 0.5) * spread,
                        (Math.random() - 0.5) * spread,
                        (Math.random() - 0.5) * spread
                    ));
                }
                Arrow arrow = player.launchProjectile(Arrow.class, dir.multiply(2));
                arrow.setDamage(damage);
                arrow.setCustomName("BasicArcherArrow");
                arrow.setCustomNameVisible(false);
                arrow.setPierceLevel(Math.max(0, pierceLevel));
                arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                arrow.setMetadata(META_KEY, new FixedMetadataValue(Main.getInstance(), pid));
            }

            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1f, 1f);
            player.getWorld().spawnParticle(Particle.INSTANT_EFFECT, player.getLocation(), 20, 0.5, 1, 0.5);
        };

        if (instantShot) {
            shootArrows.run();
        } else {
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_CROSSBOW_LOADING_START, 1f, 1f);
            new BukkitRunnable() {
                @Override public void run() { shootArrows.run(); }
            }.runTaskLater(Main.getInstance(), 10L);
        }
    }
}
