package me.nakilex.levelplugin.utils;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

/**
 * Shared combat-target helpers for marking entities that should ignore player spell/attack damage.
 */
public final class CombatTargetUtil {
    public static final String DAMAGE_IMMUNE_TAG = "lp_damage_immune";
    public static final String DAMAGE_IMMUNE_META = "lp_damage_immune";

    private CombatTargetUtil() {
    }

    public static void markDamageImmune(Entity entity, Plugin plugin) {
        if (entity == null || plugin == null) {
            return;
        }
        entity.addScoreboardTag(DAMAGE_IMMUNE_TAG);
        entity.setMetadata(DAMAGE_IMMUNE_META, new FixedMetadataValue(plugin, true));
    }

    public static boolean isDamageImmune(Entity entity) {
        if (entity == null) {
            return false;
        }
        return entity.getScoreboardTags().contains(DAMAGE_IMMUNE_TAG)
                || entity.hasMetadata(DAMAGE_IMMUNE_META);
    }

    public static boolean isSpellValidTarget(LivingEntity entity) {
        return entity != null && !entity.isDead() && !isDamageImmune(entity);
    }

    public static boolean isPlayerSourced(Entity damager) {
        if (damager == null) {
            return false;
        }
        if (damager instanceof Player) {
            return true;
        }
        if (damager instanceof Projectile projectile) {
            return projectile.getShooter() instanceof Player;
        }
        return false;
    }
}
