package me.nakilex.levelplugin.luxbridge.listener;

import me.nakilex.levelplugin.luxbridge.LuxBridgeManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class LuxBridgePlayerListener implements Listener {
    private final LuxBridgeManager manager;

    public LuxBridgePlayerListener(LuxBridgeManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!manager.hasSession(player)) return;
        event.setCancelled(true);
        if (player.isSneaking()) {
            manager.acceptAnswer(player);
        } else {
            manager.skipOrNext(player);
        }
    }

    @EventHandler
    public void onHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (!manager.hasSession(player)) return;
        event.setCancelled(true);
        manager.selectNext(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        manager.stop(event.getPlayer());
    }
}
