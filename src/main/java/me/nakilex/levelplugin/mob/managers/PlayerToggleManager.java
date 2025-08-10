package me.nakilex.levelplugin.mob.managers;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic per‑player toggle manager. Each instance represents a single
 * feature toggle, mapping player UUIDs to whether that feature is enabled.
 */
public class PlayerToggleManager {

    // Thread‑safe map of player UUID → enabled flag
    private final Map<UUID, Boolean> toggles = new ConcurrentHashMap<>();

    /**
     * Check whether this feature is enabled for the player.
     *
     * @param player the player to check
     * @return true if they’ve toggled it ON (default false)
     */
    public boolean isEnabled(Player player) {
        return toggles.getOrDefault(player.getUniqueId(), false);
    }

    /**
     * Sets the enabled state.
     *
     * @param player  the player
     * @param enabled true to turn ON, false to turn OFF
     */
    public void setEnabled(Player player, boolean enabled) {
        toggles.put(player.getUniqueId(), enabled);
    }

    /**
     * Flip the current state.
     *
     * @param player the player
     * @return the new state (true = now enabled)
     */
    public boolean toggle(Player player) {
        UUID uuid = player.getUniqueId();
        boolean now = !toggles.getOrDefault(uuid, false);
        toggles.put(uuid, now);
        return now;
    }

    /**
     * Clears any stored state for a player (e.g. on logout, if desired).
     */
    public void clear(Player player) {
        toggles.remove(player.getUniqueId());
    }
}
