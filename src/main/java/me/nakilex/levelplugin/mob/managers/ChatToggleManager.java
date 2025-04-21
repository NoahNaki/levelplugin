package me.nakilex.levelplugin.mob.managers;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which players have damage‐chat toggled ON.
 */
public class ChatToggleManager {
    private static final ChatToggleManager instance = new ChatToggleManager();
    public static ChatToggleManager getInstance() { return instance; }

    private final Map<UUID, Boolean> toggles = new ConcurrentHashMap<>();

    /** True if chat output is ON for this player. */
    public boolean isEnabled(Player p) {
        return toggles.getOrDefault(p.getUniqueId(), false);
    }

    /** Flip the state and return the new value. */
    public boolean toggle(Player p) {
        UUID u = p.getUniqueId();
        boolean now = !toggles.getOrDefault(u, false);
        toggles.put(u, now);
        return now;
    }
}
