package me.nakilex.levelplugin.player.attributes.managers;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks the cooldown of the most recently used spell so it can be
 * displayed in the action bar.
 */
public class CooldownIndicatorManager {
    private static final CooldownIndicatorManager instance = new CooldownIndicatorManager();
    public static CooldownIndicatorManager getInstance() { return instance; }

    public static class Info {
        public String name;
        public long expireAt;
    }

    private final Map<UUID, Info> indicators = new ConcurrentHashMap<>();

    /** Record a cooldown to show on the action bar. */
    public void show(Player player, String spellName, long durationMs) {
        Info info = new Info();
        info.name = spellName;
        info.expireAt = System.currentTimeMillis() + durationMs;
        indicators.put(player.getUniqueId(), info);
    }

    /** Return the active info or null if expired/none. */
    public Info get(Player player) {
        Info info = indicators.get(player.getUniqueId());
        if (info == null) return null;
        if (System.currentTimeMillis() > info.expireAt) {
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

