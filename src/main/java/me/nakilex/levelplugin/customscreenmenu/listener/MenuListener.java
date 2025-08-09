package me.nakilex.levelplugin.customscreenmenu.listener;

import me.nakilex.levelplugin.customscreenmenu.CustomScreenMenuPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class MenuListener implements Listener {
    private final CustomScreenMenuPlugin plugin;

    public MenuListener(CustomScreenMenuPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.getMenuManager().handleQuit(e.getPlayer());
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        plugin.getMenuManager().handleQuit(e.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        plugin.getMenuManager().handleQuit(e.getEntity());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        plugin.getMenuManager().handleQuit(e.getPlayer());
    }
}
