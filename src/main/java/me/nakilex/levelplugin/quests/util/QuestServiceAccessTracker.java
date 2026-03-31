package me.nakilex.levelplugin.quests.util;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks short-lived cooldowns for NPC-driven service interactions so quests
 * can temporarily disable reopening menus after an action is performed.
 */
public final class QuestServiceAccessTracker {

    public enum Service {
        SALVAGE,
        AUCTION
    }

    /** Default cooldown in milliseconds after completing a service interaction. */
    public static final long DEFAULT_COOLDOWN_MS = 2_500L;

    private static final Map<Service, Map<UUID, Long>> COOLDOWNS = new EnumMap<>(Service.class);

    static {
        for (Service service : Service.values()) {
            COOLDOWNS.put(service, new java.util.HashMap<>());
        }
    }

    private QuestServiceAccessTracker() {}

    /**
     * Mark the player as cooling down for the given service.
     */
    public static void markInteraction(UUID playerId, Service service) {
        markInteraction(playerId, service, DEFAULT_COOLDOWN_MS);
    }

    /**
     * Mark the player as cooling down for the given service using a custom duration.
     */
    public static void markInteraction(UUID playerId, Service service, long durationMs) {
        if (playerId == null || service == null || durationMs <= 0) {
            return;
        }
        COOLDOWNS.get(service).put(playerId, System.currentTimeMillis() + durationMs);
    }

    /**
     * Check whether the player is still cooling down for a service.
     */
    public static boolean isCoolingDown(UUID playerId, Service service) {
        if (playerId == null || service == null) {
            return false;
        }
        Map<UUID, Long> map = COOLDOWNS.get(service);
        Long expires = map.get(playerId);
        if (expires == null) {
            return false;
        }
        if (expires < System.currentTimeMillis()) {
            map.remove(playerId);
            return false;
        }
        return true;
    }

    /**
     * Remaining cooldown in milliseconds for a service interaction.
     * Returns 0 when no cooldown is active.
     */
    public static long getRemainingMs(UUID playerId, Service service) {
        if (playerId == null || service == null) {
            return 0L;
        }
        Map<UUID, Long> map = COOLDOWNS.get(service);
        Long expires = map.get(playerId);
        if (expires == null) {
            return 0L;
        }
        long remaining = expires - System.currentTimeMillis();
        if (remaining <= 0L) {
            map.remove(playerId);
            return 0L;
        }
        return remaining;
    }
}
