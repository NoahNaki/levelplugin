package me.nakilex.levelplugin.utils;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.entity.Player;

public class FallDamageDisabler implements Listener {

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        // Check if the damage cause is fall damage
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            // Check if the entity is a player
            if (event.getEntity() instanceof Player) {
                // Cancel the damage event
                event.setCancelled(true);
            }
        }
    }
}
