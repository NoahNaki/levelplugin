package me.nakilex.levelplugin.player.utils;

import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.Entity;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPickupArrowEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class ArrowUtils implements Listener {

    private final Plugin plugin;

    public ArrowUtils(Plugin plugin) {
        this.plugin = plugin;
    }

    // Event listener that prevents players from picking up arrows
    @EventHandler
    public void onArrowPickup(PlayerPickupArrowEvent event) {
        Player player = event.getPlayer();
        event.setCancelled(true);  // Prevent the player from picking up arrows
    }

    // Start the task to clean up arrows that are on the ground periodically
    public void startArrowCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Loop through all worlds and collect arrows
                for (World world : plugin.getServer().getWorlds()) {
                    for (Entity entity : world.getEntities()) {
                        // Check if the entity is an arrow
                        if (entity instanceof Arrow) {
                            Arrow arrow = (Arrow) entity;

                            // If the arrow is on the ground or stuck in a block, remove it
                            if (arrow.isOnGround() || arrow.isInBlock()) {
                                arrow.remove();  // Remove arrow from the world
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 55L);  // Run every 55 ticks (2.75 seconds)
    }

}
