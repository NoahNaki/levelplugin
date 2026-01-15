package me.nakilex.levelplugin.npc.listeners;

import me.nakilex.levelplugin.npc.system.NPC;
import me.nakilex.levelplugin.npc.system.NpcApi;
import me.nakilex.levelplugin.npc.system.NpcSkinService;
import me.nakilex.levelplugin.npc.system.NpcTagUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class NpcViewerListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (NpcTagUtil.isNpc(event.getPlayer())) {
            return;
        }
        applySkins(event.getPlayer());
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        if (NpcTagUtil.isNpc(event.getPlayer())) {
            return;
        }
        applySkins(event.getPlayer());
    }

    private void applySkins(Player player) {
        for (NPC npc : NpcApi.getRegistry()) {
            if (!npc.isSpawned()
                    || npc.getType() != org.bukkit.entity.EntityType.PLAYER
                    || npc.getPacketPlayer() == null
                    || npc.getPacketPlayer().getLocation().getWorld() == null) {
                continue;
            }
            if (!npc.getPacketPlayer().getLocation().getWorld().equals(player.getWorld())) {
                continue;
            }
            NpcSkinService.applySkinToViewer(player, npc, 20L);
        }
    }
}
