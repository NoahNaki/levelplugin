package me.nakilex.levelplugin.player.classes.listeners;

import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ArcherDoubleJumpListener implements Listener {

    // Store each player's last horizontal movement vector.
    private final Map<UUID, Vector> lastMoveDirection = new HashMap<>();

    /**
     * When a player toggles flight (by double-tapping jump), check if they are an Archer and, if so,
     * apply a custom velocity based on their walking direction.
     */
    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();

        // Only process players in survival or adventure mode.
        if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) {
            return;
        }

        // Only continue if the player is an Archer.
//        if (!isArcher(player)) {
//            return;
//        }

        // This event should only be fired in mid-air.
        if (player.isOnGround()) {
            return;
        }

        // Cancel the default flight behavior.
        event.setCancelled(true);
        // Prevent additional jumps until the player lands.
        player.setAllowFlight(false);

        // Retrieve the player's last recorded horizontal movement.
        Vector movementDirection = lastMoveDirection.get(player.getUniqueId());
        Vector doubleJumpVelocity;

        if (movementDirection != null && movementDirection.lengthSquared() > 0.001) {
            // Use the recorded movement direction.
            doubleJumpVelocity = movementDirection.clone().normalize().multiply(0.5); // Adjust horizontal boost.
        } else {
            // Fallback: use the player's view direction if no movement is recorded.
            doubleJumpVelocity = player.getLocation().getDirection().setY(0).normalize().multiply(0.5);
        }

        // Set the vertical boost for the double jump.
        doubleJumpVelocity.setY(0.7); // Adjust vertical boost as needed.

        // Apply the custom velocity.
        player.setVelocity(player.getVelocity().add(doubleJumpVelocity));

        // (Optional) Remove the stored direction so that it doesn't interfere with future jumps.
        lastMoveDirection.remove(player.getUniqueId());

        // (Optional) You can add sound effects or particle effects here.
    }

    /**
     * Listens for player movement to both:
     *   1. Record the last horizontal movement direction.
     *   2. Re-enable flight (and thus double-jumping) once the player lands.
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Only process if the player is an Archer.
//        if (!isArcher(player)) {
//            return;
//        }

        // Calculate horizontal movement.
        double dx = event.getTo().getX() - event.getFrom().getX();
        double dz = event.getTo().getZ() - event.getFrom().getZ();
        if (dx != 0 || dz != 0) {
            Vector moveDir = new Vector(dx, 0, dz);
            if (moveDir.lengthSquared() > 0.001) {  // Only store if movement is significant.
                lastMoveDirection.put(player.getUniqueId(), moveDir);
            }
        }

        // When the player lands, re-enable flight to allow for the next double jump.
        if (player.isOnGround() && !player.getAllowFlight()) {
            player.setAllowFlight(true);
        }
    }

    /**
     * Determines whether the given player is an Archer.
     * Replace this with your actual logic (for example, checking a player manager or a stored property).
     */
//    private boolean isArcher(Player player) {
//        // Example: using a permission.
//        return player.hasPermission("class.archer");
//
//        // Alternatively, if you have a player manager:
//        // return YourPlayerManager.getPlayerClass(player) == PlayerClass.ARCHER;
//    }
}
