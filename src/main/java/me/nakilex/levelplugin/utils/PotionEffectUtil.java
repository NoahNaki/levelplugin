package me.nakilex.levelplugin.utils;

import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;

/** Utility methods for handling potion effects. */
public final class PotionEffectUtil {
    private PotionEffectUtil() {}

    /** Remove all active potion effects from the given entity. */
    public static void clearAllEffects(LivingEntity entity) {
        for (PotionEffect effect : entity.getActivePotionEffects()) {
            entity.removePotionEffect(effect.getType());
        }
    }
}
