package me.nakilex.levelplugin.player.attributes.managers;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks player sprint toggle state so sprinting can continue below vanilla
 * hunger thresholds. By ignoring the brief sprint toggle sent by the server
 * when food drops under 6, players may keep sprinting until reaching 0 hunger.
 */
public class SprintManager implements Listener {
    private static final SprintManager instance = new SprintManager();
    public static SprintManager getInstance() { return instance; }

    private final Map<UUID, Boolean> sprinting = new ConcurrentHashMap<>();
    // Time of last successful sprint toggle ON
    private final Map<UUID, Long> lastOnTime = new ConcurrentHashMap<>();

    @EventHandler
    public void onToggle(PlayerToggleSprintEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();

        if (event.isSprinting()) {
            // Player initiated sprint toggle ON
            sprinting.put(id, true);
            lastOnTime.put(id, System.currentTimeMillis());
            return;
        }

        // Toggle OFF - may be forced by vanilla when hunger drops below 6
        long last = lastOnTime.getOrDefault(id, 0L);
        if (player.getFoodLevel() > 0 && System.currentTimeMillis() - last < 200) {
            // Ignore vanilla stop and keep sprinting
            event.setCancelled(true);
            player.setSprinting(true);
            return;
        }

        sprinting.put(id, false);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        sprinting.remove(id);
        lastOnTime.remove(id);
    }

    public boolean wantsSprint(Player player) {
        return sprinting.getOrDefault(player.getUniqueId(), false);
    }

    public void setWantsSprint(Player player, boolean value) {
        sprinting.put(player.getUniqueId(), value);
    }
}
