package me.nakilex.levelplugin.player.attributes.managers;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks the mana cost of the most recently cast spell so it can be briefly
 * displayed in the action bar.
 */
public class ManaIndicatorManager {
    private static final ManaIndicatorManager instance = new ManaIndicatorManager();
    public static ManaIndicatorManager getInstance() { return instance; }

    private static class Info {
        int cost;
        long expireAt;
    }

    private final Map<UUID, Info> indicators = new ConcurrentHashMap<>();
    private static final long DURATION_MS = 500; // 0.5 seconds

    /** Record the mana cost to show on the action bar. */
    public void showCost(Player player, int cost) {
        Info info = new Info();
        info.cost = cost;
        info.expireAt = System.currentTimeMillis() + DURATION_MS;
        indicators.put(player.getUniqueId(), info);
    }

    /** Return the active cost or null if expired/none. */
    public Integer getCost(Player player) {
        Info info = indicators.get(player.getUniqueId());
        if (info == null) return null;
        if (System.currentTimeMillis() > info.expireAt) {
            indicators.remove(player.getUniqueId());
            return null;
        }
        return info.cost;
    }

    /** Clear any active indicator for the player. */
    public void clear(Player player) {
        indicators.remove(player.getUniqueId());
    }
}
