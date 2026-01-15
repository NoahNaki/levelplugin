package me.nakilex.levelplugin.guild.siege;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.npc.system.NpcTagUtil;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class GuildSiegeListener implements Listener {
    private final GuildSiegeManager manager;

    public GuildSiegeListener(GuildSiegeManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (NpcTagUtil.isNpc(event.getPlayer())) {
            return;
        }
        manager.leave(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (NpcTagUtil.isNpc(event.getPlayer())) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () ->
                manager.refreshTownVisibility(event.getPlayer()), 40L);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (NpcTagUtil.isNpc(event.getPlayer())) {
            return;
        }
        java.util.UUID id = event.getPlayer().getUniqueId();
        org.bukkit.Location respawn = manager.getRespawnLocation(id);
        if (respawn != null) {
            event.setRespawnLocation(respawn);
        }
    }
}
