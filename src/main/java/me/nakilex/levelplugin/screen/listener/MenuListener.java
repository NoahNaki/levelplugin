package me.nakilex.levelplugin.screen.listener;

import me.nakilex.levelplugin.screen.CursorMenuSystem;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles basic lifecycle events for the cursor menu. When a player joins the
 * server a default menu can be opened, while leaving cleans up any spawned
 * entities.
 */
public class MenuListener implements Listener {
    private final CursorMenuSystem system;

    public MenuListener(CursorMenuSystem system) {
        this.system = system;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        // Example auto menu. Real usage should check configuration.
        // system.setupCursor(e.getPlayer(), "welcome");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        system.stopCursor(e.getPlayer());
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) {
        if (system.isInMenu(e.getPlayer())) {
            e.setCancelled(true);
        }
    }
}
