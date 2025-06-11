package me.nakilex.levelplugin.spells.listener;

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
        fb.remove();
    }
}
