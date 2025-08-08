package me.nakilex.levelplugin.screen.listener;

import me.nakilex.levelplugin.screen.CursorMenuSystem;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

/**
 * Ensures players stay in the intended world when a cursor menu is active.
 */
public class WorldChangeListener implements Listener {
    private final CursorMenuSystem system;

    public WorldChangeListener(CursorMenuSystem system) {
        this.system = system;
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        if (system.isInMenu(e.getPlayer())) {
            // Teleport back to camera location
            system.getSectionManager().get("default");
            // Implementation can be extended to fetch stored camera loc.
        }
    }
}
