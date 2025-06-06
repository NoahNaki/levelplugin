package me.nakilex.levelplugin.utils;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;

public class HungerDisabler implements Listener {

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            // Prevent vanilla hunger mechanics from changing the bar
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onHealthRegain(EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof Player && event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED) {
            // Cancel natural health regen from being fully fed
            event.setCancelled(true);
        }
    }
}
