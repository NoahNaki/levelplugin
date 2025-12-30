package me.nakilex.levelplugin.utils;

import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Utility methods for handling potion effects. */
public final class PotionEffectUtil {
    private PotionEffectUtil() {}

    /** Remove all active potion effects from the given entity. */
    public static void clearAllEffects(LivingEntity entity) {
        for (PotionEffect effect : entity.getActivePotionEffects()) {
            entity.removePotionEffect(effect.getType());
        }
    }

    /** Apply a hidden potion effect, replacing any existing one of the same type. */
    public static void applyHiddenEffect(LivingEntity entity, PotionEffectType type, int durationTicks, int amplifier) {
        if (durationTicks <= 0) {
            return;
        }
        PotionEffect effect = new PotionEffect(type, durationTicks, amplifier, false, false, false);
        entity.addPotionEffect(effect, true);
    }

    /** Remove a specific potion effect if present. */
    public static void removeEffect(LivingEntity entity, PotionEffectType type) {
        entity.removePotionEffect(type);
    }
}
