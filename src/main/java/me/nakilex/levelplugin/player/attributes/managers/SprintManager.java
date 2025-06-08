package me.nakilex.levelplugin.player.attributes.managers;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks player sprint toggle state so sprinting can continue below vanilla hunger thresholds.
 */
public class SprintManager implements Listener {
    private static final SprintManager instance = new SprintManager();
    public static SprintManager getInstance() { return instance; }

    private final Map<UUID, Boolean> sprinting = new HashMap<>();

    @EventHandler
    public void onToggle(PlayerToggleSprintEvent event) {
        sprinting.put(event.getPlayer().getUniqueId(), event.isSprinting());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        sprinting.remove(event.getPlayer().getUniqueId());
    }

    public boolean wantsSprint(Player player) {
        return sprinting.getOrDefault(player.getUniqueId(), false);
    }

    public void setWantsSprint(Player player, boolean value) {
        sprinting.put(player.getUniqueId(), value);
    }
}
