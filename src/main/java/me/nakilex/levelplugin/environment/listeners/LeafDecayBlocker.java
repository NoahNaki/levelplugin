package me.nakilex.levelplugin.environment.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.LeavesDecayEvent;

/**
 * Prevents leaf decay so changes to the random tick speed gamerule do not
 * accelerate or otherwise affect any leaf blocks.
 */
public class LeafDecayBlocker implements Listener {

    @EventHandler
    public void onLeafDecay(LeavesDecayEvent event) {
        event.setCancelled(true);
    }
}
