package me.nakilex.npc.plugin.listener;

import me.nakilex.npc.core.event.NpcInteractEvent;
import me.nakilex.npc.core.event.NpcLeftClickEvent;
import me.nakilex.npc.core.model.Npc;
import me.nakilex.npc.plugin.service.NpcService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.Optional;

public class NpcInteractionListener implements Listener {
    private final NpcService service;

    public NpcInteractionListener(NpcService service) {
        this.service = service;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        Optional<Npc> npc = service.findByEntityUuid(entity.getUniqueId());
        if (npc.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        Bukkit.getPluginManager().callEvent(new NpcInteractEvent(npc.get(), event.getPlayer()));
        service.getTraitRegistry().list().forEach(trait -> trait.onInteract(npc.get(), event.getPlayer()));
    }

    @EventHandler
    public void onLeftClick(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        Optional<Npc> npc = service.findByEntityUuid(event.getEntity().getUniqueId());
        if (npc.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        Bukkit.getPluginManager().callEvent(new NpcLeftClickEvent(npc.get(), player));
    }
}
