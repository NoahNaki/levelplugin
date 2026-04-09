package me.nakilex.levelplugin.server;

import me.nakilex.levelplugin.Main;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class LevelPluginCommandGuard implements Listener {

    public LevelPluginCommandGuard(Main plugin, ServerSelectionManager serverSelectionManager) {
        // Command guard restrictions were intentionally removed so commands are
        // no longer blocked outside alpha/build worlds.
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        // No-op: all commands are now allowed in all worlds.
    }
}
