package me.nakilex.levelplugin.doublejump.listeners;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;

public class DoubleJumpListener implements Listener {

    // Hardcoded jump velocity
    private final double jumpVelocity = 0.5;  // Adjust as needed

    public DoubleJumpListener() {
        // no-op constructor
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Allow flight so player can double jump
        player.setAllowFlight(true);
    }

    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();

        // Skip if in Creative mode (normal flight allowed)
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        // Only proceed if the player isn't already flying
        if (!player.isFlying()) {
            // Cancel the default flight toggle
            event.setCancelled(true);
            // Disable flight until they land
            player.setAllowFlight(false);

            // Apply upward velocity
            player.setVelocity(player.getLocation().getDirection().setY(jumpVelocity));

            // Particles
            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 30, 0.5, 0.1, 0.5, 0.1);

            // Sound effect
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1.0f, 1.0f);
        }
    }

    @EventHandler
    public void onPlayerLand(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        // If the player is on the ground, re-enable flight so they can double jump again
        if (((Entity) player).isOnGround()) {
            player.setAllowFlight(true);
        }
    }
}
