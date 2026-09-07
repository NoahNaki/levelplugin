package me.nakilex.levelplugin.world;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public final class WorldRulesListener implements Listener {
    private final WorldManager worldManager;

    public WorldRulesListener(WorldManager worldManager) {
        this.worldManager = worldManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        worldManager.applyPlayerRules(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        worldManager.applyPlayerRules(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        worldManager.applyPlayerRules(event.getPlayer());
    }
}
