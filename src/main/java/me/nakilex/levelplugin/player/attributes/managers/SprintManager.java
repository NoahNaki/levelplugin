package me.nakilex.levelplugin.player.attributes.managers;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.event.EventPriority;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks player sprint toggle state so sprinting can continue below vanilla
 * hunger thresholds. Any server attempt to stop sprinting is ignored until the
 * player's food level actually reaches zero.
 */
public class SprintManager implements Listener {
    private static final SprintManager instance = new SprintManager();
    public static SprintManager getInstance() { return instance; }

    // Tracks whether the player currently wants to keep sprinting
    private final Map<UUID, Boolean> sprinting = new ConcurrentHashMap<>();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onToggle(PlayerToggleSprintEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();

        if (event.isSprinting()) {
            // Player pressed sprint
            sprinting.put(id, true);
            return;
        }

        if (sprinting.getOrDefault(id, false) && player.getFoodLevel() > 0) {
            // Server is trying to stop sprinting due to low hunger
            event.setCancelled(true);
            player.setSprinting(true);
            return;
        }

        sprinting.put(id, false);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();

        if (!sprinting.getOrDefault(id, false)) return;

        double dx = event.getTo().getX() - event.getFrom().getX();
        double dz = event.getTo().getZ() - event.getFrom().getZ();
        double distSq = dx * dx + dz * dz;

        // If player barely moved or is only walking, stop sprinting
        if (distSq < 0.04) {
            sprinting.put(id, false);
            player.setSprinting(false);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        sprinting.remove(id);
    }

    public boolean wantsSprint(Player player) {
        return sprinting.getOrDefault(player.getUniqueId(), false);
    }

    public void setWantsSprint(Player player, boolean value) {
        sprinting.put(player.getUniqueId(), value);
    }
}
