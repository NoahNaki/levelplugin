package me.nakilex.levelplugin.utils;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Prevents farmland from being trampled by players walking or jumping on it.
 */
public class CropTrampleListener implements Listener {

    @EventHandler
    public void onCropTrample(PlayerInteractEvent event) {
        if (event.getAction() != Action.PHYSICAL) return;
        if (event.getClickedBlock() == null) return;
        if (event.getClickedBlock().getType() == Material.FARMLAND) {
            event.setCancelled(true);
        }
    }
}
