package me.nakilex.levelplugin.listeners;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;

/**
 * If you only want debug or other logic here, keep it.
 * But do not apply or remove stats for the weapon.
 */
public class WeaponHeldListener implements Listener {

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        // Just debug if you want
        Bukkit.getLogger().info("[WeaponHeldListener] " + event.getPlayer().getName()
            + " switching oldSlot=" + event.getPreviousSlot()
            + " -> newSlot=" + event.getNewSlot());
    }
}
