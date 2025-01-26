package me.nakilex.levelplugin.utils;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.entity.Player;

public class HungerDisabler implements Listener {

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        // Check if the entity is a player
        if (event.getEntity() instanceof Player) {
            // Cancel the event to prevent hunger depletion
            event.setCancelled(true);

            // Set the player's food level to max (20)
            ((Player) event.getEntity()).setFoodLevel(20);

            // Optionally, ensure the player's saturation is full
            ((Player) event.getEntity()).setSaturation(20.0f);
        }
    }
}
