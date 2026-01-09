package me.nakilex.levelplugin.hud.placeholders;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HudPlaceholderCache {
    private final Map<UUID, Map<String, CacheEntry>> cache = new ConcurrentHashMap<>();
    private volatile long ttlMs;

    public HudPlaceholderCache(long ttlMs) {
        this.ttlMs = ttlMs;
    }

    public void setTtlMs(long ttlMs) {
        this.ttlMs = ttlMs;
    }

    public String get(Player player, String key) {
        if (player == null || key == null) {
            return null;
        }
        Map<String, CacheEntry> playerCache = cache.get(player.getUniqueId());
        if (playerCache == null) {
            return null;
        }
        CacheEntry entry = playerCache.get(key);
        if (entry == null || entry.isExpired()) {
            if (entry != null) {
                playerCache.remove(key);
            }
            return null;
        }
        return entry.value();
    }

    public void put(Player player, String key, String value) {
        if (player == null || key == null) {
            return;
        }
        long expiresAt = ttlMs <= 0 ? Long.MAX_VALUE : System.currentTimeMillis() + ttlMs;
        cache.computeIfAbsent(player.getUniqueId(), id -> new ConcurrentHashMap<>())
                .put(key, new CacheEntry(value, expiresAt));
    }

    public void clear(Player player) {
        if (player == null) {
            return;
        }
        cache.remove(player.getUniqueId());
    }

    public void clearAll() {
        cache.clear();
    }

    private record CacheEntry(String value, long expiresAt) {
        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}
