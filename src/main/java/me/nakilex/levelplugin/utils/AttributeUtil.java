package me.nakilex.levelplugin.utils;

import org.bukkit.attribute.Attribute;

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
}
