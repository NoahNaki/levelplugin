package me.nakilex.levelplugin.utils;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;

/** Utility helpers for resolving Bukkit attributes across versions. */
public final class AttributeUtil {
    private AttributeUtil() {}

    /**
     * Resolve the first available {@link Attribute} by trying each provided
     * name in order. This helps bridge name differences across Minecraft
     * versions (e.g., GENERIC_MAX_HEALTH vs MAX_HEALTH).
     *
     * @param names attribute enum names to try in priority order
     * @return first matching attribute or {@code null} if none could be resolved
     */
    public static Attribute resolve(String... names) {
        if (names == null) return null;
        for (String name : names) {
            if (name == null) continue;
            try {
                return Attribute.valueOf(name);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return null;
    }

    /**
     * Set an entity's max health and immediately heal it to that max value.
     * This should be called after entity-specific mutations such as slime size changes
     * because those mutations may reset vanilla health attributes.
     */
    public static void setMaxHealthAndHeal(LivingEntity entity, double maxHealth) {
        if (entity == null) {
            return;
        }
        double safeHealth = Math.max(1.0D, maxHealth);
        Attribute maxHealthAttr = resolve("GENERIC_MAX_HEALTH", "MAX_HEALTH");
        AttributeInstance attribute = maxHealthAttr == null ? null : entity.getAttribute(maxHealthAttr);
        if (attribute != null) {
            attribute.setBaseValue(safeHealth);
            entity.setHealth(Math.min(safeHealth, attribute.getValue()));
            return;
        }
        entity.setHealth(Math.min(safeHealth, entity.getHealth()));
    }
}
