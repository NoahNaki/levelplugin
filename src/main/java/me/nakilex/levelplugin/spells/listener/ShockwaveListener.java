package me.nakilex.levelplugin.spells.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;

/**
 * Prevents temporary Shockwave falling blocks from placing into the world.
 */
public class ShockwaveListener implements Listener {
    @EventHandler
    public void onBlockPlace(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof FallingBlock fb)) return;
        if (!fb.hasMetadata("Shockwave")) return;
        event.setCancelled(true);
        // Let the block fall slightly through the ground before removing
        fb.setVelocity(fb.getVelocity().setY(-0.4));
        Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugin("LevelPlugin"),
                fb::remove,
                60L
        );
    }
}
