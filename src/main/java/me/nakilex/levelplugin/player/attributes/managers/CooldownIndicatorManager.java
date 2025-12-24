package me.nakilex.levelplugin.player.attributes.managers;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks the cooldown and mana cost of the most recently used spell so it can be
 * displayed in the action bar.
 */
public class CooldownIndicatorManager {
    private static final CooldownIndicatorManager instance = new CooldownIndicatorManager();
    public static CooldownIndicatorManager getInstance() { return instance; }

    public static class Info {
        public String name;
        public long expireAt;
        public int cost;
        public long costExpireAt;
        public String actionBarMessage;
        public long actionBarExpireAt;
    }

    private static final long COST_DURATION_MS = 1500;
    private static final long ACTIONBAR_MIN_DURATION_MS = 200;

    private final Map<UUID, Info> indicators = new ConcurrentHashMap<>();

    /** Record a cooldown and mana cost to show on the action bar. */
    public void show(Player player, String spellName, long durationMs, int cost) {
        long now = System.currentTimeMillis();
        Info info = new Info();
        info.name = spellName;
        info.expireAt = now + durationMs;
        info.cost = cost;
        info.costExpireAt = now + COST_DURATION_MS;
        indicators.put(player.getUniqueId(), info);
    }

    /** Display a temporary action bar message without replacing spell cooldowns. */
    public void showActionBar(Player player, String message, long durationMs) {
        if (player == null || message == null) {
            return;
        }
        long now = System.currentTimeMillis();
        Info info = indicators.computeIfAbsent(player.getUniqueId(), id -> new Info());
        info.actionBarMessage = message;
        info.actionBarExpireAt = now + Math.max(ACTIONBAR_MIN_DURATION_MS, durationMs);
    }

    /** Return the active info or null if expired/none. */
    public Info get(Player player) {
        Info info = indicators.get(player.getUniqueId());
        if (info == null) return null;
        long now = System.currentTimeMillis();
        boolean cooldownExpired = now > info.expireAt && now > info.costExpireAt;
        boolean actionBarExpired = now > info.actionBarExpireAt;
        if (cooldownExpired && actionBarExpired) {
            indicators.remove(player.getUniqueId());
            return null;
        }
        return info;
    }

    /** Clear any active indicator for the player. */
    public void clear(Player player) {
        indicators.remove(player.getUniqueId());
    }
}
