package me.nakilex.levelplugin.guild.siege;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class GuildSiegeListener implements Listener {
    private final GuildSiegeManager manager;

    public GuildSiegeListener(GuildSiegeManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        manager.leave(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        manager.refreshTownVisibility(event.getPlayer());
    }
}
