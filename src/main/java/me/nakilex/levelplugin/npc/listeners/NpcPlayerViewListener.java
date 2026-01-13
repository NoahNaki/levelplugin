package me.nakilex.levelplugin.npc.listeners;

import me.nakilex.levelplugin.npc.system.NpcApi;
import me.nakilex.levelplugin.npc.system.NpcPlayerRenderer;
import me.nakilex.levelplugin.npc.system.NPC;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class NpcPlayerViewListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        for (NPC npc : NpcApi.getRegistry()) {
            if (npc.getType() == EntityType.PLAYER && npc.isSpawned()) {
                NpcPlayerRenderer.spawnFor(event.getPlayer(), npc);
            }
        }
    }
}
