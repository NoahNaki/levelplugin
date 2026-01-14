package me.nakilex.levelplugin.npc.listeners;

import me.nakilex.levelplugin.npc.system.NPC;
import me.nakilex.levelplugin.npc.system.NpcApi;
import me.nakilex.levelplugin.npc.system.NpcSkinService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class NpcViewerListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        applySkins(event.getPlayer());
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        applySkins(event.getPlayer());
    }

    private void applySkins(Player player) {
        for (NPC npc : NpcApi.getRegistry()) {
            if (!npc.isSpawned()
                    || npc.getType() != org.bukkit.entity.EntityType.PLAYER
                    || npc.getEntity() == null
                    || npc.getEntity().getWorld() == null) {
                continue;
            }
            if (!npc.getEntity().getWorld().equals(player.getWorld())) {
                continue;
            }
            NpcSkinService.applySkinToViewer(player, npc, 20L);
        }
    }
}
