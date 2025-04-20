package me.nakilex.levelplugin.doublejump.listeners;

import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;

public class DoubleJumpListener implements Listener {

    // Hardcoded jump velocity; adjust as needed
    private final double jumpVelocity = 0.5;


    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Retrieve the player's class via StatsManager (like in SpellGUI)
        PlayerClass playerClass = StatsManager.getInstance().getPlayerStats(player.getUniqueId()).playerClass;
        // Allow flight (and hence double jump) only if the player is an ARCHER
        if (playerClass == PlayerClass.ARCHER) {
            player.setAllowFlight(true);
        } else {
            player.setAllowFlight(false);
        }
    }

    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        // Check using StatsManager; only ARCHER can double jump
        if (StatsManager.getInstance().getPlayerStats(player.getUniqueId()).playerClass != PlayerClass.ARCHER) {
            return;
        }
        // Skip double jump logic if the player is in Creative mode
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        // Proceed only if the player isn't already flying
        if (!player.isFlying()) {
            // Cancel the default flight toggle and disable flight until landing
            event.setCancelled(true);
            player.setAllowFlight(false);

            // Apply upward velocity for the double jump
            player.setVelocity(player.getLocation().getDirection().setY(jumpVelocity));

            // Spawn cloud particles at the player's location
            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 30, 0.5, 0.1, 0.5, 0.1);

            // Play a sound effect
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1.0f, 1.0f);
        }
    }

    @EventHandler
    public void onPlayerLand(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        // When the player is on the ground, re-enable flight if they're an ARCHER
        if (player.isOnGround() && StatsManager.getInstance().getPlayerStats(player.getUniqueId()).playerClass == PlayerClass.ARCHER) {
            player.setAllowFlight(true);
        }
    }
}
