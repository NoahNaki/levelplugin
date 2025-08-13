package me.nakilex.levelplugin.guild.siege;

import me.nakilex.levelplugin.Main;
import org.bukkit.Bukkit;
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
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () ->
                manager.refreshTownVisibility(event.getPlayer()), 40L);
    }
}
