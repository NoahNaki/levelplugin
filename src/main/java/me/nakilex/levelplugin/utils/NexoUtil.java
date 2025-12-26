package me.nakilex.levelplugin.utils;

import com.nexomc.nexo.api.NexoFurniture;

import java.util.Collection;
import java.util.HashSet;
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
        Set<String> ids = getRegisteredIds(NexoFurniture.class);
        if (ids.isEmpty()) {
            logger.warning("[NexoUtil] No furniture IDs detected in NexoFurniture registry.");
            return;
        }
        logger.info("[NexoUtil] Available furniture IDs: " + ids);
    }

    /**
     * Returns a set of all furniture IDs currently registered by Nexo. Falls
     * back to an empty set if the registry cannot be introspected.
     */
    public static Set<String> getRegisteredFurnitureIds() {
        return getRegisteredIds(NexoFurniture.class);
    }

    private static Set<String> getRegisteredIds(Class<?> registryClass) {
        Set<String> ids = new HashSet<>();
        try {
            for (java.lang.reflect.Field field : registryClass.getDeclaredFields()) {
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
                for (Object key : keys) {
                    if (key != null) {
                        ids.add(key.toString());
                    }
                }
            }
        } catch (Exception ex) {
            return ids;
        }
        return ids;
    }
}
