package me.nakilex.levelplugin.spells.managers;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks spell cooldowns per player.
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
     * @return true if the given spell is still on cooldown for the player.
     */
    public boolean isOnCooldown(UUID playerId, String spellId) {
        Map<String, Long> playerMap = cooldowns.get(playerId);
        if (playerMap == null) return false;
        Long expires = playerMap.get(spellId);
        return expires != null && expires > System.currentTimeMillis();
    }

    /**
     * @return milliseconds remaining on cooldown, or 0 if none.
     */
    public long getRemainingTime(UUID playerId, String spellId) {
        Map<String, Long> playerMap = cooldowns.get(playerId);
        if (playerMap == null) return 0;
        Long expires = playerMap.get(spellId);
        if (expires == null) return 0;
        long rem = expires - System.currentTimeMillis();
        return Math.max(0, rem);
    }

    /**
     * Sets a cooldown for the given spell and player.
     * @param seconds duration in seconds
     */
    public void setCooldown(UUID playerId, String spellId, double seconds) {
        long expiresAt = System.currentTimeMillis() + (long)(seconds * 1000L);
        cooldowns.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
            .put(spellId, expiresAt);
    }
}
