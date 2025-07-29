package me.nakilex.levelplugin.mob.utils;

import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

/** Utility for calculating a MythicMob's combat power based on common attributes. */
public final class CombatPowerUtil {
    private CombatPowerUtil() {}

    /**
     * Calculate a simple combat power rating for the given MythicMob.
     * The formula currently sums max health and weighted attributes.
     *
     * @param mob ActiveMob instance
     * @return combat power value
     */
    public static int getCombatPower(ActiveMob mob) {
        if (mob == null || mob.getEntity() == null) return 0;
        LivingEntity ent = (LivingEntity) mob.getEntity().getBukkitEntity();
        double hp = ent.getAttribute(resolve("GENERIC_MAX_HEALTH", "MAX_HEALTH")) != null
                ? ent.getAttribute(resolve("GENERIC_MAX_HEALTH", "MAX_HEALTH")).getBaseValue()
                : ent.getMaxHealth();
        double dmg = ent.getAttribute(resolve("GENERIC_ATTACK_DAMAGE", "ATTACK_DAMAGE")) != null
                ? ent.getAttribute(resolve("GENERIC_ATTACK_DAMAGE", "ATTACK_DAMAGE")).getBaseValue()
                : 0;
        double move = ent.getAttribute(resolve("GENERIC_MOVEMENT_SPEED", "MOVEMENT_SPEED")) != null
                ? ent.getAttribute(resolve("GENERIC_MOVEMENT_SPEED", "MOVEMENT_SPEED")).getBaseValue()
                : 0;
        double atk = ent.getAttribute(resolve("GENERIC_ATTACK_SPEED", "ATTACK_SPEED")) != null
                ? ent.getAttribute(resolve("GENERIC_ATTACK_SPEED", "ATTACK_SPEED")).getBaseValue()
                : 0;
        double level = mob.getLevel();
        double power = hp + dmg * 10 + move * 100 + atk * 20 + level * 5;
        return (int) Math.round(power);
    }

    private static Attribute resolve(String generic, String fallback) {
        try {
            return Attribute.valueOf(generic);
        } catch (IllegalArgumentException ex) {
            try {
                return Attribute.valueOf(fallback);
            } catch (IllegalArgumentException ignore) {
                return null;
            }
        }
    }
}
