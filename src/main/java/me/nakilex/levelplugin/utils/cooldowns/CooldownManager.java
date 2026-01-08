package me.nakilex.levelplugin.utils.cooldowns;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic cooldown tracker keyed by player and action id.
 */
public class CooldownManager {
    private static CooldownManager instance;
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    private CooldownManager() {}

    public static synchronized CooldownManager getInstance() {
        if (instance == null) {
            instance = new CooldownManager();
        }
        return instance;
    }

    /**
     * @return true if the given action is still on cooldown for the player.
     */
    public boolean isOnCooldown(UUID playerId, String actionId) {
        Map<String, Long> playerMap = cooldowns.get(playerId);
        if (playerMap == null) return false;
        Long expires = playerMap.get(actionId);
        return expires != null && expires > System.currentTimeMillis();
    }

    /**
     * @return milliseconds remaining on cooldown, or 0 if none.
     */
    public long getRemainingTime(UUID playerId, String actionId) {
        Map<String, Long> playerMap = cooldowns.get(playerId);
        if (playerMap == null) return 0;
        Long expires = playerMap.get(actionId);
        if (expires == null) return 0;
        long rem = expires - System.currentTimeMillis();
        return Math.max(0, rem);
    }

    /**
     * Sets a cooldown for the given action and player.
     * @param seconds duration in seconds
     */
    public void setCooldown(UUID playerId, String actionId, double seconds) {
        long expiresAt = System.currentTimeMillis() + (long)(seconds * 1000L);
        cooldowns.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
            .put(actionId, expiresAt);
    }
}
