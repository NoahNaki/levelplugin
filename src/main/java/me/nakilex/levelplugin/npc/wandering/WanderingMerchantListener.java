package me.nakilex.levelplugin.npc.wandering;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/** Handles interactions with the wandering merchant NPC. */
public class WanderingMerchantListener implements Listener {
    private final WanderingMerchantManager manager;

    public WanderingMerchantListener(WanderingMerchantManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onRightClick(PlayerInteractEntityEvent e) {
        if (manager.isActive() && e.getRightClicked().getUniqueId().equals(manager.getMerchant().getUniqueId())) {
            e.setCancelled(true);
            manager.openShop(e.getPlayer());
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!manager.isActive()) return;
        if (!e.getEntity().getUniqueId().equals(manager.getMerchant().getUniqueId())) return;
        manager.closeShop();
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        if (manager.isActive() && e.getEntity().getUniqueId().equals(manager.getMerchant().getUniqueId())) {
            for (WanderingMerchantOffer of : manager.getGui().getOffers()) {
                if (of.getStock() > 0) {
                    e.getDrops().add(of.getItem());
                }
            }
            manager.despawn();
        }
    }
}
