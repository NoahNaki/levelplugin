package me.nakilex.levelplugin.calendar;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFormEvent;

/**
 * Prevents natural snowfall from placing snow layers on the ground.
 */
public class WeatherBlockListener implements Listener {

    @EventHandler
    public void onBlockForm(BlockFormEvent event) {
        if (event.getNewState().getType() == Material.SNOW) {
            event.setCancelled(true);
        }
    }
}
