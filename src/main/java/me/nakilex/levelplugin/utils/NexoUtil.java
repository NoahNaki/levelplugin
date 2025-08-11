package me.nakilex.levelplugin.utils;

import com.nexomc.nexo.api.NexoFurniture;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/** Utility helpers for interacting with the Nexo furniture API. */
public final class NexoUtil {
    private NexoUtil() {}

    /**
     * Logs all furniture IDs currently registered by the Nexo plugin. This is
     * useful for debugging when a specific furniture ID cannot be found.
     */
    public static void logAvailableFurnitureIds(Logger logger) {
        try {
            for (java.lang.reflect.Field field : NexoFurniture.class.getDeclaredFields()) {
                Class<?> type = field.getType();
                if (!Map.class.isAssignableFrom(type) && !Set.class.isAssignableFrom(type)) {
                    continue;
                }
                field.setAccessible(true);
                Object value = field.get(null);
                Collection<?> keys;
                if (value instanceof Map<?,?> map) {
                    keys = map.keySet();
                } else if (value instanceof Set<?> set) {
                    keys = set;
                } else {
                    continue;
                }
                logger.info("[NexoUtil] Available IDs from field '" + field.getName() + "': " + keys);
            }
        } catch (Exception ex) {
            logger.warning("[NexoUtil] Failed to list furniture IDs: " + ex.getMessage());
        }
    }
}
